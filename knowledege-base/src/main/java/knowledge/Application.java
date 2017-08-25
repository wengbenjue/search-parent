package knowledge;

import knowledge.common.KnowledgeConf;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.event.AfterDeleteEvent;
import org.springframework.data.neo4j.event.AfterSaveEvent;
import org.springframework.data.neo4j.event.BeforeSaveEvent;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@EnableNeo4jRepositories(basePackages = "knowledge.repository")
public class Application extends Neo4jConfiguration {
    public static KnowledgeConf conf = new KnowledgeConf();

    public static void main(String[] args) {
        conf = conf.loadPropertiesFromResource();
        new SpringApplication(Application.class).run(args);
    }


    @Bean

    public org.neo4j.ogm.config.Configuration getConfiguration() {
        org.neo4j.ogm.config.Configuration config = new org.neo4j.ogm.config.Configuration();
        config.driverConfiguration().setDriverClassName("org.neo4j.ogm.drivers.http.driver.HttpDriver");
        config.driverConfiguration().setURI("http://" + conf.get("neo4juser") + ":" + conf.get("neo4jpasswd") + "@" + conf.get("neo4jurl"));
        return config;
    }


    @Bean
    public SessionFactory getSessionFactory() {
        return new SessionFactory(getConfiguration(), "knowledge.domain");
    }

    public Session getSession() throws Exception {
        return super.getSession();
    }


    @Bean
    ApplicationListener<BeforeSaveEvent> beforeSaveEventApplicationListener() {
        return new ApplicationListener<BeforeSaveEvent>() {
            @Override
            public void onApplicationEvent(BeforeSaveEvent event) {
                //Object entity = event.getEntity();
                //System.out.println("Before save of: " + entity);
            }
        };
    }

    @Bean
    ApplicationListener<AfterSaveEvent> afterSaveEventApplicationListener() {
        return new ApplicationListener<AfterSaveEvent>() {
            @Override
            public void onApplicationEvent(AfterSaveEvent event) {
                //  Object entity = event.getEntity();
                //  System.out.println("Before save of: " + entity);
            }
        };
    }

    @Bean
    ApplicationListener<AfterDeleteEvent> deleteEventApplicationListener() {
        return new ApplicationListener<AfterDeleteEvent>() {
            @Override
            public void onApplicationEvent(AfterDeleteEvent event) {
                //  Object entity = event.getEntity();
                //  System.out.println("Before save of: " + entity);
            }
        };
    }

}
