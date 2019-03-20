package io.opensaber.registry.model;

import io.opensaber.pojos.AuditRecord;
import io.opensaber.registry.exception.audit.LabelCannotBeNullException;
import io.opensaber.registry.sink.DatabaseProvider;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

public class AuditRecordReader {

	private static Logger logger = LoggerFactory.getLogger(AuditRecordReader.class);
	@Autowired
	ApplicationContext appContext;
	private DatabaseProvider databaseProvider;
	@Value("${registry.system.base}")
	private String registrySystemContext;

	public AuditRecordReader(DatabaseProvider databaseProvider) {
		this.databaseProvider = databaseProvider;
	}

	public List<AuditRecord> fetchAuditRecords(String label, String predicate) throws LabelCannotBeNullException {
		List<AuditRecord> records = new ArrayList<>();
		if (label == null)
			throw new LabelCannotBeNullException("Label cannot be null");
		GraphTraversalSource traversalSource = databaseProvider.getGraphStore().traversal();
		GraphTraversal<Vertex, Vertex> traversal;
		if (predicate != null) {
			traversal = traversalSource.clone().V().hasLabel(getAuditLabel(label)).out(registrySystemContext + "audit")
					.has(registrySystemContext + "predicate", predicate);
		} else {
			traversal = traversalSource.clone().V().hasLabel(getAuditLabel(label)).out(registrySystemContext + "audit");
		}
		int recordCount = 0;
		while (traversal.hasNext()) {
			Vertex auditVertex = traversal.next();
			AuditRecord record = appContext.getBean(AuditRecord.class);
			/*record.subject(label);
			record.predicate(getValue(auditVertex, registrySystemContext + "predicate"));
			record.oldObject(getValue(auditVertex, registrySystemContext + "oldObject"));
			record.newObject(getValue(auditVertex, registrySystemContext + "newObject"));
			record.readOnlyAuthInfo(getValue(auditVertex, registrySystemContext + "authInfo"));*/
			records.add(record);
			logger.debug("AuditRecordReader - AuditRecord {}  : {} ", recordCount++, record);
		}
		return records;
	}

	private String getValue(Vertex auditVertex, String key) {
		VertexProperty property = auditVertex.property(key);
		String value = "";
		if (property.isPresent()) {
			Object object = property.value();
			if (object != null) {
				value = object.toString();
			}
		}
		return value;
	}

	private String getAuditLabel(String label) {
		return label + "-AUDIT";
	}
}
