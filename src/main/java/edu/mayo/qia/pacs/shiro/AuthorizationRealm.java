package edu.mayo.qia.pacs.shiro;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Authorizer;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import edu.mayo.qia.pacs.Notion;
import edu.mayo.qia.pacs.NotionApplication;

public class AuthorizationRealm extends AuthorizingRealm {

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    String user = principals.getPrimaryPrincipal().toString();
    Map<String, AuthorizationInfo> authorizationCache = Notion.context.getBean("authorizationCache", Map.class);
    if (authorizationCache.containsKey(user)) {
      return authorizationCache.get(user);
    }

    JdbcTemplate template = Notion.context.getBean(JdbcTemplate.class);
    final SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
    // Find all our permissions
    // @formatter:off
    template.query("select GR.PoolKey as PoolKey, GR.IsPoolAdmin as IsPoolAdmin, GR.IsCoordinator as IsCoordinator "
        + "from GROUPROLE GR, USERGROUP UG, GROUPS G, USERS U "
        + " where U.UserName = ? and U.UserKey = UG.UserKey and UG.GroupKey = GR.GroupKey",
        new Object[] { user },
        // @formatter:on
        new RowCallbackHandler() {

          @Override
          public void processRow(ResultSet rs) throws SQLException {
            int poolKey = rs.getInt("PoolKey");
            if (rs.getBoolean("IsPoolAdmin")) {
              info.addObjectPermission(new WildcardPermission("pool:*:" + poolKey));
              info.addObjectPermission(new WildcardPermission("pool:admin:" + poolKey));
              return;
            }
            if (rs.getBoolean("IsCoordinator")) {
              info.addObjectPermission(new WildcardPermission("pool:query:" + poolKey));
              info.addObjectPermission(new WildcardPermission("pool:coordinator:" + poolKey));
            }
          }
        });

    // Permissions everyone has
    info.addObjectPermission(new WildcardPermission("pool:list"));

    if (template.queryForObject("select IsAdmin from USERS where Username = ?", Boolean.class, user)) {
      info.addObjectPermission(new WildcardPermission("admin:*"));
      info.addObjectPermission(new WildcardPermission("pool:*"));
    }
    authorizationCache.put(user, info);
    return info;
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    // TODO Auto-generated method stub
    return null;
  }

}
