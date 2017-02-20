# Copyright 2016 Google Inc. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""SSH client utilities for key-generation, dispatching the ssh commands etc."""
import errno
import getpass
import logging
import os
import re
import subprocess
import enum

from googlecloudsdk.calliope import exceptions
from googlecloudsdk.command_lib.util import gaia
from googlecloudsdk.command_lib.util import time_util
from googlecloudsdk.core import config
from googlecloudsdk.core import exceptions as core_exceptions
from googlecloudsdk.core import log
from googlecloudsdk.core import properties
from googlecloudsdk.core.console import console_io
from googlecloudsdk.core.util import files
from googlecloudsdk.core.util import platforms


# `ssh` exits with this exit code in the event of an SSH error (as opposed to a
# successful `ssh` execution where the *command* errored).
_SSH_ERROR_EXIT_CODE = 255

# Normally, all SSH output is simply returned to the user (or sent to
# /dev/null if user output is disabled). For testing, this value can be
# overridden with a file path.
SSH_OUTPUT_FILE = None

PER_USER_SSH_CONFIG_FILE = os.path.join('~', '.ssh', 'config')

# The default timeout for waiting for a host to become reachable.
# Useful for giving some time after VM booting, key propagation etc.
_DEFAULT_TIMEOUT = 60


class MissingCommandError(core_exceptions.Error):
  """Indicates that an external executable couldn't be found."""


class CommandError(core_exceptions.Error):
  """Raise for a failure when invoking ssh, scp, or similar."""

  def __init__(self, cmd, message=None, return_code=None):
    if not (message or return_code):
      raise ValueError('One of message or return_code is required.')

    self.cmd = cmd

    message_text = '[{0}]'.format(message) if message else None
    return_code_text = ('return code [{0}]'.format(return_code)
                        if return_code else None)
    why_failed = ' and '.join(filter(None, [message_text, return_code_text]))

    super(CommandError, self).__init__(
        '[{0}] exited with {1}.'.format(self.cmd, why_failed),
        exit_code=return_code)


class Environment(object):
  """Environment maps SSH commands to executable location on file system.

    Recommended usage:

    env = Environment.Current()
    env.RequireSSH()
    cmd = [env.ssh, 'user@host']

  An attribute which is None indicates that the executable couldn't be found.

  Attributes:
    ssh: Location of ssh command.
    ssh_term: Location of ssh terminal command, for interactive sessions.
    scp: Location of scp command.
    keygen: Location of the keygen command.
  """

  _NIX_COMMANDS = {
      'ssh': 'ssh',
      'ssh_term': 'ssh',  # For interactive mode
      'scp': 'scp',
      'keygen': 'ssh-keygen',
  }

  _WINDOWS_COMMANDS = {
      'ssh': 'plink',
      'ssh_term': 'putty',
      'scp': 'pscp',
      'keygen': 'winkeygen',
  }

  def __init__(self, ssh=None, ssh_term=None, scp=None, keygen=None):
    """Create a new environment by supplying executable paths.

    None supplied as an argument indicates that the executable is not available
    in the current environment.

    Args:
      ssh: Location of ssh command.
      ssh_term: Location of ssh terminal command, for interactive sessions.
      scp: Location of scp command.
      keygen: Location of the keygen command.
    """
    self.ssh = ssh
    self.ssh_term = ssh_term
    self.scp = scp
    self.keygen = keygen

  def SupportsSSH(self):
    """Whether all SSH commands are supported.

    Returns:
      True if and only if all commands are supported, else False.
    """
    return all((self.ssh, self.ssh_term, self.scp, self.keygen))

  def RequireSSH(self):
    """Simply raises an error if any SSH command is not supported.

    Raises:
      MissingCommandError: One or more of the commands were not found.
    """
    if not self.SupportsSSH():
      raise MissingCommandError('Your platform does not support SSH.')

  @classmethod
  def Current(cls):
    """Retrieve the current environment.

    Returns:
      Environment, the active and current environment on this machine.
    """
    if platforms.OperatingSystem.IsWindows():
      commands = Environment._WINDOWS_COMMANDS
      path = _SdkHelperBin()
    else:
      commands = Environment._NIX_COMMANDS
      path = None
    env = Environment()
    for key, cmd in commands.iteritems():
      setattr(env, key, files.FindExecutableOnPath(cmd, path=path))
    return env


