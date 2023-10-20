package org.egov.id.masterdata;

import org.egov.id.model.IdRequest;
import org.egov.id.model.RequestInfo;
import org.egov.mdms.model.MasterDetail;
import org.egov.mdms.model.MdmsResponse;

import java.util.List;
import java.util.Map;

public interface MasterDataProvider {
    String getCity(RequestInfo requestInfo, IdRequest idRequest);
    String getIdFormat(RequestInfo requestInfo, IdRequest idRequest);
}
