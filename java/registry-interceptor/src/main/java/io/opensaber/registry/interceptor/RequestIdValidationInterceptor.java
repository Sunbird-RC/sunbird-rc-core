package io.opensaber.registry.interceptor;

import com.google.gson.Gson;
import io.opensaber.registry.interceptor.handler.BaseRequestHandler;
import io.opensaber.registry.middleware.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;


public class RequestIdValidationInterceptor implements HandlerInterceptor {

    private Gson gson;
    private Map<String,String> requestIdMap;

    private static Logger logger = LoggerFactory.getLogger(RequestIdValidationInterceptor.class);

    public RequestIdValidationInterceptor(Map requestIdMap, Gson gson) {
        this.gson = gson;
        this.requestIdMap = requestIdMap;
    }

    /**
     * This method checks for each request it contains a valid request id for accessing the api
     * @param request
     * @param response
     * @param handler
     * @return true or false
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        BaseRequestHandler baseRequestHandler = new BaseRequestHandler();
        try{
            //Code commented for later purpose, need to work on reading body from request
            /*baseRequestHandler.setRequest(request);
            Request req = (Request) baseRequestHandler.getRequestBodyMap().get(Constants.REQUEST_ATTRIBUTE);*/
            if(requestIdMap.containsKey(request.getRequestURI()) /*&& requestIdMap.get(request.getRequestURI()).equalsIgnoreCase(req.getId())*/){
                return true;
            } else {
                throw new Exception();
            }

        }catch(Exception e){
            logger.error(" Authentication Failed !", e);
            baseRequestHandler.setResponse(response);
            baseRequestHandler.writeResponseObj(gson, Constants.ENTITY_ID_MISMATCH);
            response = baseRequestHandler.getResponse();
        }
		return false;
    }

}
