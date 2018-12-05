package io.opensaber.registry.frame;

import java.io.IOException;

import io.opensaber.registry.exception.EntityCreationException;
import io.opensaber.registry.exception.MultipleEntityException;

public interface FrameEntity {

	public String getContent(org.eclipse.rdf4j.model.Model entityModel)
			throws IOException, MultipleEntityException, EntityCreationException;

	public String getContent();

}
