package edu.mayo.qia.pacs.shiro;

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
    setUserRolesQuery("select roles.role from roles, user_role, users where roles.id = user_role.id and users.id = user_role.user_id and users.email = ?");
    setPermissionsQuery("select role_permission.permission from role_permission, roles where roles.role = ?");
    HashedCredentialsMatcher matcher = new HashedCredentialsMatcher(configuration.notion.hashAlgorithm);
    matcher.setHashIterations(configuration.notion.hashIterations);
    matcher.setStoredCredentialsHexEncoded(true);
    setCredentialsMatcher(matcher);
  }

}