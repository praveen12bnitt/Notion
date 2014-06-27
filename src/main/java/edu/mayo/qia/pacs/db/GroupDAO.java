package edu.mayo.qia.pacs.db;

import org.hibernate.SessionFactory;

import edu.mayo.qia.pacs.components.Group;

public class GroupDAO extends SimpleDAO<Group> {

  public GroupDAO(SessionFactory sessionFactory) {
    super(sessionFactory);
  }

}
