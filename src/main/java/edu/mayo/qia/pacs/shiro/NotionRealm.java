package edu.mayo.qia.pacs.shiro;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.realm.jdbc.JdbcRealm;

import edu.mayo.qia.pacs.Notion;
import edu.mayo.qia.pacs.NotionConfiguration;

public class NotionRealm extends JdbcRealm {

  public NotionRealm() {
    NotionConfiguration configuration = Notion.context.getBean("configuration", NotionConfiguration.class);
    setDataSource(Notion.context.getBean("dataSource", DataSource.class));
    setSaltStyle(SaltStyle.COLUMN);
    setAuthenticationQuery("select password, salt from users where email = ?");
    HashedCredentialsMatcher matcher = new HashedCredentialsMatcher(configuration.notion.hashAlgorithm);
    matcher.setHashIterations(configuration.notion.hashIterations);
    matcher.setStoredCredentialsHexEncoded(true);
    setCredentialsMatcher(matcher);
  }

  @Override
  protected Set<String> getRoleNamesForUser(Connection conn, String username) throws SQLException {
    return new HashSet<String>();
  }
}