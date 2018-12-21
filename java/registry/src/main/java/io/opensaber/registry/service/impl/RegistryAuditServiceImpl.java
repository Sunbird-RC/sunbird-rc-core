package io.opensaber.registry.service.impl;

import io.opensaber.registry.dao.IRegistryDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.opensaber.registry.frame.FrameEntity;
import io.opensaber.registry.service.RegistryAuditService;

@Component
public class RegistryAuditServiceImpl implements RegistryAuditService {

	private static Logger logger = LoggerFactory.getLogger(RegistryServiceImpl.class);

	@Autowired
	private IRegistryDao registryDao;

	@Value("${audit.frame.file}")
	private String auditFrameFile;

	@Autowired
	private FrameEntity frameEntity;
}
