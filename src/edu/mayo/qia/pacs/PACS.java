package edu.mayo.qia.pacs;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class PACS {

  public static File directory;
  public static AnnotationConfigApplicationContext context;
  public static int DICOMPort = -1;
  public static int RESTPort = -1;
  public static final String sorterQueue = "pacs.sorter";
  static Logger logger = Logger.getLogger(PACS.class);

  /**
   * Start the research PACS in the current directory, or the one specified on
   * the command line.
   * 
   * @param args
   */
  public static void main(String[] args) {
    new PACS(args);
  }

  public PACS(String[] args) {
    Options options = new Options();
    options.addOption("p", "port", true, "Port to listen for DICOM traffic, default is 11117");
    options.addOption("r", "rest", true, "Port to listen for REST traffic, default is 11118");
    CommandLine commandLine = null;
    try {
      commandLine = new GnuParser().parse(options, args);
    } catch (ParseException e) {
      System.out.println("Error while parsing command line: " + e.getMessage() + "\n");
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

    // Load our beans
    context = new AnnotationConfigApplicationContext();
    context.scan("edu.mayo.qia.pacs", "edu.mayo.qia.pacs.dao");
    context.refresh();

    logger.info("\n=====\n\nResearch PACS Started\n\tREST PORT: " + RESTPort + "\n\tDICOM Port: " + DICOMPort + "\n\tHome directory: " + directory.getAbsolutePath() + "\n\tURL: http://localhost:" + RESTPort + "\n\n=====\n");
  }
}
