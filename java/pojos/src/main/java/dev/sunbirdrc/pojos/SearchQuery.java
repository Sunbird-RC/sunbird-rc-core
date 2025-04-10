package dev.sunbirdrc.pojos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SearchQuery {

	@Setter
    private List<Filter> filters;
	private int limit;
	@Setter
    private int offset;
	@Setter
    private List<String> fields;
	@Setter
    private String rootLabel;
	private List<String> entityTypes;

	public SearchQuery(String rootLabel, int offset, int limit) {
		this.rootLabel = rootLabel;
		this.offset = offset;
		this.limit = limit;
	}
	
   public SearchQuery(List<String> entityTypes, int offset, int limit) {
        this.entityTypes = entityTypes;
        this.offset = offset;
        this.limit = limit;
    }

    //limit value must not proceed the default(max) limit value
	public void setLimit(int limit) {
	    if(limit <= this.limit){
	        this.limit = limit; 
	    }
	}

}
