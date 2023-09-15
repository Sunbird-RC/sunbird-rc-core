package dev.sunbirdrc.pojos;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 
 * @author jyotsna
 *
 */
public class ResponseWrapper extends ContentCachingResponseWrapper {

	private static Logger logger = LoggerFactory.getLogger(ResponseWrapper.class);

	public ResponseWrapper(HttpServletResponse response) {
		super(response);
	}

	/**
	 * Method to write content/body into the response
	 *
	 * @return
	 */
	public void writeResponseBody(String content) {
		logger.info("Response content:" + content);
		BufferedWriter bufferedWriter = null;
		try {
			OutputStream outputStream = this.getResponse().getOutputStream();

			if (outputStream != null) {
				bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
				bufferedWriter.write(content);
			}
		} catch (Exception e) {
			logger.error("ERROR IN SENDING RESPONSE: {}", ExceptionUtils.getStackTrace(e));
		} finally {
			if (bufferedWriter != null) {
				try {
					bufferedWriter.close();
				} catch (Exception e) {
					logger.error("ERROR in closing stream: {}", ExceptionUtils.getStackTrace(e));
				}
			}
		}
	}

	/**
	 * Method to read body of the response
	 * 
	 * @return
	 */
	public String getResponseContent() throws IOException {
		// return new String(this.getContentAsByteArray());
		return IOUtils.toString(this.getContentInputStream(), UTF_8);
	}

}
