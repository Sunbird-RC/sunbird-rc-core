package io.opensaber.views;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ViewTransformer {
    /**
     * transforms a given JsonNode to representation of view templates
     * view template indicates any new field or mask fields for transformation
     * 
     * @param viewTemplate
     * @param node
     * @return
     */
	public JsonNode transform(ViewTemplate viewTemplate, JsonNode node) throws Exception {

		ObjectNode result = JsonNodeFactory.instance.objectNode();

		// loops for all entityTypes
		Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
		while (fields.hasNext()) {

			String subjectType = fields.next().getKey();
			JsonNode nodeAttrs = node.get(subjectType);

			JsonNode resultNode = JsonNodeFactory.instance.objectNode();
			if (nodeAttrs.isArray()) {
				ArrayNode resultArray = JsonNodeFactory.instance.arrayNode();

				for (int i = 0; i < nodeAttrs.size(); i++) {
					
					JsonNode tNode = tranformNode(viewTemplate, nodeAttrs.get(i));
					resultArray.add(tNode);
				}
				resultNode = resultArray;

			} else if (nodeAttrs.isObject()) {
				resultNode = tranformNode(viewTemplate, nodeAttrs);
				
			} else {
				throw new IllegalArgumentException("Not a valid node for transformation, must be a object node or array node");
			}

			result.set(subjectType, resultNode);
		}

		return result;
	}

    /**
     * Transforms a single node for given view template
     * 
     * @param viewTemplate
     * @param nodeAttrs
     * @return
     */
    private JsonNode tranformNode(ViewTemplate viewTemplate, JsonNode nodeAttrs) throws Exception {
        ObjectNode result = JsonNodeFactory.instance.objectNode();

        for (Field field : viewTemplate.getFields()) {

            String functionStr = field.getFunction();
            if (functionStr != null) {

                String fdName = field.getFunctioName();
                FunctionDefinition funcDef = viewTemplate.getFunctionDefinition(fdName);            
                                
                List<Object> actualValues = new ArrayList<>();
                for (String oneArg : field.getArgNames()) {
                    // Cut off the $
                	if(nodeAttrs.get(oneArg.substring(1)) != null) {
                        actualValues.add(ValueType.getValue(nodeAttrs.get(oneArg.substring(1))));
                	}
                }
                
                IEvaluator<Object> evaluator = EvaluatorFactory.getInstance(funcDef, actualValues);
                if (field.getDisplay()) {
                    Object evaluatedValue = evaluator.evaluate();
                    if(evaluatedValue instanceof String){
                        result.put(field.getTitle(), evaluatedValue.toString());
                    } else {
                        result.set(field.getTitle(), JsonNodeFactory.instance.pojoNode(evaluatedValue));
                    }
                }
            // if display is set, show up the field in result    
            } else if (field.getDisplay()) {
                result.set(field.getTitle(), nodeAttrs.get(field.getName()));
            }
        }
        
        appendSignatures(viewTemplate, nodeAttrs,result);        
        return result;
    }
    
    /**
     * Appends the signature array to the result as per the fields specified in view template file.
     *
     * @param viewTemplate
     * @param nodeAttrs
     * @param resultNode
     * @return
     * */
    private JsonNode appendSignatures(ViewTemplate viewTemplate, JsonNode nodeAttrs, JsonNode resultNode) throws Exception {
    	if(nodeAttrs.get("signatures")!=null) {
			
			ArrayNode sigArray = new ObjectMapper().createArrayNode();
			
	    	for (JsonNode sigNode : nodeAttrs.get("signatures")) {
	    		JsonNode signatureField = sigNode.get("signatureFor");
	    		
	    		if(signatureField != null){
	    			//Adds the signatures of fields specified in view template file
		    		for (Field fieldTemp : viewTemplate.getFields()) {
		    			if(signatureField.asText().endsWith("/"+fieldTemp.getName()) && fieldTemp.getDisplay()){		
		    				sigArray.add(sigNode);
		    			}
		    		}
		    		//Adds the subject signature
		    		if(signatureField.asText().endsWith("/"+viewTemplate.getSubject())){	    				
	    				sigArray.add(sigNode);
	    			}
	    		}
	    	}
	    	if(sigArray.size()>0) {
	    		((ObjectNode)resultNode).set("signatures", sigArray);
	    	}
		}
    	return resultNode;
    }
}