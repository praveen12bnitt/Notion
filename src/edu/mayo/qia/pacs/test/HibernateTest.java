package edu.mayo.qia.pacs.test;

import static org.junit.Assert.*;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.mayo.qia.pacs.components.Pool;

@RunWith(SpringJUnit4ClassRunner.class)
public class HibernateTest extends PACSTest {

  @Autowired
  SessionFactory sessionFactory;

  @Autowired
  JdbcTemplate template;

  @Test
  public void test() {

    // Try to save an object
    Session session;
    session = sessionFactory.openSession();
    session.beginTransaction();
    session.save(new Pool("One", "One"));
    session.getTransaction().commit();
    session.close();

    // Verify it!
    assertTrue("Inserted", 1 == template.queryForObject("select count(*) from Pool", Integer.class).intValue());

    // Try to find it
    session = sessionFactory.openSession();
    Criteria criteria = session.createCriteria(Pool.class);
    criteria.add(Restrictions.eq("name", "One"));

    List<Pool> result = criteria.list();
    assertTrue("Fetched pool", result.size() == 1);
    assertTrue("Has the proper comment", result.get(0).path.equals("One"));

    result.get(0).name = "Two";
    session.beginTransaction();
    session.update(result.get(0));
    session.getTransaction().commit();
    assertTrue("Updated", 1 == template.queryForObject("select count(*) from Pool where Name='Two'", Integer.class).intValue());

    // Delete it
    session.beginTransaction();
    session.delete(result.get(0));
    session.getTransaction().commit();
    session.close();
    assertTrue("Deleted", 0 == template.queryForObject("select count(*) from Pool", Integer.class).intValue());
  }
}
