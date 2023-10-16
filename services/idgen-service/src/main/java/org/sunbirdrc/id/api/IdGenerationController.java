package org.sunbirdrc.id.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.sunbirdrc.id.model.IdGenerationRequest;
import org.sunbirdrc.id.model.IdGenerationResponse;
import org.sunbirdrc.id.service.IdGenerationService;

import javax.validation.Valid;

/**
 * api's related to the IdGeneration Controller
 * 
 * @author Pavan Kumar Kamma
 */
@RestController
@RequestMapping(path = "/id/")
public class IdGenerationController {

	@Autowired
	IdGenerationService idGenerationService;

	/**
	 * description: generate unique ID for property
	 * 
	 * @param IdGenerationRequest
	 * @return IdGenerationResponse
	 * @throws Exception
	 */
	@RequestMapping(method = RequestMethod.POST, path = "_generate")
	public IdGenerationResponse generateIdResponse(
			@RequestBody @Valid IdGenerationRequest idGenerationRequest)
			throws Exception {

		IdGenerationResponse idGenerationResponse = idGenerationService
				.generateIdResponse(idGenerationRequest);

		return idGenerationResponse;
	}

}
