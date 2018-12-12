package io.opensaber.registry.dao;

import java.util.List;
import java.util.NoSuchElementException;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import io.opensaber.registry.exception.AuditFailedException;
import io.opensaber.registry.exception.DuplicateRecordException;
import io.opensaber.registry.exception.EncryptionException;
import io.opensaber.registry.exception.RecordNotFoundException;
import io.opensaber.registry.sink.DatabaseProvider;

public interface RegistryDao {

	public List getEntityList();

	public String addEntity(Graph entity, String label, String rootNodeLabel, String property)
			throws DuplicateRecordException, RecordNotFoundException, NoSuchElementException, EncryptionException,
			AuditFailedException;

	public boolean updateEntity(Graph entityForUpdate, String rootNodeLabel, String methodOrigin)
			throws RecordNotFoundException, NoSuchElementException, EncryptionException, AuditFailedException;

	public Graph getEntityById(String label, boolean includeSignatures)
			throws RecordNotFoundException, NoSuchElementException, EncryptionException, AuditFailedException;

	public Graph getEntityByVertex(Vertex vertex)
			throws RecordNotFoundException, NoSuchElementException, EncryptionException, AuditFailedException;

	public boolean deleteEntityById(String id) throws RecordNotFoundException, AuditFailedException;

	public String getRootLabelForNodeLabel(String nodeLabel);

    public String getTypeForNodeLabel(String label);
    
	public void setDatabaseProvider(DatabaseProvider databaseProvider);

}