def _IsValidSshUsername(user):
  # All characters must be ASCII, and no spaces are allowed
  # This may grant false positives, but will prevent backwards-incompatible
  # behavior.
  return all(ord(c) < 128 and c != ' ' for c in user)


class KeyFileStatus(enum.Enum):
  PRESENT = 'OK'
  ABSENT = 'NOT FOUND'
  BROKEN = 'BROKEN'


class _KeyFileKind(enum.Enum):
  """List of supported (by gcloud) key file kinds."""
  PRIVATE = 'private'
  PUBLIC = 'public'
  PPK = 'PuTTY PPK'


class Keys(object):
  """Manages private and public SSH key files.

  This class manages the SSH public and private key files, and verifies
  correctness of them. A Keys object is instantiated with a path to a
  private key file. The public key locations are inferred by the private
  key file by simply appending a different file ending (`.pub` and `.ppk`).

  If the keys are broken or do not yet exist, the EnsureKeysExist method
  can be utilized to shell out to the system SSH keygen and write new key
  files.

  By default, there is an SSH key for the gcloud installation,
  `DEFAULT_KEY_FILE` which should likely be used. Note that SSH keys are
  generated and managed on a per-installation basis. Strictly speaking,
  there is no 1:1 relationship between installation and user account.

  Verifies correctness of key files:
   - Populates list of SSH key files (key pair, ppk key on Windows).
   - Checks if files are present and (to basic extent) correct.
   - Can remove broken key (if permitted by user).
   - Provides status information.
  """

  DEFAULT_KEY_FILE = os.path.join('~', '.ssh', 'google_compute_engine')

  class KeyFileData(object):

    def __init__(self, filename):
      # We keep filename as file handle. Filesystem race is impossible to avoid
      # in this design as we spawn a subprocess and pass in filename.
      # TODO(b/33288605) fix it.
      self.filename = filename
      self.status = None

  def __init__(self, key_file):
    """Create a Keys object which manages the given files.

    Args:
      key_file: str, The file path to the private SSH key file (other files are
          derived from this name). Automatically handles symlinks and user
          expansion.
    """
    private_key_file = os.path.realpath(os.path.expanduser(key_file))
    self.dir = os.path.dirname(private_key_file)
    self.keys = {
        _KeyFileKind.PRIVATE: self.KeyFileData(private_key_file),
        _KeyFileKind.PUBLIC: self.KeyFileData(private_key_file + '.pub')
    }
    if platforms.OperatingSystem.IsWindows():
      self.keys[_KeyFileKind.PPK] = self.KeyFileData(private_key_file + '.ppk')

  @classmethod
  def FromFilename(cls, filename=None):
    """Create Keys object given a file name.

    Args:
      filename: str or None, the name to the file or DEFAULT_KEY_FILE if None

    Returns:
      Keys, an instance which manages the keys with the given name.
    """
    return cls(filename or Keys.DEFAULT_KEY_FILE)

  @property
  def key_file(self):
    """Filename of the private key file."""
    return self.keys[_KeyFileKind.PRIVATE].filename

  def _StatusMessage(self):
    """Prepares human readable SSH key status information."""
    messages = []
    key_padding = 0
    status_padding = 0
    for kind in self.keys:
      data = self.keys[kind]
      key_padding = max(key_padding, len(kind.value))
      status_padding = max(status_padding, len(data.status.value))
    for kind in self.keys:
      data = self.keys[kind]
      messages.append('{} {} [{}]\n'.format(
          (kind.value + ' key').ljust(key_padding + 4),
          ('(' + data.status.value + ')') .ljust(status_padding + 2),
          data.filename))
    messages.sort()
    return ''.join(messages)

  def Validate(self):
    """Performs minimum key files validation.

    Returns:
      KeyFileStatus.PRESENT if key files meet minimum requirements.
      KeyFileStatus.ABSENT if neither private nor public keys exist.
      KeyFileStatus.BROKEN if there is some key, but it is broken or incomplete.
    """
    def ValidateFile(kind):
      status_or_line = self._WarnOrReadFirstKeyLine(self.keys[kind].filename,
                                                    kind.value)
      if isinstance(status_or_line, KeyFileStatus):
        return status_or_line
      else:  # returned line - present
        self.keys[kind].first_line = status_or_line
        return KeyFileStatus.PRESENT

    for file_kind in self.keys:
      self.keys[file_kind].status = ValidateFile(file_kind)

    # The remaining checks are for the public key file.

    # Must have at least 2 space separated fields.
    if self.keys[_KeyFileKind.PUBLIC].status is KeyFileStatus.PRESENT:
      fields = self.keys[_KeyFileKind.PUBLIC].first_line.split(' ')
      if len(fields) < 2:
        log.warn(
            'The public SSH key file for gcloud is corrupt.')
        self.keys[_KeyFileKind.PUBLIC].status = KeyFileStatus.BROKEN

    # Summary
    collected_values = [x.status for x in self.keys.itervalues()]
    if all(x == KeyFileStatus.ABSENT for x in collected_values):
      return KeyFileStatus.ABSENT
    elif all(x == KeyFileStatus.PRESENT for x in collected_values):
      return KeyFileStatus.PRESENT
    else:
      return KeyFileStatus.BROKEN

  def RemoveKeyFilesIfPermittedOrFail(self, force_key_file_overwrite=None):
    """Removes all SSH key files if user permitted this behavior.

    Precondition: The SSH key files are currently in a broken state.

    Depending on `force_key_file_overwrite`, delete all SSH key files:

    - If True, delete key files.
    - If False, cancel immediately.
    - If None and
      - interactive, prompt the user.
      - non-interactive, cancel.

    Args:
      force_key_file_overwrite: bool or None, overwrite broken key files.

    Raises:
      console_io.OperationCancelledError: Operation intentionally cancelled.
      OSError: Error deleting the broken file(s).
    """
    message = 'Your SSH key files are broken.\n' + self._StatusMessage()
    if force_key_file_overwrite is False:
      raise console_io.OperationCancelledError(message + 'Operation aborted.')
    message += 'We are going to overwrite all above files.'
    log.warn(message)
    if force_key_file_overwrite is None:
      # - Interactive when pressing 'Y', continue
      # - Interactive when pressing enter or 'N', raise OperationCancelledError
      # - Non-interactive, raise OperationCancelledError
      console_io.PromptContinue(default=False, cancel_on_no=True)

    # Remove existing broken key files.
    for key_file in self.keys.viewvalues():
      try:
        os.remove(key_file.filename)
      except OSError as e:
        if e.errno == errno.EISDIR:
          # key_file.filename points to a directory
          raise

  def _WarnOrReadFirstKeyLine(self, path, kind):
    """Returns the first line from the key file path.

    A None return indicates an error and is always accompanied by a log.warn
    message.

    Args:
      path: The path of the file to read from.
      kind: The kind of key file, 'private' or 'public'.

    Returns:
      None (and prints a log.warn message) if the file does not exist, is not
      readable, or is empty. Otherwise returns the first line utf8 decoded.
    """
    try:
      with open(path) as f:
        # Decode to utf8 to handle any unicode characters. Key data is base64
        # encoded so it cannot contain any unicode. Comments may contain
        # unicode, but they are ignored in the key file analysis here, so
        # replacing invalid chars with ? is OK.
        line = f.readline().strip().decode('utf8', 'replace')
        if line:
          return line
        msg = 'is empty'
        status = KeyFileStatus.BROKEN
    except IOError as e:
      if e.errno == errno.ENOENT:
        msg = 'does not exist'
        status = KeyFileStatus.ABSENT
      else:
        msg = 'is not readable'
        status = KeyFileStatus.BROKEN
    log.warn('The %s SSH key file for gcloud %s.', kind, msg)
    return status

  def GetPublicKey(self):
    """Returns the public key verbatim from file as a string.

    Precondition: The public key must exist. Run Keys.EnsureKeysExist() prior.

    Returns:
      str, The public key.
    """
    # TODO(b/33467618): There is a file-format specification for the key file.
    # It looks roughly like this: `TYPE KEY [COMMENT]`. Make sure to use that
    # instead.
    filepath = self.keys[_KeyFileKind.PUBLIC].filename
    with open(filepath) as f:
      # We get back a unicode list of keys for the remaining metadata, so
      # convert to unicode. Assume UTF 8, but if we miss a character we can just
      # replace it with a '?'. The only source of issues would be the hostnames,
      # which are relatively inconsequential.
      return f.readline().strip().decode('utf8', 'replace')

  def EnsureKeysExist(self, keygen, overwrite):
    """Generate ssh key files if they do not yet exist.

    Precondition: Environment.SupportsSSH()

    Args:
      keygen: str, path to ssh-keygen, see Environment.keygen
      overwrite: bool or None, overwrite key files if they are broken.

    Raises:
      console_io.OperationCancelledError: if interrupted by user
      CommandError: if the ssh-keygen command failed.
    """
    key_files_validity = self.Validate()

    if key_files_validity is KeyFileStatus.BROKEN:
      self.RemoveKeyFilesIfPermittedOrFail(overwrite)
      # Fallthrough
    if key_files_validity is not KeyFileStatus.PRESENT:
      if key_files_validity is KeyFileStatus.ABSENT:
        # If key is broken, message is already displayed
        log.warn('You do not have an SSH key for gcloud.')
        log.warn('[%s] will be executed to generate a key.', keygen)

      if not os.path.exists(self.dir):
        msg = ('This tool needs to create the directory [{0}] before being '
               'able to generate SSH keys.'.format(self.dir))
        console_io.PromptContinue(
            message=msg, cancel_on_no=True,
            cancel_string='SSH key generation aborted by user.')
        files.MakeDir(self.dir, 0700)

      keygen_args = [keygen]
      if platforms.OperatingSystem.IsWindows():
        # No passphrase in the current implementation.
        keygen_args.append(self.key_file)
      else:
        if properties.VALUES.core.disable_prompts.GetBool():
          # Specify empty passphrase on command line
          keygen_args.extend(['-P', ''])
        keygen_args.extend([
            '-t', 'rsa',
            '-f', self.key_file,
        ])
      RunExecutable(keygen_args)


