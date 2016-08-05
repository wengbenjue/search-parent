package search.solr.client;

import search.common.entity.searchinterface.NiNi;
import search.solr.client.searchInterface.SearchInterface;

/**
 * Created by soledede on 2016/2/22.
 */
public class SearchInterfaceTest {

    public static void main(String[] args) {
        testSearchByKeywords();
    }


    public static void testSearchByKeywords() {
        NiNi re =   SearchInterface.searchByKeywords("mergescloud", "screencloud", "圆筒", 363, null, null, 0, 10);
        System.out.println(re);
    }

    public void testSearch() {
        // SearchInterface.searchByKeywords()
    }


}
