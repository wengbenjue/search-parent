package search;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Arrays;
import java.util.List;

//@ComponentScan
//@EnableAutoConfiguration

@SpringBootApplication(exclude = {SolrAutoConfiguration.class})
@EnableTransactionManagement
@EnableNeo4jRepositories(basePackages = "search.common.neo4j.repository")
public class Application extends Neo4jConfiguration {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public org.neo4j.ogm.config.Configuration getConfiguration() {
        org.neo4j.ogm.config.Configuration config = new org.neo4j.ogm.config.Configuration();
        config.driverConfiguration().setDriverClassName("org.neo4j.ogm.drivers.http.driver.HttpDriver");
        config.driverConfiguration().setURI("http://neo4j:csf@54.222.222.172:7474");
        return config;
    }


    @Bean
    public SessionFactory getSessionFactory() {
        return new SessionFactory(getConfiguration(), "search.common.neo4j.domain");
    }

    public Session getSession() throws Exception {
        return super.getSession();
    }

}
