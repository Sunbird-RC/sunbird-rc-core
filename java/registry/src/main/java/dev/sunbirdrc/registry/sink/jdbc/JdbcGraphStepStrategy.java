package dev.sunbirdrc.registry.sink.jdbc;

import org.apache.tinkerpop.gremlin.process.traversal.Step;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.step.HasContainerHolder;
import org.apache.tinkerpop.gremlin.process.traversal.step.filter.HasStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.GraphStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.NoOpBarrierStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.HasContainer;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.AbstractTraversalStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalHelper;

/**
 * Replaces each V()/E() in a JdbcGraph traversal with a JdbcGraphStep and moves the has()
 * steps that follow it into that step, so they can narrow the SQL read. Same shape as
 * TinkerPop's TinkerGraphStepStrategy; registered for JdbcGraph in its static initialiser.
 */
public final class JdbcGraphStepStrategy
        extends AbstractTraversalStrategy<TraversalStrategy.ProviderOptimizationStrategy>
        implements TraversalStrategy.ProviderOptimizationStrategy {

    private static final JdbcGraphStepStrategy INSTANCE = new JdbcGraphStepStrategy();

    private JdbcGraphStepStrategy() {
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void apply(final Traversal.Admin<?, ?> traversal) {
        if (TraversalHelper.onGraphComputer(traversal)) {
            return;
        }
        for (final GraphStep originalGraphStep : TraversalHelper.getStepsOfClass(GraphStep.class, traversal)) {
            final JdbcGraphStep<?, ?> jdbcGraphStep = new JdbcGraphStep<>(originalGraphStep);
            TraversalHelper.replaceStep(originalGraphStep, jdbcGraphStep, traversal);
            Step<?, ?> currentStep = jdbcGraphStep.getNextStep();
            while (currentStep instanceof HasStep || currentStep instanceof NoOpBarrierStep) {
                if (currentStep instanceof HasStep) {
                    for (final HasContainer hasContainer : ((HasContainerHolder) currentStep).getHasContainers()) {
                        if (!GraphStep.processHasContainerIds(jdbcGraphStep, hasContainer)) {
                            jdbcGraphStep.addHasContainer(hasContainer);
                        }
                    }
                    TraversalHelper.copyLabels(currentStep, currentStep.getPreviousStep(), false);
                    traversal.removeStep(currentStep);
                }
                currentStep = currentStep.getNextStep();
            }
        }
    }

    public static JdbcGraphStepStrategy instance() {
        return INSTANCE;
    }
}
