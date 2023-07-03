package org.sunbirdrc.id.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * <h1>IdRequest</h1>
 * 
 * @author Narendra
 *
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class IdRequest {

	@Size(max = 200)
	@JsonProperty("idName")
	@NotNull
	private String idName;

	@NotNull
	@Size(max=200)
	@JsonProperty("tenantId")
	private String tenantId;

	@Size(max = 200)
	@JsonProperty("format")
	private String format;

	@JsonProperty("count")
	private Integer count;

}
