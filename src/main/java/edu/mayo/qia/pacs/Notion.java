package edu.mayo.qia.pacs;

public class Notion {

  static {
    System.setProperty("java.awt.headless", "true");
  }

  public static void main(String[] args) throws Exception {
    System.setProperty("java.awt.headless", "true");
    new NotionApplication().run(args);
  }

}
