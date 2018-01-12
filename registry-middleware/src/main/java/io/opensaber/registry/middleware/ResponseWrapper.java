package io.opensaber.registry.middleware;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * 
 * @author jyotsna
 *
 */
public class ResponseWrapper extends HttpServletResponseWrapper{
 
    public ResponseWrapper(HttpServletResponse response) {
        super(response);
    }
 
    /**
	 * Method to write content/body into the response
	 *
	 * @return
	 */
	public HttpServletResponseWrapper writeResponseBody(HttpServletResponseWrapper responseWrapper,String content) {
		BufferedWriter bufferedWriter = null;
		try {
			OutputStream outputStream = responseWrapper.getOutputStream();

			if (outputStream != null) {
				bufferedWriter = new BufferedWriter(new OutputStreamWriter(
						outputStream));
				bufferedWriter.write(content);

			} 
		} catch (IOException ex) {
		} finally {
			if (bufferedWriter != null) {
				try {
					bufferedWriter.close();
				} catch (IOException ex) {
				}
			}
		}
		return responseWrapper;
	}
	
}
