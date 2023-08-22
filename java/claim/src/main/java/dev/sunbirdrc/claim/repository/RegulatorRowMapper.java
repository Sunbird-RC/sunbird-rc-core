package dev.sunbirdrc.claim.repository;

import dev.sunbirdrc.claim.entity.Regulator;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class RegulatorRowMapper implements RowMapper<Regulator> {
    @Override
    public Regulator mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Regulator(
                rs.getString("ID"),
                rs.getString("name"),
                rs.getString("phoneNumber"),
                rs.getString("council"),
                rs.getString("email"),
                rs.getString("osOwner"),
                rs.getString("osid")

        );
    }
}
