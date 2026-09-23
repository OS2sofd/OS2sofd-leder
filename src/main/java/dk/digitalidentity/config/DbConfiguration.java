package dk.digitalidentity.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a configuration field whose value can be overridden in the {@code feature_settings} table
 * and edited from the admin UI at {@code /admin/features}.
 *
 * The bean must carry {@link org.springframework.boot.context.properties.ConfigurationProperties}
 * (or be a nested type whose owner does), since the property prefix is used to derive the storage
 * key as {@code <prefix>.<fieldName>}.
 *
 * Type is inferred from the field type:
 *   boolean / Boolean → BOOLEAN
 *   int / Integer     → INTEGER
 *   String            → STRING
 *
 * No custom getter is needed; the {@link dk.digitalidentity.security.DbConfigurationAspect}
 * intercepts the bean's standard getter and consults the DB when an override exists.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DbConfiguration {

	/** Section label in the admin UI (e.g. "Generelle indstillinger", "Visning", "Tilhørsforhold"). */
	String group();

	/** Human-readable label shown next to the form field in the admin UI. */
	String label();
}
