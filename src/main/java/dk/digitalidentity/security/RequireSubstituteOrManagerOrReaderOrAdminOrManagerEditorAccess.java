package dk.digitalidentity.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ROLE_MANAGER') or hasRole('ROLE_SUBSTITUTE') or hasRole('ROLE_READER') or hasRole('ROLE_ADMIN') or hasRole('ROLE_MANAGER_EDITOR')")
public @interface RequireSubstituteOrManagerOrReaderOrAdminOrManagerEditorAccess {

}
