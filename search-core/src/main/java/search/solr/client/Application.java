package search.solr.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import search.es.client.util.DataManager;

//@ComponentScan
//@EnableAutoConfiguration
@SpringBootApplication(exclude = {SolrAutoConfiguration.class})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
