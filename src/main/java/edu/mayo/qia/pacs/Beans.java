package edu.mayo.qia.pacs;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.googlecode.flyway.core.Flyway;

import edu.mayo.qia.pacs.components.PoolManager;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

@Configuration
@EnableScheduling
@EnableAsync
public class Beans {
  static Logger logger = Logger.getLogger(Beans.class);

  @Autowired
  PoolManager poolManager;

  @Autowired
  NotionConfiguration configuration;

  @Bean
  freemarker.template.Configuration freemarker() throws IOException {
    // Freemarker
    BeansWrapper.getDefaultInstance().setExposeFields(true);
    freemarker.template.Configuration cfg = new freemarker.template.Configuration();
    // Where do we load the templates from:

    ArrayList<TemplateLoader> loaders = new ArrayList<TemplateLoader>();
    if (configuration.notion.templatePath != null) {
      loaders.add(new FileTemplateLoader(new File(configuration.notion.templatePath)));
    }
    loaders.add(new ClassTemplateLoader(this.getClass(), "/templates"));
    cfg.setTemplateLoader(new MultiTemplateLoader(loaders.toArray(new TemplateLoader[] {})));

    // Some other recommended settings:
    cfg.setIncompatibleImprovements(new Version(2, 3, 20));
    cfg.setDefaultEncoding("UTF-8");
    cfg.setLocale(Locale.US);
    DefaultObjectWrapper ow = new DefaultObjectWrapper();
    ow.setExposeFields(true);
    ow.setExposureLevel(BeansWrapper.EXPOSE_SAFE);
    cfg.setObjectWrapper(ow);
    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    return cfg;
  }

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  @Bean
  @DependsOn("flyway")
  public JdbcTemplate template() throws SQLException {
    JdbcTemplate template = new JdbcTemplate();
    template.setDataSource(dataSource());
    return template;
  }

  @Bean
  public DataSource dataSource() {
    return NotionApplication.dataSource;
  }

  @Bean
  @DependsOn("dataSource")
  public Flyway flyway() throws Exception {
    Flyway flyway = new Flyway();
    flyway.setDataSource(dataSource());
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

  /*
   * @Bean public HttpServer httpServer() throws IOException { // See if the
   * html directory exists File htmlDirectory = new File(PACS.directory,
   * "html"); if (!htmlDirectory.exists()) { htmlDirectory.mkdirs(); // Copy
   * resources into it }
   * 
   * // We want to manage the server threads ourselves // The following code was
   * extracted from ServerConfiguration and Google HttpServer server = new
   * HttpServer(); final NetworkListener listener = new
   * NetworkListener("grizzly", NetworkListener.DEFAULT_NETWORK_HOST,
   * PACS.RESTPort); listener.setSecure(false);
   * listener.getTransport().getWorkerThreadPoolConfig().setCorePoolSize(1);
   * listener.getTransport().getWorkerThreadPoolConfig().setMaxPoolSize(10);
   * listener.getTransport().setSelectorRunnersCount(5);
   * server.addListener(listener); // Here's the recommended approach //
   * http://jersey
   * .576304.n2.nabble.com/Right-way-to-create-embedded-grizzly-with
   * -already-instantiated-Application-tt1470802.html#a1484718 ResourceConfig rc
   * = new PackagesResourceConfig("edu.mayo.qia.pacs.rest");
   * rc.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, true);
   * 
   * SpringComponentProviderFactory handler = new
   * SpringComponentProviderFactory(rc, PACS.context); HttpHandler processor =
   * ContainerFactory.createContainer(HttpHandler.class, rc, handler);
   * server.getServerConfiguration().addHttpHandler(processor, "/rest");
   * 
   * LocalStaticHttpHandler cpHandler = new
   * LocalStaticHttpHandler(PACS.class.getClassLoader(), "html/", "public/");
   * cpHandler
   * .setFileCacheEnabled(Boolean.parseBoolean(System.getProperty("NOTION_CACHE"
   * , "true")));
   * 
   * StaticHttpHandler staticHandler = new
   * StaticHttpHandler(htmlDirectory.getAbsolutePath());
   * staticHandler.setFileCacheEnabled(false);
   * server.getServerConfiguration().addHttpHandler(cpHandler, "/");
   * 
   * server.start(); return server; }
   */
}
