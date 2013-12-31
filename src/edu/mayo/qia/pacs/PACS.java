package edu.mayo.qia.pacs;

import java.io.File;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;
import org.h2.tools.Server;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class PACS {

  public static File directory;
  public static AnnotationConfigApplicationContext context;
  public static int DICOMPort = -1;
  public static int RESTPort = -1;
  public static boolean isDBInMemory = false;
  public static final String sorterQueue = "pacs.sorter";
  static Logger logger = Logger.getLogger(PACS.class);

  public static String version = "1.0.0.0";

  static {
    System.setProperty("java.awt.headless", "true");
    logger.debug("Using a headless environment: " + java.awt.GraphicsEnvironment.isHeadless());
  }

  /**
   * Start the research PACS in the current directory, or the one specified on
   * the command line.
   * 
   * @param args
   */
  public static void main(String[] args) {
    new PACS(args);
  }

  public PACS(List<String> args) {
    this(args.toArray(new String[] {}));
  }

  public PACS(String[] args) {
    Options options = new Options();
    options.addOption("p", "port", true, "Port to listen for DICOM traffic, default is 11117");
    options.addOption("r", "rest", true, "Port to listen for REST traffic, default is 11118");
    options.addOption("m", "memoryDB", false, "Start services in memory (DB only)");
    options.addOption("h", "help", false, "Print help and exit");
    options.addOption("d", "db", true, "Start the embedded DB Web server on the given port (normally 8082), will not start without this option");
    CommandLine commandLine = null;
    try {
      commandLine = new GnuParser().parse(options, args);
    } catch (ParseException e) {
      System.out.println("Error while parsing command line: " + e.getMessage() + "\n");
      System.exit(1);
    }
    if (commandLine.hasOption("help")) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.setArgName("[directory]");
      // @formatter:off
      String footer = "\nStart the Notion PACS system using [directory] for data storage.  If not specified "
        + "defaults to the current working directory (" + new File(System.getProperty("user.dir")).toString() + ").  "
        + "By default the REST api is started on port 11118, with the web app being served at http://localhost:11118 "
        + "\nThe DICOM listener starts on port 11117 (can be changed with a --port) and provides "
        + "C-ECHO, C-MOVE, C-STORE and C-FIND services.  "
        + "Notion serves as a full DICOM query / retrive SCP.\n"
        + "Database administration can be handled via the bundled web interface.  By default, http://localhost:8082, if the "
        + " --db option is given.  It will not start up otherwise. "
        + "The JDBC connection URL is given in the log message of the server.";
      formatter.printHelp("Notion [options] [directory]", "options:", options, footer);
      // @formatter:on
      System.exit(1);
    }

    if (commandLine.getArgs().length >= 1) {
      directory = new File(commandLine.getArgs()[0]);
    } else {
      directory = new File(System.getProperty("user.dir"));
    }
    if (!directory.exists()) {
      directory.mkdirs();
    }

    DICOMPort = Integer.parseInt(commandLine.getOptionValue("port", "11117"));
    RESTPort = Integer.parseInt(commandLine.getOptionValue("rest", "11118"));

    isDBInMemory = commandLine.hasOption("m");

    // Load our beans
    context = new AnnotationConfigApplicationContext();
    context.scan("edu.mayo.qia.pacs", "edu.mayo.qia.pacs.dicom");
    context.refresh();

    logger.info("\n=====\n\nResearch PACS Started\n\tREST PORT: " + RESTPort + "\n\tDICOM Port: " + DICOMPort + "\n\tHome directory: " + directory.getAbsolutePath() + "\n\tURL: http://localhost:" + RESTPort + "\n\n=====\n");
    if (commandLine.hasOption("db")) {
      logger.info("Starting webserver for embedded database");
      try {
        DataSource ds = context.getBean("dataSource", DataSource.class);
        BasicDataSource bds = (BasicDataSource) ds;
        Server server = Server.createWebServer("-webPort", commandLine.getOptionValue("db", "8082")).start();
        logger.info("\n======\n\nEmbedded DB Server started:\n\t" + server.getURL() + "\n\tDB URL: " + bds.getUrl() + "\n\n======");
      } catch (Exception e) {
        logger.error("Error starting web server", e);
      }
    }
  }
}
