package search.common.tool.ictclas;

import org.junit.Test;

/**
 * Created by soledede.weng on 2016/9/2.
 */
public class IctclasTestJava {
    @Test
    public void parse() {
        Segmentation segmentation = new IctclasSegmentation();
        segmentation.addUserWord("工信处 n");
        segmentation.addUserWord("女干事 n");
        System.out.println(segmentation.parse("工信处女干事每月经过下属科室都要亲口交代24口交换机等技术性器件的安装工作", true));
    }
}
