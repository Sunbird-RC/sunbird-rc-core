package dev.sunbirdrc.claim.repository;

import dev.sunbirdrc.claim.entity.StudentForeignVerification;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class StudentForeignVerificationRowMapper implements RowMapper<StudentForeignVerification> {
    @Override
    public StudentForeignVerification mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new StudentForeignVerification(
                rs.getString("title"),
                rs.getString("name"),
                rs.getString("registrationType"),
                rs.getString("registrationNumber"),
                rs.getString("refNo"),
                rs.getString("phoneNumber"),
                rs.getString("passingYear"),
                rs.getString("passingMonth"),
                rs.getString("osOwner"),
                rs.getString("osid"),
                rs.getString("nursingCollage"),
                rs.getString("mothersName"),
                rs.getString("joiningYear"),
                rs.getString("joiningMonth"),
                rs.getString("gender"),
                rs.getString("finalYearRollNo"),
                rs.getString("fathersName"),
                rs.getString("examBody"),
                rs.getString("email"),
                rs.getString("dateOfBirth"),
                rs.getString("date"),
                rs.getString("courseName"),
                rs.getString("council"),
                rs.getString("candidatePic"),
                rs.getString("barCode"),
                rs.getString("aadhaarNo")
        );
    }
}