class KnownHosts(object):
  """Represents known hosts file, supports read, write and basic key management.

  Currently a very naive, but sufficient, implementation where each entry is
  simply a string, and all entries are list of those strings.
  """

  # TODO(b/33467618): Rename the file itself
  DEFAULT_PATH = os.path.realpath(os.path.expanduser(
      os.path.join('~', '.ssh', 'google_compute_known_hosts')))

  def __init__(self, known_hosts, file_path):
    """Construct a known hosts representation based on a list of key strings.

    Args:
      known_hosts: str, list each corresponding to a line in known_hosts_file.
      file_path: str, path to the known_hosts_file.
    """
    self.known_hosts = known_hosts
    self.file_path = file_path

  @classmethod
  def FromFile(cls, file_path):
    """Create a KnownHosts object given a known_hosts_file.

    Args:
      file_path: str, path to the known_hosts_file.

    Returns:
      KnownHosts object corresponding to the file. If the file could not be
      opened, the KnownHosts object will have no entries.
    """
    try:
      known_hosts = files.GetFileContents(file_path).splitlines()
    except files.Error as e:
      known_hosts = []
      log.debug('SSH Known Hosts File [{0}] could not be opened: {1}'
                .format(file_path, e))
    return KnownHosts(known_hosts, file_path)

  @classmethod
  def FromDefaultFile(cls):
    """Create a KnownHosts object from the default known_hosts_file.

    Returns:
      KnownHosts object corresponding to the default known_hosts_file.
    """
    return KnownHosts.FromFile(KnownHosts.DEFAULT_PATH)

  def ContainsAlias(self, host_key_alias):
    """Check if a host key alias exists in one of the known hosts.

    Args:
      host_key_alias: str, the host key alias

    Returns:
      bool, True if host_key_alias is in the known hosts file. If the known
      hosts file couldn't be opened it will be treated as if empty and False
      returned.
    """
    return any(host_key_alias in line for line in self.known_hosts)

  def Add(self, hostname, host_key, overwrite=False):
    """Add or update the entry for the given hostname.

    If there is no entry for the given hostname, it will be added. If there is
    an entry already and overwrite_keys is False, nothing will be changed. If
    there is an entry and overwrite_keys is True, the key will be updated if it
    has changed.

    Args:
      hostname: str, The hostname for the known_hosts entry.
      host_key: str, The host key for the given hostname.
      overwrite: bool, If true, will overwrite the entry corresponding to
        hostname with the new host_key if it already exists. If false and an
        entry already exists for hostname, will ignore the new host_key value.
    """
    new_key_entry = '{0} {1}'.format(hostname, host_key)
    for i, key in enumerate(self.known_hosts):
      if key.startswith(hostname):
        if overwrite:
          self.known_hosts[i] = new_key_entry
        break
    else:
      self.known_hosts.append(new_key_entry)

  def Write(self):
    """Writes the file to disk."""
    with files.OpenForWritingPrivate(self.file_path) as f:
      f.write('\n'.join(self.known_hosts) + '\n')


