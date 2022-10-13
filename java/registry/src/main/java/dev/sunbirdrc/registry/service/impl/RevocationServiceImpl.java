package dev.sunbirdrc.registry.service.impl;

import dev.sunbirdrc.registry.entities.RevokedCredential;
import dev.sunbirdrc.registry.helper.RegistryHelper;
import dev.sunbirdrc.registry.middleware.util.Constants;
import dev.sunbirdrc.registry.middleware.util.OSSystemFields;
import dev.sunbirdrc.registry.service.RevocationService;
import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RevocationServiceImpl implements RevocationService {

//	private final RegistryHelper registryHelper;
//
//	@Autowired
//	public RevocationServiceImpl(RegistryHelper registryHelper) {
//		this.registryHelper = registryHelper;
//	}

	@Override
	public void storeCredential(String entity, String entityId, String userId, Vertex deletedVertex) {
		if (deletedVertex != null) {
			String signedData = deletedVertex.property(OSSystemFields._osSignedData.name()).isPresent() ? (String) deletedVertex.property(Constants.TYPE_STR_JSON_LD).value() : null;
			if (!StringUtils.isEmpty(signedData)) {
				RevokedCredential revokedCredential = RevokedCredential.builder().entity(entity).entityId(entityId).signedData(signedData).signedHash("" + signedData.hashCode()).build();
				revokedCredential.hashCode();
			}
		}
	}
}
