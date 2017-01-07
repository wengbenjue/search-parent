package search.common.entity.biz.report;

/**
 * Created by soledede.weng on 2017-01-07.
 */
public class ResearchReport {
    private String image_dec;
    private String image_n;
    private String image_p;
    private String image_t;
    private String imgpath;
    private String upt;

    public ResearchReport() {
    }

    public ResearchReport(String image_dec, String image_n, String image_p, String image_t, String imgpath, String upt) {
        this.image_dec = image_dec;
        this.image_n = image_n;
        this.image_p = image_p;
        this.image_t = image_t;
        this.imgpath = imgpath;
        this.upt = upt;
    }

    public String getImage_dec() {
        return image_dec;
    }

    public void setImage_dec(String image_dec) {
        this.image_dec = image_dec;
    }

    public String getImage_n() {
        return image_n;
    }

    public void setImage_n(String image_n) {
        this.image_n = image_n;
    }

    public String getImage_p() {
        return image_p;
    }

    public void setImage_p(String image_p) {
        this.image_p = image_p;
    }

    public String getImage_t() {
        return image_t;
    }

    public void setImage_t(String image_t) {
        this.image_t = image_t;
    }

    public String getImgpath() {
        return imgpath;
    }

    public void setImgpath(String imgpath) {
        this.imgpath = imgpath;
    }

    public String getUpt() {
        return upt;
    }

    public void setUpt(String upt) {
        this.upt = upt;
    }
}
