/**
 * 
 */
package io.opensaber.registry.interceptor;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import io.opensaber.registry.interceptor.handler.BaseRequestHandler;
import io.opensaber.registry.interceptor.handler.RequestWrapper;
import io.opensaber.registry.middleware.impl.RDFConverter;
import io.opensaber.registry.middleware.impl.RdfToJsonldConverter;
import io.opensaber.registry.middleware.util.Constants;

/**
 * @author jyotsna
 *
 */
@Order(1)
@Component
public class RDFConversionInterceptor extends BaseRequestHandler implements HandlerInterceptor {


	private RDFConverter rdfConverter;

	@Autowired
	public RDFConversionInterceptor(RDFConverter rdfConverter){
		this.rdfConverter = rdfConverter;
	}


	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		setRequest(request);
		Map<String,Object> attributeMap = rdfConverter.execute(getRequestBodyMap());
		setRequestAttributes(attributeMap);
		request = getRequest();
		if(request.getAttribute(Constants.RDF_OBJECT)!=null){
			return true;
		}
		return false;
	}


	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		// TODO Auto-generated method stub

	}


	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		// TODO Auto-generated method stub

	}

}
