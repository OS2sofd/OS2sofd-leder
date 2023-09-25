package dk.digitalidentity.config;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FunctionHierarchy {
	private boolean enabled;
	private boolean startDateMandatory = true;
	private boolean stopDateMandatory = true;
	private boolean showAllFunctionsEnabled = false;
}
