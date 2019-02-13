package io.opensaber.registry.config.validation;

import io.opensaber.registry.model.DBConnectionInfo;
import io.opensaber.registry.model.DBConnectionInfoMgr;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

@Component
public class DatabaseConfigValidator implements ConstraintValidator<ValidDatabaseConfig, DBConnectionInfoMgr> {

    @Override
    public boolean isValid(DBConnectionInfoMgr mgr, ConstraintValidatorContext context) {

        boolean isValidFlag = false;
        String message = null;
        
        if (mgr.getProvider().isEmpty() || mgr.getUuidPropertyName().isEmpty()) {
            message = "database.provider or database.uuidPropertyName is empty";
        }
        if (mgr.getConnectionInfo().size() < 1) {
            message = "At least one connectionInfo must be specified";
        }
        
        boolean nShardsExist = mgr.getConnectionInfo().size() > 1;
        for (DBConnectionInfo info : mgr.getConnectionInfo()) {
            if (info.getShardId().isEmpty() || info.getUri().isEmpty()) {
                message = "database.connectionInfo.shardId or database.connectionInfo.uri is empty";
                break;
            }
            if (nShardsExist && info.getShardLabel().isEmpty()) {
                message = "database.connectionInfo.shardLabel is empty";
                break;
            }
            if (nShardsExist && !isUniqueShardId(mgr.getConnectionInfo(), info.getShardId())) {
                message = "database.connectionInfo.shardId must be unique";
                break;
            }
            if (nShardsExist
                    && !isUniqueShardLabel(mgr.getConnectionInfo(), info.getShardLabel())) {
                message = "database.connectionInfo.shardLabel must be unique";
                break;
            }
        }

        if (message != null)
            setMessage(context, message);
        else
            isValidFlag = true;

        return isValidFlag;
    }

    private void setMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }

    private boolean isUniqueShardId(final List<DBConnectionInfo> dbConnectionInfoList, final String shardId) {
        boolean shardIdUnique = dbConnectionInfoList.stream().filter(o -> o.getShardId().equals(shardId)).count() == 1;
        return shardIdUnique;
    }

    private boolean isUniqueShardLabel(final List<DBConnectionInfo> dbConnectionInfoList, String shardLabel) {
        boolean shardLabelUnique = dbConnectionInfoList.stream().filter(o -> o.getShardLabel().equals(shardLabel))
                .count() == 1;
        return shardLabelUnique;
    }
}