def GetDefaultSshUsername(warn_on_account_user=False):
  """Returns the default username for ssh.

  The default username is the local username, unless that username is invalid.
  In that case, the default username is the username portion of the current
  account.

  Emits a warning if it's not using the local account username.

  Args:
    warn_on_account_user: bool, whether to warn if using the current account
      instead of the local username.

  Returns:
    str, the default SSH username.
  """
  user = getpass.getuser()
  if not _IsValidSshUsername(user):
    full_account = properties.VALUES.core.account.Get(required=True)
    account_user = gaia.MapGaiaEmailToDefaultAccountName(full_account)
    if warn_on_account_user:
      log.warn('Invalid characters in local username [{0}]. '
               'Using username corresponding to active account: [{1}]'.format(
                   user, account_user))
    user = account_user
  return user


def UserHost(user, host):
  """Returns a string of the form user@host."""
  if user:
    return user + '@' + host
  else:
    return host


def RunExecutable(cmd_args, strict_error_checking=True,
                  ignore_ssh_errors=False):
  """Run the given command, handling errors appropriately.

  Args:
    cmd_args: list of str, the arguments (including executable path) to run
    strict_error_checking: bool, whether a non-zero, non-255 exit code should be
      considered a failure.
    ignore_ssh_errors: bool, when true ignore all errors, including the 255
      exit code.

  Returns:
    int, the return code of the command

  Raises:
    CommandError: if the command failed (based on the command exit code and
      the strict_error_checking flag)
  """
  outfile = SSH_OUTPUT_FILE or os.devnull
  with open(outfile, 'w') as output_file:
    if log.IsUserOutputEnabled() and not SSH_OUTPUT_FILE:
      stdout, stderr = None, None
    else:
      stdout, stderr = output_file, output_file
    if (platforms.OperatingSystem.IsWindows() and
        not cmd_args[0].endswith('winkeygen.exe')):
      # TODO(user): b/25126583 will drop StrictHostKeyChecking=no and 'y'.
      # PuTTY and friends always prompt on fingerprint mismatch. A 'y' response
      # adds/updates the fingerprint registry entry and proceeds. The prompt
      # will appear once for each new/changed host. Redirecting stdin is not a
      # problem. Even interactive ssh is not a problem because a separate PuTTY
      # term is used and it ignores the calling process stdin.
      stdin = subprocess.PIPE
    else:
      stdin = None
    try:
      proc = subprocess.Popen(
          cmd_args, stdin=stdin, stdout=stdout, stderr=stderr)
      if stdin == subprocess.PIPE:
        # Max one prompt per host and there can't be more hosts than args.
        proc.communicate('y\n' * len(cmd_args))
      returncode = proc.wait()
    except OSError as e:
      raise CommandError(cmd_args[0], message=e.strerror)
    if not ignore_ssh_errors:
      if ((returncode and strict_error_checking) or
          returncode == _SSH_ERROR_EXIT_CODE):
        raise CommandError(cmd_args[0], return_code=returncode)
    return returncode


