package dk.digitalidentity.service;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import dk.digitalidentity.config.DbConfiguration;
import dk.digitalidentity.dao.FeatureSettingDao;
import dk.digitalidentity.dao.model.FeatureSetting;

/**
 * Discovers all {@link DbConfiguration}-annotated fields across {@link ConfigurationProperties}
 * beans, seeds the {@code feature_settings} table with their property-bound values on first boot,
 * and provides DB override lookup for the {@link dk.digitalidentity.security.DbConfigurationAspect}
 * (which intercepts the corresponding getters).
 *
 * Storage key for each field is derived as {@code <bean's @ConfigurationProperties prefix>.<fieldName>},
 * so existing property paths and existing DB rows continue to work without translation.
 */
@Service
@Slf4j
public class FeatureSettingsService {

	public enum FieldType { BOOLEAN, INTEGER, STRING }

	/** Public read-only view of a discovered settings field, used by the admin UI. */
	public static final class FlagDescriptor {
		@Getter private final String key;
		@Getter private final String group;
		@Getter private final String label;
		@Getter private final FieldType type;
		private final Object targetBean;
		private final Field field;

		FlagDescriptor(String key, String group, String label, FieldType type, Object targetBean, Field field) {
			this.key = key;
			this.group = group;
			this.label = label;
			this.type = type;
			this.targetBean = targetBean;
			this.field = field;
		}
	}

	@Autowired
	private FeatureSettingDao dao;

	@Autowired
	private FeatureSettingsService self;

	@Autowired
	private ApplicationContext applicationContext;

	private final Map<String, FlagDescriptor> byKey = new LinkedHashMap<>();
	// HashMap (not IdentityHashMap) because Field.equals compares declaring class + name + type,
	// and Class.getDeclaredField returns a fresh copy each call - they're .equals() but never ==.
	private final Map<Field, FlagDescriptor> byField = new java.util.HashMap<>();

	private volatile boolean ready = false;

	public boolean isReady() {
		return ready;
	}

	@PostConstruct
	@Transactional
	public void init() {
		discover();
		seed();
		ready = true;
	}

	private void discover() {
		Map<String, Object> beans = applicationContext.getBeansWithAnnotation(ConfigurationProperties.class);
		for (Object bean : beans.values()) {
			Class<?> beanClass = AopUtils.getTargetClass(bean);
			ConfigurationProperties cp = beanClass.getAnnotation(ConfigurationProperties.class);
			if (cp == null) {
				continue;
			}
			String prefix = !cp.prefix().isEmpty() ? cp.prefix() : cp.value();
			if (prefix.isEmpty()) {
				continue;
			}
			Object target = unwrap(bean);
			for (Field field : beanClass.getDeclaredFields()) {
				DbConfiguration ann = field.getAnnotation(DbConfiguration.class);
				if (ann == null) {
					continue;
				}
				FieldType type = inferType(field.getType(), field);
				field.setAccessible(true);
				// keys mirror Spring's canonical kebab-case property names so they line up with the
				// property paths customers see in their docker-compose / application.properties.
				String key = prefix + "." + camelToKebab(field.getName());
				FlagDescriptor desc = new FlagDescriptor(key, ann.group(), ann.label(), type, target, field);
				byKey.put(key, desc);
				byField.put(field, desc);
			}
		}
		log.info("Discovered " + byKey.size() + " @DbConfiguration settings");
	}

	private void seed() {
		for (FlagDescriptor desc : byKey.values()) {
			if (dao.findByKey(desc.key) != null) {
				continue;
			}
			FeatureSetting row = new FeatureSetting();
			row.setKey(desc.key);
			row.setValue(normalize(desc.type, stringify(readField(desc))));
			dao.save(row);
		}
	}

	private Object readField(FlagDescriptor desc) {
		try {
			return desc.field.get(desc.targetBean);
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException("Failed to read field " + desc.field, e);
		}
	}

