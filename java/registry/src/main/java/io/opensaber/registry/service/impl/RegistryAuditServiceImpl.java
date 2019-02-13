package io.opensaber.registry.service.impl;

import io.opensaber.registry.dao.IRegistryDao;
import io.opensaber.registry.service.RegistryAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RegistryAuditServiceImpl implements RegistryAuditService {

	private static Logger logger = LoggerFactory.getLogger(RegistryServiceImpl.class);

	// TODO - how audit happens and where it is written to needs some thought
	private IRegistryDao registryDao;

	@Value("${audit.frame.file}")
	private String auditFrameFile;

}
