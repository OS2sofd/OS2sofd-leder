package dk.digitalidentity.config;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dk.digitalidentity.config.DbConfiguration;
import dk.digitalidentity.service.FeatureSettingsService;

/**
 * Intercepts {@code get*} / {@code is*} getters on {@code dk.digitalidentity.config.settings}
 * beans. If the corresponding field is annotated with {@link DbConfiguration}, the value is read
 * from the {@code feature_settings} table via {@link FeatureSettingsService}; otherwise the
 * original Lombok-generated getter runs unchanged.
 *
 * Before {@link FeatureSettingsService#isReady()} flips to true (i.e. during boot, while the
 * service is seeding the table), every call short-circuits to the original getter so the
 * seeding step sees property-bound field values rather than recursing back through the DB.
 */
@Aspect
@Component
public class DbConfigurationAspect {

	@Autowired
	private FeatureSettingsService featureSettings;

	/**
	 * Cached method→field resolution. Map value of {@code Optional.empty()} marks "this getter
	 * does not back a @DbConfiguration field", so we don't repeat reflection for ordinary getters
	 * (which dominate the calls).
	 */
	private final ConcurrentMap<MethodKey, Optional<Field>> methodToField = new ConcurrentHashMap<>();

	@Around("(execution(* dk.digitalidentity.config.settings..*.get*()) "
			+ "|| execution(* dk.digitalidentity.config.settings..*.is*())) "
			+ "&& !execution(* dk.digitalidentity.config.settings..*.getClass())")
	public Object interceptGetter(ProceedingJoinPoint pjp) throws Throwable {
		if (featureSettings == null || !featureSettings.isReady()) {
			return pjp.proceed();
		}
		Class<?> targetClass = AopUtils.getTargetClass(pjp.getTarget());
		String methodName = pjp.getSignature().getName();
		MethodKey cacheKey = new MethodKey(targetClass, methodName);
		Optional<Field> resolved = methodToField.computeIfAbsent(cacheKey, k -> resolveField(k.targetClass, k.methodName));
		if (resolved.isEmpty()) {
			return pjp.proceed();
		}
		Field field = resolved.get();
		return featureSettings.resolveOverride(field, () -> proceedSafely(pjp));
	}

	private static Optional<Field> resolveField(Class<?> targetClass, String methodName) {
		String fieldName;
		if (methodName.startsWith("get") && methodName.length() > 3) {
			fieldName = decapitalize(methodName.substring(3));
		}
		else if (methodName.startsWith("is") && methodName.length() > 2) {
			fieldName = decapitalize(methodName.substring(2));
		}
		else {
			return Optional.empty();
		}
		try {
			Field field = targetClass.getDeclaredField(fieldName);
			if (!field.isAnnotationPresent(DbConfiguration.class)) {
				return Optional.empty();
			}
			field.setAccessible(true);
			return Optional.of(field);
		}
		catch (NoSuchFieldException e) {
			return Optional.empty();
		}
	}

	private static String decapitalize(String s) {
		if (s.isEmpty()) {
			return s;
		}
		return Character.toLowerCase(s.charAt(0)) + s.substring(1);
	}

	private static Object proceedSafely(ProceedingJoinPoint pjp) {
		try {
			return pjp.proceed();
		}
		catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	private record MethodKey(Class<?> targetClass, String methodName) {}
}
