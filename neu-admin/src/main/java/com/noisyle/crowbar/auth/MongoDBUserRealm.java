package com.noisyle.crowbar.auth;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import com.noisyle.crowbar.constant.AdminConstant;
import com.noisyle.crowbar.core.base.IUser;
import com.noisyle.crowbar.core.vo.UserContext;
import com.noisyle.crowbar.model.Admin;
import com.noisyle.crowbar.service.AdminService;

public class MongoDBUserRealm extends AuthorizingRealm {
	final protected Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private AdminService adminService;

	@Override
	public String getName() {
		return "userRealm";
	}

	@Override
	public boolean supports(AuthenticationToken token) {
		// 仅支持 UsernamePasswordToken 类型的 Token
		return token instanceof UsernamePasswordToken;
	}

	/**
	 * 身份验证
	 */
	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authcToken) {
		UsernamePasswordToken token = (UsernamePasswordToken) authcToken;
		IUser user = adminService.getAdminByLoginName(token.getUsername());
		if (user != null) {
			adminService.initUserContext(user);
			return new SimpleAuthenticationInfo(user.getId(), user.getPassword(), getName());
		} else {
			return null;
		}
	}

	/**
	 * 授权操作
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		IUser user = ((UserContext) SecurityUtils.getSubject().getSession()
				.getAttribute(AdminConstant.SESSION_KEY_USER_CONTEXT)).getUser();
		SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
		if (user.getRole() != null) {
			info.addRoles(CollectionUtils.arrayToList(user.getRole().split(",")));
		}
		return info;
	}

	public void clearCachedAuthorizationInfo(Admin user) {
		SimplePrincipalCollection principals = new SimplePrincipalCollection(user.getId(), getName());
		this.clearCachedAuthorizationInfo(principals);
	}

	/**
	 * 重写获取授权CacheKey方法
	 * 保证不同Cache实现下，上面的public clearCachedAuthorizationInfo都可以正确清除授权信息
	 */
	@Override
	protected Object getAuthorizationCacheKey(PrincipalCollection principals) {
		return principals.getPrimaryPrincipal();
	}
}
