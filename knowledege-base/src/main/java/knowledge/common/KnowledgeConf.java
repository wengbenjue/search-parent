package knowledge.common;

import knowledge.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by soledede.weng on 2016/9/2.
 */
public class KnowledgeConf {
    Logger log = LoggerFactory.getLogger(KnowledgeConf.class);

  public static Map<String, Object> settings = new ConcurrentHashMap<String, Object>();


    public KnowledgeConf loadPropertiesFromResource() {
        Properties prop = new Properties();
        try {
            loadFromSystemProperties();
            prop.load(KnowledgeConf.class.getClassLoader().getResourceAsStream("config/application.properties"));
            String user = prop.getProperty("neo4juser");
            String passwd = prop.getProperty("neo4jpasswd");
            String url = prop.getProperty("neo4jurl");
            set("neo4juser", user);
            set("neo4jpasswd", passwd);
            set("neo4jurl", url);
        } catch (IOException e) {
            log.error("加载配置失败！", e);
        }
        return this;
    }

    public KnowledgeConf loadFromSystemProperties() {
        Map<String, Object> props = Utils.getSystemProperties();
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            set(key, value);
        }
        return this;
    }


    public KnowledgeConf set(String key, Object value) {
        if (key == null) {
            throw new NullPointerException("null key");
        }
        if (value == null) {
            throw new NullPointerException("null value for " + key);
        }
        settings.put(key, value);
        return this;
    }

    /**
     * Get a parameter; throws a NoSuchElementException if it's not set
     */
    public String get(String key) {
        if (settings.containsKey(key))
            return settings.get(key).toString();
        else return null;
    }

    /**
     * Get a parameter, falling back to a default if not set
     */
    public String get(String key, String defaultValue) {
        if (settings.containsKey(key))
            return settings.get(key).toString();
        else return defaultValue;
    }


    /**
     * Get a parameter as an integer, falling back to a default if not set
     */
    public Integer getInt(String key, Integer defaultValue) {
        if (settings.containsKey(key))
            return Integer.parseInt(settings.get(key).toString());
        else return defaultValue;
    }


    public Boolean getBoolean(String key, Boolean defaultValue) {
        if (settings.containsKey(key))
            return Boolean.parseBoolean(settings.get(key).toString());
        else return defaultValue;
    }


    /**
     * Does the configuration contain a given parameter?
     */
    public Boolean contains(String key) {
        return settings.containsKey(key);
    }
}

