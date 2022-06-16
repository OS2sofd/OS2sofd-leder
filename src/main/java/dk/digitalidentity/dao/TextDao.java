package dk.digitalidentity.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.dao.model.Text;

public interface TextDao extends JpaRepository<Text, Long>{

	Text findByKey(String key);
}
