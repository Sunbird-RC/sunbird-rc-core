package dev.sunbirdrc.claim.service;

import dev.sunbirdrc.claim.config.PropertyMapper;
import dev.sunbirdrc.claim.entity.Regulator;
import dev.sunbirdrc.claim.repository.RegulatorRowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegulatorService {

    @Autowired
    private PropertyMapper propertyMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Regulator> findByCouncil(String council) {
        try {
            return jdbcTemplate.query("SELECT * FROM \"" + propertyMapper.getRegulatorTableName()
                    + "\" where council=?", new RegulatorRowMapper(), council);

        } catch (IncorrectResultSizeDataAccessException e) {
            return null;
        }
    }

    public List<Regulator> findAll() {
        return jdbcTemplate.query("SELECT * from \"" + propertyMapper.getRegulatorTableName() + "\"",
                new RegulatorRowMapper());
    }

    public boolean isRegulatorTableExist() {
        String sqlQuery = "SELECT count(*) FROM information_schema.tables WHERE table_name = '"
                + propertyMapper.getRegulatorTableName() + "'";

        Integer tableCount = jdbcTemplate.queryForObject(sqlQuery, Integer.class);

        return tableCount > 0;
    }
}
