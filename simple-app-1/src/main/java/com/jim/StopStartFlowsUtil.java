package com.jim;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.lifecycle.Callable;
import org.mule.construct.AbstractFlowConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A utility to start and stop flows based on an annotation called 'stoppable' with a value of
 * 'true'. This can be injected anywhere that supports the {@link Callable} pattern and sends
 * an 'action' messsage with either 'START' or 'STOP'.
 */
public class StopStartFlowsUtil implements Callable {

    private static Logger logger = LoggerFactory.getLogger(StopStartFlowsUtil.class);

    // A QName from XML Schema that we can mark a Flow with to indicate stoppable flows
    // a value of 'true' means the flow is stoppable.
    private static QName STOPPABLE = new QName("http://www.mulesoft.org/schema/mule/core", "stoppable");



    /**
     * For any flow annotated with 'stoppable' = true, apply the action to the flow.
     *
     * {@inheritDoc}
     */
    @Override
    public Object onCall(MuleEventContext eventContext) throws Exception {
        final HashMap<String, String> payload = (HashMap<String, String>) eventContext.getMessage().getPayload();
        final FlowState state = FlowState.valueOf(payload.get("action").toUpperCase());
        final FlowConstruct thisConstruct = eventContext.getFlowConstruct();
        final Class<AbstractFlowConstruct> flowClass = AbstractFlowConstruct.class;
        final Collection<FlowConstruct> flows = eventContext.getMuleContext().getRegistry().lookupFlowConstructs();

        List<AbstractFlowConstruct> flowsToProcess = flows.parallelStream().filter(flow -> flow != thisConstruct)
                .filter(flowClass::isInstance).map(flowClass::cast)
                .filter(flow -> "true".equals(flow.getAnnotation(STOPPABLE)))
                .collect(Collectors.toList());

        FlowProcessor processor = state.getProcessor();
        StringBuilder message = new StringBuilder("Stopped flow(s):");
        for (AbstractFlowConstruct flow : flowsToProcess) {
            try {
                processor.process(flow);
                message.append(" " + flow.getName());
            } catch (MuleException e) {
                logger.error("Could not '{}' flow named '{}'", state.name(), flow.getName());
            }
        }
        final String output = message.toString();
        logger.info(output);

        return output;
    }

    @FunctionalInterface
    interface FlowProcessor {
        void process(AbstractFlowConstruct flow) throws MuleException;
    }


    public enum FlowState {
        STOP(AbstractFlowConstruct::stop),
        START(AbstractFlowConstruct::start);
        private FlowProcessor processor;

        public FlowProcessor getProcessor() {
            return processor;
        }

        FlowState(FlowProcessor processor) {
            this.processor = processor;
        }
    }
}
