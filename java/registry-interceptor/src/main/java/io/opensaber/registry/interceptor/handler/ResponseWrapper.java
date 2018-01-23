package io.opensaber.registry.interceptor.handler;


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;


import javax.servlet.http.HttpServletResponse;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * 
 * @author jyotsna
 *
 */
public class ResponseWrapper extends ContentCachingResponseWrapper{
 
    public ResponseWrapper(HttpServletResponse response) {
        super(response);
    }
 
    /**
	 * Method to write content/body into the response
	 *
	 * @return
	 */
	public void writeResponseBody(String content) {
		BufferedWriter bufferedWriter = null;
		try {
			OutputStream outputStream = this.getResponse().getOutputStream();

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
	}
	
	/**
	 * Method to read body of the response
	 * 
	 * @return
	 */
    public String getResponseContent() {
        return new String(this.getContentAsByteArray());
    }

	
}
