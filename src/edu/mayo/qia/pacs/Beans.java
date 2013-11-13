package edu.mayo.qia.pacs;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.sql.DataSource;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.plugin.StatisticsBrokerPlugin;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.ObjectMapper;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;
import org.springframework.jms.support.converter.MappingJacksonMessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.googlecode.flyway.core.Flyway;
import com.sun.jersey.api.container.ContainerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.spi.spring.container.SpringComponentProviderFactory;

@Configuration
@EnableScheduling
@EnableAsync
public class Beans {
  static Logger logger = Logger.getLogger(Beans.class);

  @Autowired
  Sorter sorter;

  @Bean
  public LocalSessionFactoryBean sessionFactory() throws SQLException {
    Properties hibernateProperties = new Properties();
    hibernateProperties.setProperty("hibernate.show_sql", "true");
    // setProperty("hibernate.globally_quoted_identifiers", "true");
    hibernateProperties.setProperty("hibernate.hbm2ddl.auto", "validate");
    LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
    sessionFactory.setDataSource(dataSource());
    sessionFactory.setPackagesToScan(new String[] { "edu.mayo.qia.pacs.components" });
    sessionFactory.setHibernateProperties(hibernateProperties);

    return sessionFactory;
  }

  @Bean
  public DataSource dataSource() throws SQLException {
    String path = new File(PACS.directory, "DB").getPath();
    String driverClass = "org.apache.derby.jdbc.EmbeddedDriver";
    String url = "jdbc:derby:directory:" + path + ";create=true";
    // Emulate our Connection pooling setup
    BasicDataSource dbcp = new BasicDataSource();
    dbcp.setDriverClassName(driverClass);
    dbcp.setUrl(url);
    dbcp.setDefaultAutoCommit(true);
    return dbcp;

  }

  @Bean
  public ConnectionFactory connectionFactory() throws Exception {
    BrokerService broker = new BrokerService();
    broker.setPersistent(true);
    broker.setDataDirectoryFile(new File(PACS.directory, "MQ"));
    broker.setEnableStatistics(true);
    broker.setUseShutdownHook(false);
    StatisticsBrokerPlugin statsPlugin = new StatisticsBrokerPlugin();
    broker.setPlugins(new BrokerPlugin[] { statsPlugin });
    broker.start();
    ConnectionFactory f = new ActiveMQConnectionFactory("vm://localhost");
    return f;
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
  @DependsOn("flyway")
  public DefaultMessageListenerContainer sorterContainer() throws Exception {
    DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
    MessageListenerAdapter adapter = new MessageListenerAdapter();
    adapter.setDelegate(sorter);
    container.setMessageListener(adapter);
    container.setTaskExecutor(taskExecutor());
    container.setMaxConcurrentConsumers(3);
    container.setMaxMessagesPerTask(10);
    container.setDestinationName(PACS.sorterQueue);
    container.setConnectionFactory(connectionFactory());
    container.setSessionTransacted(true);
    container.setAutoStartup(true);
    return container;
  }

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_OBJECT);
    return objectMapper;
  }

  @Bean
  public MappingJacksonMessageConverter messageConverter() {
    MappingJacksonMessageConverter messageConverter = new MappingJacksonMessageConverter();
    messageConverter.setTargetType(MessageType.TEXT);
    messageConverter.setTypeIdPropertyName("JavaClass");
    messageConverter.setObjectMapper(objectMapper());
    return messageConverter;
  }

  @Bean
  @DependsOn("flyway")
  public JmsTemplate jmsTemplate() throws Exception {
    JmsTemplate jmsTemplate = new JmsTemplate();
    jmsTemplate.setConnectionFactory(connectionFactory());
    jmsTemplate.setMessageConverter(messageConverter());
    return jmsTemplate;
  }

  @Bean
  public TaskScheduler taskScheduler() {
    return new ThreadPoolTaskScheduler();
  }

  @Bean
  @DependsOn("flyway")
  public ThreadPoolTaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
    taskExecutor.setCorePoolSize(2);
    taskExecutor.setMaxPoolSize(100);
    return taskExecutor;
  }

  @Bean
  public HttpServer httpServer() throws IOException {
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
    // HttpServer server =
    // GrizzlyServerFactory.createHttpServer(URI.create("http://" +
    // NetworkListener.DEFAULT_NETWORK_HOST + ":" + PACS.RESTPort + "/"), rc);

    SpringComponentProviderFactory handler = new SpringComponentProviderFactory(rc, PACS.context);
    HttpHandler processor = ContainerFactory.createContainer(HttpHandler.class, rc, handler);
    server.getServerConfiguration().addHttpHandler(processor, "");

    server.start();
    return server;
  }
}
