package search.common.entity.bizesinterface;

/**
 * Created by soledede.weng on 2016/8/15.
 */
public class BaseStock {
    private String comSim;
    private String company;
    private String comEn;
    private String comCode;

    public BaseStock() {
    }

    public BaseStock(String comSim, String company, String comEn, String comCode) {
        this.comSim = comSim;
        this.company = company;
        this.comEn = comEn;
        this.comCode = comCode;
    }

    public String getComSim() {
        return comSim;
    }

    public void setComSim(String comSim) {
        this.comSim = comSim;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getComEn() {
        return comEn;
    }

    public void setComEn(String comEn) {
        this.comEn = comEn;
    }

    public String getComCode() {
        return comCode;
    }

    public void setComCode(String comCode) {
        this.comCode = comCode;
    }
}
