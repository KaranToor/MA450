package picture;

import java.math.BigDecimal;

/**
 * Created by Karan on 3/4/17.
 * Represents a picture object with user id, location, payment type,
 * date, price, category, and photo id
 */

public class PictureObject {
    /**
     * Holds the ID number of the current user.
     */
    private int mUserId;

    /**
     * The location field for this PictureObject.
     */
    private String mLocation;

    /**
     * The payment type field for this PictureObject.
     */
    private String mPaymentType;

    /**
     * The date field for this PictureObject.
     */
    private String mDate;

    /**
     * The price field for this PictureObject.
     */
    private BigDecimal mPrice;

    /**
     * The category field for this PictureObject.
     */
    private String mCategory;

    /**
     * The photoID field for this PictureObject.
     */
    private String mPhotoId;

    /**
     * Instantiates the variables of Picture Object
     *
     * @param theUserId      the user's id
     * @param thePhotoId     the id associated with the photo
     * @param theLocation    the location of purchase
     * @param thePrice       the total price of purchases on any one receipt
     * @param thePaymentType the payment type used for purchases
     * @param theDate        the date of purchases
     * @param theCategory    the category of these purchases
     */
    public PictureObject(int theUserId, String thePhotoId, String theLocation,
                         BigDecimal thePrice, String thePaymentType, String theDate,
                         String theCategory) {
        this.mUserId = theUserId;
        this.mLocation = theLocation;
        this.mPaymentType = thePaymentType;
        this.mDate = theDate;
        this.mPrice = thePrice;
        this.mCategory = theCategory;
        this.mPhotoId = thePhotoId;
    }

    /**
     * Gets the user id
     *
     * @return mUserId to be retrieved
     */
    public int getmUserId() {
        return mUserId;
    }

    /**
     * sets the user id
     *
     * @param mUserId to be set
     */
    public void setmUserId(int mUserId) {
        this.mUserId = mUserId;
    }

    /**
     * Gets the location of purchase from receipt
     *
     * @return mLocation to be retrieved
     */
    public String getmLocation() {
        return mLocation;
    }

    /**
     * Sets my location
     *
     * @param mLocation to be set
     */
    public void setmLocation(String mLocation) {
        this.mLocation = mLocation;
    }

    /**
     * Gets the payment type
     *
     * @return mPaymentType to be retrieved
     */
    public String getmPaymentType() {
        return mPaymentType;
    }

    /**
     * Sets the payment type
     *
     * @param mPaymentType to be set
     */
    public void setmPaymentType(String mPaymentType) {
        this.mPaymentType = mPaymentType;
    }

    /**
     * Gets the date from purchase on receipt
     *
     * @return mDate to be retrieved
     */
    public String getmDate() {
        return mDate;
    }

    /**
     * Sets the date
     *
     * @param mDate to be set
     */
    public void setmDate(String mDate) {
        this.mDate = mDate;
    }

    /**
     * Gets the cost of purchase
     *
     * @return mPrice to be retrieved
     */
    public BigDecimal getmPrice() {
        return mPrice;
    }

    /**
     * Sets the cost of the purchase
     *
     * @param mPrice to be set
     */
    public void setmPrice(BigDecimal mPrice) {
        this.mPrice = mPrice;
    }

    /**
     * Gets my category
     *
     * @return mCategory to be retrieved
     */
    public String getmCategory() {
        return mCategory;
    }

    /**
     * Sets the category
     *
     * @param mCategory to be set
     */
    public void setmCategory(String mCategory) {
        this.mCategory = mCategory;
    }

    /**
     * Gets the photo Id
     *
     * @return mPhotoId to be retrieved
     */
    public String getmPhotoId() {
        return mPhotoId;
    }

    /**
     * Sets my photo id
     *
     * @param mPhotoId to be set
     */
    public void setmPhotoId(String mPhotoId) {
        this.mPhotoId = mPhotoId;
    }

    /**
     * @return Returns a String representation of PictureObject
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(mPhotoId);
        return sb.toString();
    }
}