def _SdkHelperBin():
  """Returns the SDK helper executable bin directory."""
  # TODO(b/33467618): Remove this method?
  return os.path.join(config.Paths().sdk_root, 'bin', 'sdk')


def GetDefaultFlags(key_file=None):
  """Returns a list of default commandline flags."""
  if not key_file:
    key_file = Keys.DEFAULT_KEY_FILE
  return [
      '-i', key_file,
      '-o', 'UserKnownHostsFile={0}'.format(KnownHosts.DEFAULT_PATH),
      '-o', 'IdentitiesOnly=yes',  # ensure our SSH key trumps any ssh_agent
      '-o', 'CheckHostIP=no'
  ]


def _LocalizeWindowsCommand(cmd_args, env):
  """Translate cmd_args[1:] from ssh form to plink/putty form.

   The translations are:

      ssh form                      plink/putty form
      ========                      ================
      -i PRIVATE_KEY_FILE           -i PRIVATE_KEY_FILE.ppk
      -o ANYTHING                   <ignore>
      -p PORT                       -P PORT
      [USER]@HOST                   [USER]@HOST
      -BOOLEAN_FLAG                 -BOOLEAN_FLAG
      -FLAG WITH_VALUE              -FLAG WITH_VALUE
      POSITIONAL                    POSITIONAL

  Args:
    cmd_args: [str], The command line that will be executed.
    env: Environment, the environment we're running in.

  Returns:
    Returns translated_cmd_args, the localized command line.
  """
  positionals = 0
  cmd_args = list(cmd_args)  # Get a mutable copy.
  translated_args = [cmd_args.pop(0)]
  while cmd_args:  # Each iteration processes 1 or 2 args.
    arg = cmd_args.pop(0)
    if arg == '-i' and cmd_args:
      # -i private_key_file -- use private_key_file.ppk -- if it doesn't exist
      # then winkeygen will be called to generate it before attempting to
      # connect.
      translated_args.append(arg)
      translated_args.append(cmd_args.pop(0) + '.ppk')
    elif arg == '-o' and cmd_args:
      # Ignore `-o anything'.
      cmd_args.pop(0)
    elif arg == '-p' and cmd_args:
      # -p PORT => -P PORT
      translated_args.append('-P')
      translated_args.append(cmd_args.pop(0))
    elif arg in ['-2', '-a', '-C', '-l', '-load', '-m', '-pw', '-R', '-T',
                 '-v', '-x'] and cmd_args:
      # Pass through putty/plink flag with value.
      translated_args.append(arg)
      translated_args.append(cmd_args.pop(0))
    elif arg.startswith('-'):
      # Pass through putty/plink Boolean flags
      translated_args.append(arg)
    else:
      positionals += 1
      translated_args.append(arg)

  # If there is only 1 positional then it must be [USER@]HOST and we should
  # use env.ssh_term_executable to open an xterm window.
  # TODO(b/33467618): Logically, this is not related to Windows, but to the
  # intent of the SSH command. Remember in next round of refactoring.
  if positionals == 1 and translated_args[0] == env.ssh:
    translated_args[0] = env.ssh_term

  return translated_args


