package edu.mayo.qia.pacs;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.googlecode.flyway.core.Flyway;
import com.sun.jersey.api.container.ContainerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.spi.spring.container.SpringComponentProviderFactory;

import edu.mayo.qia.pacs.components.PoolManager;
import edu.mayo.qia.pacs.http.LocalStaticHttpHandler;

@Configuration
@EnableScheduling
@EnableAsync
public class Beans {
  static Logger logger = Logger.getLogger(Beans.class);

  @Autowired
  PoolManager poolManager;

  @Bean
  @DependsOn("flyway")
  public LocalSessionFactoryBean sessionFactory() throws SQLException {
    Properties hibernateProperties = new Properties();
    // hibernateProperties.setProperty("hibernate.show_sql", "true");
    // setProperty("hibernate.globally_quoted_identifiers", "true");
    hibernateProperties.setProperty("hibernate.hbm2ddl.auto", "validate");

    // Using a thread session means that getCurrentSession() will return a
    // thread-local
    // Session for Hibernate, and allow the Jersey API to operate across
    // multiple objects correctly.
    hibernateProperties.setProperty("hibernate.current_session_context_class", "thread");
    LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
    sessionFactory.setDataSource(dataSource());
    sessionFactory.setPackagesToScan(new String[] { "edu.mayo.qia.pacs.components" });
    sessionFactory.setHibernateProperties(hibernateProperties);

    return sessionFactory;
  }

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  @Bean
  public HibernateTransactionManager txManager() throws SQLException {
    HibernateTransactionManager tx = new HibernateTransactionManager();
    tx.setSessionFactory(sessionFactory().getObject());
    return tx;
  }

  @Bean
  public DataSource dataSource() throws SQLException {
    String path = new File(PACS.directory, "DB").getPath();
    String driverClass = "org.apache.derby.jdbc.EmbeddedDriver";

    String url;
    if (PACS.isDBInMemory) {
      url = "jdbc:derby:memory:PACS;create=true";
    } else {
      url = "jdbc:derby:directory:" + path + ";create=true";
    }
    // Emulate our Connection pooling setup
    BasicDataSource dbcp = new BasicDataSource();
    dbcp.setDriverClassName(driverClass);
    dbcp.setUrl(url);
    dbcp.setDefaultAutoCommit(true);
    return dbcp;
  }

  @Bean
  public PlatformTransactionManager transactionManager() throws SQLException {
    return new DataSourceTransactionManager(dataSource());
  }

  @Bean
  public TransactionTemplate transactionTemplate() throws SQLException {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager());
    // The design demands
    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return transactionTemplate;
  }

  @Bean
  @DependsOn("flyway")
  public JdbcTemplate template() throws SQLException {
    JdbcTemplate template = new JdbcTemplate();
    template.setDataSource(dataSource());
    return template;
  }

  @Bean
  @DependsOn("dataSource")
  public Flyway flyway() throws Exception {
    Flyway flyway = new Flyway();
    flyway.setDataSource(dataSource());
    flyway.setInitOnMigrate(true);
    flyway.migrate();
    return flyway;
  }

  @Bean
  public TaskScheduler taskScheduler() {
    ThreadPoolTaskScheduler s = new ThreadPoolTaskScheduler();
    s.setPoolSize(2);
    return s;
  }

  @Bean
  public Executor executor() {
    return Executors.newFixedThreadPool(4);
  }

  @Bean
  public HttpServer httpServer() throws IOException {
    // See if the html directory exists
    File htmlDirectory = new File(PACS.directory, "html");
    if (!htmlDirectory.exists()) {
      htmlDirectory.mkdirs();
      // Copy resources into it
    }

    // We want to manage the server threads ourselves
    // The following code was extracted from ServerConfiguration and Google
    HttpServer server = new HttpServer();
    final NetworkListener listener = new NetworkListener("grizzly", NetworkListener.DEFAULT_NETWORK_HOST, PACS.RESTPort);
    listener.setSecure(false);
    listener.getTransport().getWorkerThreadPoolConfig().setCorePoolSize(1);
    listener.getTransport().getWorkerThreadPoolConfig().setMaxPoolSize(10);
    listener.getTransport().setSelectorRunnersCount(5);
    server.addListener(listener);
    // Here's the recommended approach
    // http://jersey.576304.n2.nabble.com/Right-way-to-create-embedded-grizzly-with-already-instantiated-Application-tt1470802.html#a1484718
    ResourceConfig rc = new PackagesResourceConfig("edu.mayo.qia.pacs.rest");
    rc.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, true);

    SpringComponentProviderFactory handler = new SpringComponentProviderFactory(rc, PACS.context);
    HttpHandler processor = ContainerFactory.createContainer(HttpHandler.class, rc, handler);
    server.getServerConfiguration().addHttpHandler(processor, "/rest");

    LocalStaticHttpHandler cpHandler = new LocalStaticHttpHandler(PACS.class.getClassLoader(), "html/", "public/");
    cpHandler.setFileCacheEnabled(Boolean.parseBoolean(System.getProperty("NOTION_CACHE", "true")));

    StaticHttpHandler staticHandler = new StaticHttpHandler(htmlDirectory.getAbsolutePath());
    staticHandler.setFileCacheEnabled(false);
    server.getServerConfiguration().addHttpHandler(cpHandler, "/");

    server.start();
    return server;
  }
}
