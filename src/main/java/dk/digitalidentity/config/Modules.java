package dk.digitalidentity.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Modules {
	private SubstituteModule substituteModule = new SubstituteModule();
	private FunctionHierarchy functionHierarchy = new FunctionHierarchy();
	private AffiliationModule affiliationModule = new AffiliationModule();
	private PausePersonModule pausePersonModule = new PausePersonModule();
}
