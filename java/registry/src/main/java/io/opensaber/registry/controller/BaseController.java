package io.opensaber.registry.controller;

import io.opensaber.registry.exception.CustomException;
import io.opensaber.registry.middleware.util.Constants;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BaseController implements ErrorController {

	private static final String PATH = "/error";

	@RequestMapping(value = PATH)
	public String error() throws Exception {
		throw new CustomException(Constants.CUSTOM_EXCEPTION_ERROR);
	}

	@Override
	public String getErrorPath() {
		return PATH;
	}

}
