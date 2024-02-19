package org.egov.id.masterdata.provider;

import lombok.extern.slf4j.Slf4j;
import org.egov.id.masterdata.MasterDataProvider;
import org.egov.id.model.IdRequest;
import org.egov.id.model.RequestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.List;

@Service
@Slf4j
public class DBMasterDataProvider implements MasterDataProvider {
    @Autowired
    DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public String getCity(RequestInfo requestInfo, IdRequest idRequest) {
        String city = null;
        try {
            String tenantId = idRequest.getTenantId();
            StringBuffer idSelectQuery = new StringBuffer();
            idSelectQuery.append("SELECT citycode FROM tenantid_citycode ").append("WHERE tenantid=?");
            city = jdbcTemplate.queryForObject(idSelectQuery.toString(),new Object[]{tenantId}, String.class);
        } catch (Exception ex) {
            log.error("SQL error while trying to retrieve format from DB", ex);
        }
        return city;
    }

    @Override
    public String getIdFormat(RequestInfo requestInfo, IdRequest idRequest) {
        String idFormat = null;
        try {
            String idName = idRequest.getIdName();
            String tenantId = idRequest.getTenantId();
            // select the id format from the id generation table
            StringBuffer idSelectQuery = new StringBuffer();
            idSelectQuery.append("SELECT format FROM id_generator ").append(" WHERE idname=? and tenantid=?");

            String rs = jdbcTemplate.queryForObject(idSelectQuery.toString(),new Object[]{idName,tenantId}, String.class);
            if (!StringUtils.isEmpty(rs)) {
                idFormat = rs;
            } else {
                // querying for the id format with idname
                StringBuffer idNameQuery = new StringBuffer();
                idNameQuery.append("SELECT format FROM id_generator ").append(" WHERE idname=?");
                rs = jdbcTemplate.queryForObject(idSelectQuery.toString(),new Object[]{idName}, String.class);
                if (!StringUtils.isEmpty(rs))
                    idFormat = rs;
            }
        } catch (Exception ex){
            log.error("SQL error while trying to retrieve format from DB", ex);
        }
        return idFormat;
    }

    public void createIdFormat(IdRequest idRequest) {
        try {
            String idName = idRequest.getIdName();
            String tenantId = idRequest.getTenantId();
            String idFormat = idRequest.getFormat();
            StringBuffer idSelectQuery = new StringBuffer();
            idSelectQuery.append("INSERT INTO id_generator(idname, tenantid, format, sequencenumber) ").append("VALUES (?, ?, ?, ?) ");
            int rs = jdbcTemplate.update(idSelectQuery.toString(),idName,tenantId,idFormat,1);
            if(rs != 1) throw new RuntimeException("Unable to insert the id format");
        } catch (Exception ex){
            log.error("SQL error while trying to retrieve format from DB", ex);
            throw ex;
        }
    }
}
