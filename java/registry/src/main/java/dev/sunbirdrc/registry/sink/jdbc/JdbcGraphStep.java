package dev.sunbirdrc.registry.sink.jdbc;

import org.apache.tinkerpop.gremlin.process.traversal.Compare;
import org.apache.tinkerpop.gremlin.process.traversal.Contains;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.step.HasContainerHolder;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.GraphStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.HasContainer;
import org.apache.tinkerpop.gremlin.process.traversal.util.AndP;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A GraphStep for JdbcGraph that reads from the label's own table.
 *
 * Without it, V().hasLabel(x) calls JdbcGraph.vertices() with no ids, which loads every
 * row of every vertex table and filters in memory - fine for a handful of rows, fatal
 * once a registry holds hundreds of thousands of entities. JdbcGraphStepStrategy folds
 * the has() steps that follow V() into this step; here a label narrows the read to
 * V_{label} and an equality on a property becomes a WHERE clause. Every has() condition
 * is still re-tested on each vertex, so results are the same as before, only cheaper.
 */
public final class JdbcGraphStep<S, E extends Element> extends GraphStep<S, E> implements HasContainerHolder {

    private final List<HasContainer> hasContainers = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public JdbcGraphStep(final GraphStep<S, E> originalGraphStep) {
        super(originalGraphStep.getTraversal(), originalGraphStep.getReturnClass(),
                originalGraphStep.isStartStep(), originalGraphStep.getIds());
        originalGraphStep.getLabels().forEach(this::addLabel);
        this.setIteratorSupplier(() -> (Iterator<E>) (Vertex.class.isAssignableFrom(this.returnClass)
                ? this.vertices() : this.edges()));
    }

    private JdbcGraph graph() {
        return (JdbcGraph) this.getTraversal().getGraph().get();
    }

    private Iterator<? extends Vertex> vertices() {
        if (null == this.ids) {
            return Collections.emptyIterator();
        }
        if (this.ids.length > 0) {
            return filtered(graph().vertices(this.ids));
        }

        Set<String> labels = labelsToRead();
        HasContainer equality = propertyEquality();
        if (labels.isEmpty()) {
            // No label to narrow the read: an equality still becomes a WHERE clause per table.
            return equality == null
                    ? filtered(graph().vertices())
                    : filtered(graph().getVerticesByProperty(null, equality.getKey(), equality.getValue()));
        }

        List<Vertex> vertices = new ArrayList<>();
        for (String label : labels) {
            readLabel(label, equality).forEachRemaining(vertices::add);
        }
        return filtered(vertices.iterator());
    }

    /**
     * One label's vertices, narrowed by the equality in SQL when there is one. If the
     * database rejects the comparison (e.g. a column type the value cannot be bound to),
     * fall back to the whole label table; the in-memory filter gives the same result.
     */
    private Iterator<Vertex> readLabel(String label, HasContainer equality) {
        if (equality == null) {
            return graph().getVerticesByLabel(label);
        }
        try {
            return graph().getVerticesByProperty(label, equality.getKey(), equality.getValue());
        } catch (RuntimeException e) {
            return graph().getVerticesByLabel(label);
        }
    }

    private Iterator<? extends Edge> edges() {
        if (null == this.ids) {
            return Collections.emptyIterator();
        }
        return filtered(this.ids.length > 0 ? graph().edges(this.ids) : graph().edges());
    }

    private <X extends Element> Iterator<X> filtered(final Iterator<X> iterator) {
        return IteratorUtils.filter(iterator, element -> HasContainer.testAll(element, this.hasContainers));
    }

    /**
     * Labels the step is restricted to by hasLabel(x) or hasLabel(x, y). Empty when there
     * is no label restriction we can read by, so every table has to be considered.
     */
    private Set<String> labelsToRead() {
        for (HasContainer container : this.hasContainers) {
            if (!T.label.getAccessor().equals(container.getKey())) {
                continue;
            }
            P<?> predicate = container.getPredicate();
            Object value = predicate.getValue();
            if (predicate.getBiPredicate() == Compare.eq && value instanceof String) {
                return Collections.singleton((String) value);
            }
            if (predicate.getBiPredicate() == Contains.within && value instanceof Collection
                    && ((Collection<?>) value).stream().allMatch(String.class::isInstance)) {
                Set<String> labels = new LinkedHashSet<>();
                ((Collection<?>) value).forEach(label -> labels.add((String) label));
                return labels;
            }
        }
        return Collections.emptySet();
    }

    /** The first has(key, value) on an ordinary property that can be sent to SQL as key = value. */
    private HasContainer propertyEquality() {
        for (HasContainer container : this.hasContainers) {
            String key = container.getKey();
            if (key == null || T.label.getAccessor().equals(key) || T.id.getAccessor().equals(key)) {
                continue;
            }
            P<?> predicate = container.getPredicate();
            if (predicate.getBiPredicate() == Compare.eq && predicate.getValue() != null) {
                return container;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        if (this.hasContainers.isEmpty()) {
            return super.toString();
        }
        return 0 == this.ids.length
                ? StringFactory.stepString(this, this.returnClass.getSimpleName().toLowerCase(), this.hasContainers)
                : StringFactory.stepString(this, this.returnClass.getSimpleName().toLowerCase(),
                        Arrays.toString(this.ids), this.hasContainers);
    }

    @Override
    public List<HasContainer> getHasContainers() {
        return Collections.unmodifiableList(this.hasContainers);
    }

    @Override
    public void addHasContainer(final HasContainer hasContainer) {
        if (hasContainer.getPredicate() instanceof AndP) {
            for (final P<?> predicate : ((AndP<?>) hasContainer.getPredicate()).getPredicates()) {
                this.addHasContainer(new HasContainer(hasContainer.getKey(), predicate));
            }
        } else {
            this.hasContainers.add(hasContainer);
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ this.hasContainers.hashCode();
    }
}
