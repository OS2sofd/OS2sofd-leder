package dk.digitalidentity.security;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import dk.digitalidentity.config.StaticContextAccessor;
import dk.digitalidentity.service.SOFDService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import dk.digitalidentity.config.RoleConstants;
import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.TokenUser;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SecurityUtil {
	
	@Autowired
	private HttpServletRequest request;
	
	public static boolean isLoggedIn() {
		return SecurityContextHolder.getContext().getAuthentication() != null;
	}
	
	public static String getPersonUuid() {
		var userUuid = getUserUuid();
		String personUuid = null;
		if( userUuid != null)
		{
			personUuid = StaticContextAccessor.getBean(SOFDService.class).ConvertKombitUuidToPersonUuid(userUuid);
		}
		return personUuid;
	}

	private static String getUserUuid() {
		String uuid = null;

		if (isLoggedIn()) {
			String principal = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			String[] split = principal.split(",");
			uuid = split[split.length-1].replace("Serial=", "");

			if (uuid.length() != 36) {
				log.warn("UUID for logged in user is invalid: " + uuid);
			}
		}

		return uuid;
	}

	
	public static String getUser() {
		String name = null;
		
		if (isLoggedIn()) {
			name = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		}

		return name;
	}
	
	public static boolean hasRole(String role) {
        boolean hasRole = false;
        if (isLoggedIn()) {
            for (GrantedAuthority grantedAuthority : (SecurityContextHolder.getContext().getAuthentication()).getAuthorities()) {
                if (grantedAuthority.getAuthority().equals(role)) {
                    hasRole = true;
                }
            }
        }
        return hasRole;
    }
	
	public static boolean getBooleanAttribute(String key) {
		if (isLoggedIn()) {
			Object o = ((TokenUser) SecurityContextHolder.getContext().getAuthentication().getDetails()).getAttributes().get(key);
			if (o == null) {
				return false;
			}
			
			return (boolean) o;
		}

		return false;
	}
	
	public void switchToManagerContext() {
		TokenUser tokenUser = ((TokenUser) SecurityContextHolder.getContext().getAuthentication().getDetails());
		
		List<SamlGrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SamlGrantedAuthority(RoleConstants.MANAGER));
		
		if (getBooleanAttribute("admin")) {
			authorities.add(new SamlGrantedAuthority(RoleConstants.ADMIN));
		}
		
		tokenUser.setAuthorities(authorities);
		
		cleanCache(tokenUser, authorities);
	}
	
	public void switchToSubstituteContext() {
		TokenUser tokenUser = ((TokenUser) SecurityContextHolder.getContext().getAuthentication().getDetails());
		
		List<SamlGrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SamlGrantedAuthority(RoleConstants.SUBSTITUTE));
		
		if (getBooleanAttribute("admin")) {
			authorities.add(new SamlGrantedAuthority(RoleConstants.ADMIN));
		}
		
		tokenUser.setAuthorities(authorities);
		
		cleanCache(tokenUser, authorities);
	}
	
	public void switchToAdminContext() {
		TokenUser tokenUser = ((TokenUser) SecurityContextHolder.getContext().getAuthentication().getDetails());
		
		List<SamlGrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SamlGrantedAuthority(RoleConstants.ADMIN));
		tokenUser.setAuthorities(authorities);
		
		cleanCache(tokenUser, authorities);
		
	}

	private void cleanCache(TokenUser tokenUser, List<SamlGrantedAuthority> authorities) {
		
		// so spring really likes caching this object, so we need to make sure Spring knows about the new version
		if (SecurityContextHolder.getContext() != null &&
			SecurityContextHolder.getContext().getAuthentication() != null &&
			SecurityContextHolder.getContext().getAuthentication() instanceof UsernamePasswordAuthenticationToken) {

			SecurityContext securityContext = SecurityContextHolder.getContext();
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(getUser(), null, authorities);
			
			authentication.setDetails(tokenUser);
			SecurityContextHolder.getContext().setAuthentication(authentication);
			
			if (request != null) {
				HttpSession session = request.getSession(true);
				session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
			}
		}
	}
}
