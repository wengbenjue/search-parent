package search.common.entity.searchinterface;

import java.io.Serializable;

/**
 * Created by soledede on 2016/2/20.
 */
public class Brand  implements Serializable{
    private Integer brandId;
    private String brandZhName;
    private String brandEnName;

    public Brand() {
    }

    public Brand(Integer brandId, String brandZhName, String brandEnName) {
        this.brandId = brandId;
        this.brandZhName = brandZhName;
        this.brandEnName = brandEnName;
    }


    public Integer getBrandId() {
        return brandId;
    }

    public void setBrandId(Integer brandId) {
        this.brandId = brandId;
    }

    public String getBrandZhName() {
        return brandZhName;
    }

    public void setBrandZhName(String brandZhName) {
        this.brandZhName = brandZhName;
    }

    public String getBrandEnName() {
        return brandEnName;
    }

    public void setBrandEnName(String brandEnName) {
        this.brandEnName = brandEnName;
    }
}
