package tcss450.uw.edu.gvtest;

import java.math.BigDecimal;

/**
 * Created by Karan on 3/4/17.
 */

public class PictureObject {
    private int myUserId;
    private String myLocation;
    private String myPaymentType;
    private String myDate;
    private BigDecimal myPrice;
    private String myCategory;
    private String myPhotoId;

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

    public int getMyUserId() {
        return myUserId;
    }

    public void setMyUserId(int myUserId) {
        this.myUserId = myUserId;
    }

    public String getMyLocation() {
        return myLocation;
    }

    public void setMyLocation(String myLocation) {
        this.myLocation = myLocation;
    }

    public String getMyPaymentType() {
        return myPaymentType;
    }

    public void setMyPaymentType(String myPaymentType) {
        this.myPaymentType = myPaymentType;
    }

    public String getMyDate() {
        return myDate;
    }

    public void setMyDate(String myDate) {
        this.myDate = myDate;
    }

    public BigDecimal getMyPrice() {
        return myPrice;
    }

    public void setMyPrice(BigDecimal myPrice) {
        this.myPrice = myPrice;
    }

    public String getMyCategory() {
        return myCategory;
    }

    public void setMyCategory(String myCategory) {
        this.myCategory = myCategory;
    }

    public String getMyPhotoId() {
        return myPhotoId;
    }

    public void setMyPhotoId(String myPhotoId) {
        this.myPhotoId = myPhotoId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(myPhotoId);
        return sb.toString();
    }
}
