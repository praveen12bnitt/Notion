package edu.mayo.qia.pacs;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Notion {

  static {
    System.setProperty("java.awt.headless", "true");
  }

  public static void main(String[] args) throws Exception {
    System.setProperty("java.awt.headless", "true");
    new NotionApplication().run(args);
  }

  public static void checkAssertion(boolean v, String message) throws Exception {
    if (!v) {
      throw new Exception(message);
    }
  }

  public static AnnotationConfigApplicationContext context;

}
