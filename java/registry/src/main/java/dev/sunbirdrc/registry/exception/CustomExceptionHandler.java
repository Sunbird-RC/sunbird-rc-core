package dev.sunbirdrc.registry.exception;

import com.google.gson.Gson;
import dev.sunbirdrc.registry.interceptor.handler.BaseResponseHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

public class CustomExceptionHandler extends BaseResponseHandler implements HandlerExceptionResolver {

    private static Logger logger = LoggerFactory.getLogger(CustomExceptionHandler.class);

    private Gson gson;

    public CustomExceptionHandler(Gson gson) {
        this.gson = gson;
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler,
                                         Exception ex) {
        try {
            logger.error("Exception thrown: {}", ExceptionUtils.getStackTrace(ex));
            setResponse(response);
            writeResponseObj(gson, ex.getMessage());
        } catch (Exception e) {
            logger.error("Error in sending response");
        }
        return null;
    }

}
