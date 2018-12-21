package io.opensaber.registry.service.impl;

import io.opensaber.registry.dao.SearchDao;
import io.opensaber.registry.frame.FrameEntity;
import io.opensaber.registry.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SearchServiceImpl implements SearchService {

	@Autowired
	FrameEntity frameEntity;
	@Autowired
	private SearchDao searchDao;

}
