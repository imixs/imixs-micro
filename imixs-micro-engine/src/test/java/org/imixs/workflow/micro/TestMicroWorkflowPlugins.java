package org.imixs.workflow.micro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for Imixs MicroWorkflowService.
 * <p>
 * The test class verifies the Plugin functionality. Plugins can be defined
 * model
 * 
 * @author rsoika
 */
public class TestMicroWorkflowPlugins {

    private static final Logger logger = Logger.getLogger(TestMicroWorkflowPlugins.class.getName());

    private MicroWorkflowService workflowService;

    @BeforeEach
    public void setup() throws PluginException {
        workflowService = new MicroWorkflowService("sample");
        // load default model
        workflowService.loadBPMNModel("/bpmn/simple_plugins.bpmn");
    }

    /**
     * This test tests the basic behavior of the WorkflowKernel process method.
     */
    @Test
    public void testSimpleProcessingCycle() {

        ItemCollection workItem = new ItemCollection();
        workItem.model("1.0.0")
                .task(1000)
                .event(10);
        workItem.replaceItemValue("device.data", "Hello World");

        try {
            workflowService.processWorkItem(workItem);

            // verify workflow status
            assertEquals(1100, workItem.getTaskID());
            assertEquals("1.0.0", workItem.getModelVersion());

            // verify data
            assertEquals(workItem.getItemValueString("device.data"), "Hello World");
            // verify <item name="test.data">some other data</item>
            assertEquals(workItem.getItemValueString("test.data"), "some other data");

        } catch (ModelException | ProcessingErrorException | PluginException e) {
            e.printStackTrace();
            fail();
        }

    }

}