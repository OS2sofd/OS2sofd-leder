package dk.digitalidentity.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Modules {
	private SubstituteModule substituteModule;
	private FunctionHierarchy functionHierarchy;
}
