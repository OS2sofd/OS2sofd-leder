package dk.digitalidentity.security;

import dk.digitalidentity.config.RoleConstants;
import dk.digitalidentity.config.StaticContextAccessor;
import dk.digitalidentity.config.settings.ManagerConfiguration;
import dk.digitalidentity.samlmodule.model.SamlGrantedAuthority;
import dk.digitalidentity.samlmodule.model.SamlLoginPostProcessor;
import dk.digitalidentity.samlmodule.model.TokenUser;
import dk.digitalidentity.service.SOFDManagerService;
import dk.digitalidentity.service.SOFDService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class RolePostProcesser implements SamlLoginPostProcessor {
	public static final String LOGGED_IN_USER_ATTRIBUTE = "loggedInUser";
	public static final String CURRENT_CONTEXT_ATTRIBUTE = "currentContext";

	@Autowired
	private SOFDManagerService sofdManagerService;

	@Autowired
	private ManagerConfiguration managerConfiguration;

	public record LoggedInUser(String personUuid, String userId, String name, String email, List<ManagerUIContext> contexts) {}
	public record ManagerUIContext(int id, String uuid, String name, String role, List<String> constraintOUs) {}
	
	@Override
	public void process(TokenUser tokenUser) {
		var contextId = 0;
		// add contexts from OS2sofd (managers and substitutes)
		var kombitUuid = convertSerialToUuid(tokenUser.getUsername());
		log.debug("KombitUuid: " + kombitUuid);
		var personUuid = StaticContextAccessor.getBean(SOFDService.class).ConvertKombitUuidToPersonUuid(kombitUuid);
		log.debug("PersonUuid: " + personUuid);
		var userId = StaticContextAccessor.getBean(SOFDService.class).ConvertKombitUuidToUserId(kombitUuid);
		log.debug("UserId: " + userId);
		var sofdLoginContexts = sofdManagerService.getLoginContexts(personUuid);

		if( sofdLoginContexts == null ) {
			log.error("No user found in OS2sofd");
			return;
		}

		var loggedInUser = new LoggedInUser(personUuid, userId, sofdLoginContexts.name(),sofdLoginContexts.email(), new ArrayList<>());

		// When disableAutoSofdContexts is enabled, the automatic leder/stedfortræder contexts
		// fetched from SOFD Core are suppressed; access is then granted only via SAML role claims.
		if (!managerConfiguration.isDisableAutoSofdContexts()) {
			for( var context : sofdLoginContexts.contexts() ) {
	            switch (context.role()) {
	                case MANAGER -> {
						var constraintOUs = StringUtils.hasText(context.ouUuids()) ? Arrays.asList(context.ouUuids().split(",")) : null;
						loggedInUser.contexts.add(new ManagerUIContext(++contextId, context.uuid(),"Leder " + context.name(), RoleConstants.MANAGER,constraintOUs));
	                }
	                case SUBSTITUTE -> {
						var constraintOUs = StringUtils.hasText(context.ouUuids()) ? Arrays.asList(context.ouUuids().split(",")) : null;
						loggedInUser.contexts.add(new ManagerUIContext(++contextId, context.uuid(),"Stedfortræder for " + context.name(), RoleConstants.SUBSTITUTE, constraintOUs));
	                }
	            }
			}
		}

		// add contexts from the SAML token
		var managerOus = sofdManagerService.getOrgUnitManagers();
		var rootOUList = managerOus.stream().filter(o -> o.parentOrgUnitUuid() == null).map(o -> o.orgUnitUuid()).toList();
        for (SamlGrantedAuthority grantedAuthority : tokenUser.getAuthorities()) {
			switch	(grantedAuthority.getAuthority()) {
				case "ROLE_administrator" -> {
					loggedInUser.contexts.add(new ManagerUIContext(++contextId, loggedInUser.personUuid, "Administrator", RoleConstants.ADMIN, rootOUList));
				}
				case "ROLE_manager_editor" -> {
					List<String> constraintOus = null;
					if (grantedAuthority.getConstraints() != null) {
						var editorConstraint = grantedAuthority.getConstraints().stream().filter(c -> c.getConstraintType().equalsIgnoreCase("http://digital-identity.dk/constraints/orgunit/1")).findFirst().orElse(null);
						if (editorConstraint != null) {
							constraintOus = getAllChildren(managerOus, Arrays.stream(editorConstraint.getConstraintValue().split(",")).toList());

						}
					}
					loggedInUser.contexts.add(new ManagerUIContext(++contextId, loggedInUser.personUuid, "Lederopmærker", RoleConstants.MANAGER_EDITOR, constraintOus));
				}
				case "ROLE_leder" -> {
					loggedInUser.contexts.add(new ManagerUIContext(++contextId, loggedInUser.personUuid, "Leder", RoleConstants.MANAGER, parseOrgUnitConstraint(grantedAuthority, managerOus)));
				}
				case "ROLE_stedfortraeder" -> {
					loggedInUser.contexts.add(new ManagerUIContext(++contextId, loggedInUser.personUuid, "Stedfortræder", RoleConstants.SUBSTITUTE, parseOrgUnitConstraint(grantedAuthority, managerOus)));
				}
				case "ROLE_pause_editor" -> {
					loggedInUser.contexts.add(new ManagerUIContext(++contextId, loggedInUser.personUuid, "Pauseopmærker", RoleConstants.PAUSE_EDITOR, rootOUList));
				}
				case "ROLE_read" -> {
					loggedInUser.contexts.add(new ManagerUIContext(++contextId, loggedInUser.personUuid, "Læseadgang", RoleConstants.READER, rootOUList));
				}
			}
        }

		if( loggedInUser.contexts.isEmpty() ) {
			log.warn("No contexts found for user");
		}

		tokenUser.getAttributes().put(LOGGED_IN_USER_ATTRIBUTE, loggedInUser);
	}

	private List<String> parseOrgUnitConstraint(SamlGrantedAuthority grantedAuthority, List<SOFDManagerService.OrgUnitManagerDto> managerOus) {
		if (grantedAuthority.getConstraints() == null) {
			return null;
		}
		var constraint = grantedAuthority.getConstraints().stream()
				.filter(c -> c.getConstraintType().equalsIgnoreCase("http://digital-identity.dk/constraints/orgunit/1"))
				.findFirst()
				.orElse(null);
		if (constraint == null) {
			return null;
		}
		return getAllChildren(managerOus, Arrays.stream(constraint.getConstraintValue().split(",")).toList());
	}

	private List<String> getAllChildren(List<SOFDManagerService.OrgUnitManagerDto> managerOus, List<String> orgUnitUuids) {
        var result = new ArrayList<String>(orgUnitUuids);
		if (!orgUnitUuids.isEmpty()) {
			result.addAll( getAllChildren(managerOus, managerOus.stream().filter(o -> orgUnitUuids.contains(o.parentOrgUnitUuid())).map(SOFDManagerService.OrgUnitManagerDto::orgUnitUuid).toList()) );
		}
		return result;
	}

	private static String convertSerialToUuid(String serial) {
		if (serial.length() != 36) {
			String[] split = serial.split(",");
			serial = split[split.length - 1].replace("Serial=", "");
		}
		return serial;
	}
}
