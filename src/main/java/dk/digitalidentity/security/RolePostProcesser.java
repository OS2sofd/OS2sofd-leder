package dk.digitalidentity.security;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import dk.digitalidentity.config.RoleConstants;
import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.SamlLoginPostProcessor;
import dk.digitalidentity.samlmodule.model.TokenUser;

@Component
public class RolePostProcesser implements SamlLoginPostProcessor {

	@Override
	public void process(TokenUser tokenUser) {
		List<SamlGrantedAuthority> newAuthorities = new ArrayList<>();
		for (Iterator<? extends GrantedAuthority> iterator = tokenUser.getAuthorities().iterator(); iterator.hasNext();) {
			GrantedAuthority grantedAuthority = iterator.next();
			
			if ("ROLE_administrator".equals(grantedAuthority.getAuthority())) {
				newAuthorities.add(new SamlGrantedAuthority(RoleConstants.ADMIN));
				tokenUser.getAttributes().put("admin", true);
			}
		}
		
		
		tokenUser.setAuthorities(newAuthorities);
	}
}
