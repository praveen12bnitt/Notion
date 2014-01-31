package edu.mayo.qia.pacs;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
public class SessionManager {

  public Session session;

  @Autowired
  SessionFactory sessionFactory;

  @PostConstruct
  void createSession() {
    session = sessionFactory.openSession();
  }

  @PreDestroy
  void close() {
    session.close();
  }
}
