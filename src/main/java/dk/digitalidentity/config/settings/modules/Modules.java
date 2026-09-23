package dk.digitalidentity.config.settings.modules;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Getter
public class Modules {
	@Autowired private SubstituteModule substituteModule;
	@Autowired private FunctionHierarchy functionHierarchy;
	@Autowired private AffiliationModule affiliationModule;
	@Autowired private PausePersonModule pausePersonModule;
	@Autowired private AccountApprovalModule accountApprovalModule;
	@Autowired private ManagerModule managerModule;
}
