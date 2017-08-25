package search.solr.client.producterTest;

import search.solr.client.product.Producter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by soledede on 2016/2/14.
 */
public class ProducterTest {

    public static void main(String[] args) {
       // testAddProduct();
       // testDeleteProduct();
       // testAddCollectionProduct();
       // testDeleteCollectionProduct();
        testCustomMessage();
    }


    public static void testAddProduct() {
       // System.out.println("测试增量更新生产者通过kafka发送通知给消费者单条更新" + Producter.send(System.currentTimeMillis(), 68));
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
       // System.out.println("测试增量更新生产者通过kafka发送通知给消费者，批量更新，传入更新时间段" + Producter.send(System.currentTimeMillis(), System.currentTimeMillis(), 167));
    }

    public static void testDeleteProduct(){
        Producter.delete("234252342");
        List<String> list = new ArrayList<String>();
        list.add("23343");
        list.add("324234245");
        Producter.delete(list);
    }

    public static void testAddCollectionProduct() {
        System.out.println("通过collection,测试增量更新生产者通过kafka发送通知给消费者单条更新" + Producter.send("test",System.currentTimeMillis(), 68));
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("通过collection,测试增量更新生产者通过kafka发送通知给消费者，批量更新，传入更新时间段" + Producter.send("test",System.currentTimeMillis(), System.currentTimeMillis(), 167));
    }

    public static void testDeleteCollectionProduct(){
       // Producter.delete("test","234252342");
        List<String> list = new ArrayList<String>();
        list.add("23343");
        list.add("324234245");
       // Producter.delete("test",list);
    }

    public static void testCustomMessage(){
        //Producter.sendMsg("catagoryCloud-23432542-234324-4");
    }

}
