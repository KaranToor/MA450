package tcss450.uw.edu.gvtest;

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
    private int myUserId;

    /**
     * The location field for this PictureObject.
     */
    private String myLocation;

    /**
     * The payment type field for this PictureObject.
     */
    private String myPaymentType;

    /**
     * The date field for this PictureObject.
     */
    private String myDate;

    /**
     * The price field for this PictureObject.
     */
    private BigDecimal myPrice;

    /**
     * The category field for this PictureObject.
     */
    private String myCategory;

    /**
     * The photoID field for this PictureObject.
     */
    private String myPhotoId;

    /**
     * Instantiates the variables of Picture Object
     * @param myUserId the user's id
     * @param myPhotoId the id associated with the photo
     * @param myLocation the location of purchase
     * @param myPrice the total price of purchases on any one receipt
     * @param myPaymentType the payment type used for purchases
     * @param myDate the date of purchases
     * @param myCategory the category of these purchases
     */
    public PictureObject(int myUserId, String myPhotoId, String myLocation,
                         BigDecimal myPrice, String myPaymentType, String myDate,
                         String myCategory) {
        this.myUserId = myUserId;
        this.myLocation = myLocation;
        this.myPaymentType = myPaymentType;
        this.myDate = myDate;
        this.myPrice = myPrice;
        this.myCategory = myCategory;
        this.myPhotoId = myPhotoId;
    }

    /**
     * Gets the user id
     * @return myUserId to be retrieved
     */
    public int getMyUserId() {
        return myUserId;
    }

    /**
     * sets the user id
     * @param myUserId to be set
     */
    public void setMyUserId(int myUserId) {
        this.myUserId = myUserId;
    }

    /**
     * Gets the location of purchase from receipt
     * @return myLocation to be retrieved
     */
    public String getMyLocation() {
        return myLocation;
    }

    /**
     * Sets my location
     * @param myLocation to be set
     */
    public void setMyLocation(String myLocation) {
        this.myLocation = myLocation;
    }

    /**
     * Gets the payment type
     * @return myPaymentType to be retrieved
     */
    public String getMyPaymentType() {
        return myPaymentType;
    }

    /**
     * Sets the payment type
     * @param myPaymentType to be set
     */
    public void setMyPaymentType(String myPaymentType) {
        this.myPaymentType = myPaymentType;
    }

    /**
     * Gets the date from purchase on receipt
     * @return myDate to be retrieved
     */
    public String getMyDate() {
        return myDate;
    }

    /**
     * Sets the date
     * @param myDate to be set
     */
    public void setMyDate(String myDate) {
        this.myDate = myDate;
    }

    /**
     * Gets the cost of purchase
     * @return myPrice to be retrieved
     */
    public BigDecimal getMyPrice() {
        return myPrice;
    }

    /**
     * Sets the cost of the purchase
     * @param myPrice to be set
     */
    public void setMyPrice(BigDecimal myPrice) {
        this.myPrice = myPrice;
    }

    /**
     * Gets my category
     * @return myCategory to be retrieved
     */
    public String getMyCategory() {
        return myCategory;
    }

    /**
     * Sets the category
     * @param myCategory to be set
     */
    public void setMyCategory(String myCategory) {
        this.myCategory = myCategory;
    }

    /**
     * Gets the photo Id
     * @return myPhotoId to be retrieved
     */
    public String getMyPhotoId() {
        return myPhotoId;
    }

    /**
     * Sets my photo id
     * @param myPhotoId to be set
     */
    public void setMyPhotoId(String myPhotoId) {
        this.myPhotoId = myPhotoId;
    }

    /**
     * @return Returns a String representation of PictureObject
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(myPhotoId);
        return sb.toString();
    }
}