def LocalizeCommand(cmd_args, env):
  """Translates an ssh/scp command line to match the local implementation.

  Args:
    cmd_args: [str], The command line that will be executed.
    env: Environment, the environment we're running in.

  Returns:
    Returns translated_cmd_args, the localized command line.
  """
  if platforms.OperatingSystem.IsWindows():
    return _LocalizeWindowsCommand(cmd_args, env)
  return cmd_args


def GetHostKeyArgs(host_key_alias=None, plain=False,
                   strict_host_key_checking=None):
  """Returns default values for HostKeyAlias and StrictHostKeyChecking.

  Args:
    host_key_alias: Alias of the host key in the known_hosts file.
    plain: bool, if running in plain mode.
    strict_host_key_checking: bool, whether to enforce strict host key
        checking. If false, it will be determined by existence of host_key_alias
        in the known hosts file.

  Returns:
    list, list of arguments to add to the ssh command line.
  """
  if plain or platforms.OperatingSystem.IsWindows():
    return []

  known_hosts = KnownHosts.FromDefaultFile()
  if strict_host_key_checking:
    strict_host_key_value = strict_host_key_checking
  elif known_hosts.ContainsAlias(host_key_alias):
    strict_host_key_value = 'yes'
  else:
    strict_host_key_value = 'no'

  cmd_args = ['-o', 'HostKeyAlias={0}'.format(host_key_alias), '-o',
              'StrictHostKeyChecking={0}'.format(strict_host_key_value)]
  return cmd_args


