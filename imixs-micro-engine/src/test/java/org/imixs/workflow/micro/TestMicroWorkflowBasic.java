package org.imixs.workflow.micro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Test class for Imixs MicroWorkflowService.
 * <p>
 * The test class verifies basic functionality.
 * 
 * @author rsoika
 */
public class TestMicroWorkflowBasic {

    private static final Logger logger = Logger.getLogger(TestMicroWorkflowBasic.class.getName());

    private MicroWorkflowService workflowService;

    @BeforeEach
    public void setup() throws PluginException {
        workflowService = new MicroWorkflowService();
        workflowService.setDevice("workstation-1");
        // load default model
        workflowService.loadBPMNModel("/bpmn/example-001.bpmn");
    }

    /**
     * This test tests the basic behavior of the WorkflowKernel process method.
     */
    @Test
    @Disabled
    public void testSimpleProcessingCycle() {

        ItemCollection workItem = new ItemCollection();
        workItem.model("1.0.0")
                .task(1000)
                .event(20);
        workItem.replaceItemValue("device.data", "Hello World");

        try {
            workflowService.processWorkItem(workItem);

            // verify workflow status
            assertEquals(1100, workItem.getTaskID());
            assertEquals("1.0.0", workItem.getModelVersion());

            // verify data
            assertEquals(workItem.getItemValueString(WorkflowKernel.WORKFLOWSTATUS), "Produce");
            assertEquals(workItem.getItemValueString("device.data"), "Hello World");
        } catch (ModelException | ProcessingErrorException | PluginException e) {
            e.printStackTrace();
            fail();
        }

    }

}
