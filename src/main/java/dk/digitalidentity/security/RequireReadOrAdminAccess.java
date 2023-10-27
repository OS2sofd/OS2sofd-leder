package dk.digitalidentity.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ROLE_READER') or hasRole('ROLE_ADMIN')")
public @interface RequireReadOrAdminAccess {

}