def WaitUntilSSHable(user, host, env, key_file, host_key_alias=None,
                     plain=False, strict_host_key_checking=None,
                     timeout=_DEFAULT_TIMEOUT):
  """Blocks until SSHing to the given host succeeds."""
  ssh_args_for_polling = [env.ssh]
  ssh_args_for_polling.extend(GetDefaultFlags(key_file))
  ssh_args_for_polling.extend(
      GetHostKeyArgs(host_key_alias, plain, strict_host_key_checking))

  ssh_args_for_polling.append(UserHost(user, host))
  ssh_args_for_polling.append('true')
  ssh_args_for_polling = LocalizeCommand(ssh_args_for_polling, env)

  start_sec = time_util.CurrentTimeSec()
  while True:
    logging.debug('polling instance for SSHability')
    retval = subprocess.call(ssh_args_for_polling)
    if retval == 0:
      break
    if time_util.CurrentTimeSec() - start_sec > timeout:
      # TODO(b/33467618): Create another exception
      raise exceptions.ToolException(
          'Could not SSH to the instance.  It is possible that '
          'your SSH key has not propagated to the instance yet. '
          'Try running this command again.  If you still cannot connect, '
          'verify that the firewall and instance are set to accept '
          'ssh traffic.')
    time_util.Sleep(5)


# A remote path has three parts host[@user]:[path], where @user and path are
# optional.
#   A host:
#   - cannot start with '.'
#   - cannot contain ':', '/', '\\', '@'
#   A user:
#   - cannot contain ':'.
#   A path:
#   - can be anything

_SSH_REMOTE_PATH_REGEX = r'[^.:/\\@][^:/\\@]*(@[^:]*)?:'


def IsScpLocalPath(path):
  """Checks if path is an scp local file path.

  Args:
    path: The path name to check.

  Returns:
    True if path is an scp local path, false if it is a remote path.
  """
  # Paths that start with a drive are local. _SSH_REMOTE_PATH_REGEX could match
  # path for some os implementations, so the drive test must be done before the
  # pattern match.
  if os.path.splitdrive(path)[0]:
    return True
  # Paths that match _SSH_REMOTE_PATH_REGEX are not local.
  if re.match(_SSH_REMOTE_PATH_REGEX, path):
    return False
  # Otherwise the path is local.
  return True