	private static FieldType inferType(Class<?> fieldType, Field field) {
		if (fieldType == boolean.class || fieldType == Boolean.class) {
			return FieldType.BOOLEAN;
		}
		if (fieldType == int.class || fieldType == Integer.class) {
			return FieldType.INTEGER;
		}
		if (fieldType == String.class) {
			return FieldType.STRING;
		}
		throw new IllegalStateException("@DbConfiguration on unsupported type " + fieldType + " (" + field + ")");
	}

	private static String camelToKebab(String camel) {
		return camel.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase();
	}

	private static Object unwrap(Object bean) {
		if (bean instanceof Advised advised) {
			try {
				Object target = advised.getTargetSource().getTarget();
				return target != null ? target : bean;
			}
			catch (Exception e) {
				return bean;
			}
		}
		return bean;
	}

	// ----- override lookup (cached) -----

	@Cacheable(value = "featureSettings", unless = "#result == null")
	public String findRaw(String key) {
		FeatureSetting row = dao.findByKey(key);
		return row == null ? null : row.getValue();
	}

	@CacheEvict(value = "featureSettings", key = "#key")
	public void setRaw(String key, String value) {
		FeatureSetting row = dao.findByKey(key);
		if (row == null) {
			row = new FeatureSetting();
			row.setKey(key);
		}
		row.setValue(value);
		dao.save(row);
	}

	/**
	 * Resolve an override for a known {@link DbConfiguration} field; if absent, evaluate fallback.
	 * Called from the AOP aspect intercepting the corresponding getter.
	 */
	public Object resolveOverride(Field field, Supplier<Object> fallback) {
		FlagDescriptor desc = byField.get(field);
		if (desc == null) {
			return fallback.get();
		}
		String raw = self.findRaw(desc.key);
		if (raw == null) {
			return fallback.get();
		}
		return parse(raw, desc.type, fallback);
	}

	private Object parse(String raw, FieldType type, Supplier<Object> fallback) {
		switch (type) {
			case BOOLEAN: return Boolean.parseBoolean(raw);
			case INTEGER:
				if (!StringUtils.hasText(raw)) return null;
				try { return Integer.parseInt(raw.trim()); }
				catch (NumberFormatException e) { return fallback.get(); }
			case STRING: return raw;
			default: return fallback.get();
		}
	}

	// ----- admin UI API -----

	/** Returns flags grouped by {@link FlagDescriptor#getGroup()} preserving declaration order. */
	public Map<String, List<FlagDescriptor>> grouped() {
		return byKey.values().stream().collect(Collectors.groupingBy(
				FlagDescriptor::getGroup,
				LinkedHashMap::new,
				Collectors.toList()));
	}

	public Collection<FlagDescriptor> all() {
		return byKey.values();
	}

	public Object currentValue(FlagDescriptor desc) {
		String raw = self.findRaw(desc.key);
		if (raw == null) {
			return readField(desc);
		}
		return parse(raw, desc.type, () -> readField(desc));
	}

	/** Persist a value supplied as a form parameter (untyped String) for the given key. */
	public void setValue(String key, String rawValue) {
		FlagDescriptor desc = byKey.get(key);
		if (desc == null) {
			return;
		}
		self.setRaw(key, normalize(desc.type, rawValue));
	}

	private String normalize(FieldType type, String rawValue) {
		if (rawValue == null) {
			rawValue = "";
		}
		switch (type) {
			case BOOLEAN:
				return Boolean.toString(Boolean.parseBoolean(rawValue));
			case INTEGER:
				if (!StringUtils.hasText(rawValue)) {
					return "";
				}
				try { return Integer.toString(Integer.parseInt(rawValue.trim())); }
				catch (NumberFormatException e) { return ""; }
			case STRING:
			default:
				return rawValue.trim();
		}
	}

	private static String stringify(Object value) {
		return value == null ? "" : value.toString();
	}
}
