package dk.digitalidentity.security;

import dk.digitalidentity.config.RoleConstants;
import dk.digitalidentity.config.StaticContextAccessor;
import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.TokenUser;
import dk.digitalidentity.service.SOFDFunctionAssignmentService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

@Component
@Slf4j
public class SecurityUtil {
	
	@Autowired
	private HttpServletRequest request;

	public static RolePostProcesser.LoggedInUser getLoggedInUser() {
		try {
			if( SecurityContextHolder.getContext().getAuthentication() != null ) {
				var tokenUser = ((TokenUser) SecurityContextHolder.getContext().getAuthentication().getDetails());
				return (RolePostProcesser.LoggedInUser) tokenUser.getAttributes().get(RolePostProcesser.LOGGED_IN_USER_ATTRIBUTE);
			}
			return null;
		}
		catch( Exception e ) {
			return null;
		}
	}

	@SneakyThrows
	public static RolePostProcesser.ManagerUIContext getCurrentContext() {
		if( SecurityContextHolder.getContext().getAuthentication() != null && getLoggedInUser() != null ) {
			var tokenUser = ((TokenUser) SecurityContextHolder.getContext().getAuthentication().getDetails());
			var context = (RolePostProcesser.ManagerUIContext) tokenUser.getAttributes().get(RolePostProcesser.CURRENT_CONTEXT_ATTRIBUTE);
			if( context == null ) {
				// no context set yet - set one
				context = getLoggedInUser().contexts().stream().findFirst().orElse(null);
				if( context != null ) {
					setCurrentContext(context);
				}
			}
			return context;
		}
		else {
			return null;
		}
	}

	public static void setCurrentContext(RolePostProcesser.ManagerUIContext managerUIContext) {
		if( SecurityContextHolder.getContext().getAuthentication() != null ) {
			var tokenUser = ((TokenUser) SecurityContextHolder.getContext().getAuthentication().getDetails());
			tokenUser.getAttributes().put(RolePostProcesser.CURRENT_CONTEXT_ATTRIBUTE,managerUIContext);
			tokenUser.setAuthorities( List.of(new SamlGrantedAuthority(managerUIContext.role())) );
			StaticContextAccessor.getBean(SecurityUtil.class).cleanCache(tokenUser);
			StaticContextAccessor.getBean(SOFDFunctionAssignmentService.class).clearCache();
		}
	}

	public static boolean isAdmin() {
		return SecurityUtil.getCurrentContext().role().equalsIgnoreCase(RoleConstants.ADMIN);
	}

	private void cleanCache(TokenUser tokenUser) {

		// so spring really likes caching this object, so we need to make sure Spring knows about the new version
		// if we don't do this, the various hasRole checks from thymeleaf will fail.
		if (SecurityContextHolder.getContext() != null &&
			SecurityContextHolder.getContext().getAuthentication() != null &&
			SecurityContextHolder.getContext().getAuthentication() instanceof UsernamePasswordAuthenticationToken) {

			SecurityContext securityContext = SecurityContextHolder.getContext();
			var user = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null, tokenUser.getAuthorities());

			authentication.setDetails(tokenUser);
			SecurityContextHolder.getContext().setAuthentication(authentication);

			if (request != null) {
				HttpSession session = request.getSession(true);
				session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
			}
		}
	}
}
