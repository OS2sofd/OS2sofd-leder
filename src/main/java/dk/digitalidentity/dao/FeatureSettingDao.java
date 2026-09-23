package dk.digitalidentity.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.dao.model.FeatureSetting;

public interface FeatureSettingDao extends JpaRepository<FeatureSetting, Long> {

	FeatureSetting findByKey(String key);
}
