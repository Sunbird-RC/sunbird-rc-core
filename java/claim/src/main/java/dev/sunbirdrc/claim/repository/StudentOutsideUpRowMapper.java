package dev.sunbirdrc.claim.repository;

import dev.sunbirdrc.claim.entity.StudentForeignVerification;
import dev.sunbirdrc.claim.entity.StudentOutsideUP;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class StudentOutsideUpRowMapper implements RowMapper<StudentOutsideUP> {
    @Override
    public StudentOutsideUP mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new StudentOutsideUP(
                rs.getString("aadhaarNo"),
                rs.getString("barCode"),
                rs.getString("candidatePic"),
                rs.getString("council"),
                rs.getString("courseName"),
                rs.getString("courseState"),
                rs.getString("date"),
                rs.getString("dateOfBirth"),
                rs.getString("email"),
                rs.getString("examBody"),
                rs.getString("fathersName"),
                rs.getString("feeReciptNo"),
                rs.getString("finalYearRollNo"),
                rs.getString("gender"),
                rs.getString("joiningMonth"),
                rs.getString("joiningYear"),
                rs.getString("mothersName"),
                rs.getString("name"),
                rs.getString("nurseRegNo"),
                rs.getString("nursingCollage"),
                rs.getString("osid"),
                rs.getString("osOwner"),
                rs.getString("passingMonth"),
                rs.getString("passingYear"),
                rs.getString("phoneNumber"),
                rs.getString("registrationNo"),
                rs.getString("registrationType"),
                rs.getString("title")
        );
    }
}
