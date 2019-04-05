package io.opensaber.registry.model;

import java.util.ArrayList;
import java.util.List;

public class IndexFields {

	private String definitionName;
	private List<String> indexFields = new ArrayList<>();
	private List<String> uniqueIndexFields = new ArrayList<>();
	private List<String> newSingleIndexFields = new ArrayList<>();
	private List<String> newCompositeIndexFields = new ArrayList<>();
	private List<String> newUniqueIndexFields = new ArrayList<>();

	public String getDefinitionName() {
		return definitionName;
	}

	public void setDefinitionName(String definitionName) {
		this.definitionName = definitionName;
	}

	public List<String> getIndexFields() {
		return indexFields;
	}

	public void setIndexFields(List<String> indexFields) {
		this.indexFields = indexFields;
	}

	public List<String> getUniqueIndexFields() {
		return uniqueIndexFields;
	}

	public void setUniqueIndexFields(List<String> uniqueIndexFields) {
		this.uniqueIndexFields = uniqueIndexFields;
	}

	public List<String> getNewSingleIndexFields() {
		return newSingleIndexFields;
	}

	public void setNewSingleIndexFields(List<String> newSingleIndexFields) {
		this.newSingleIndexFields = newSingleIndexFields;
	}

	public List<String> getNewCompositeIndexFields() {
		return newCompositeIndexFields;
	}

	public void setNewCompositeIndexFields(List<String> newCompositeIndexFields) {
		this.newCompositeIndexFields = newCompositeIndexFields;
	}

	public List<String> getNewUniqueIndexFields() {
		return newUniqueIndexFields;
	}

	public void setNewUniqueIndexFields(List<String> newUniqueIndexFields) {
		this.newUniqueIndexFields = newUniqueIndexFields;
	}

}
