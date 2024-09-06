# Imixs-Micro

A lightweight workflow service running on plain Java VMs 

To add the workflow engine into your Maven java project add the following maven dependencies:

```xml
    <dependency>
        <groupId>org.imixs.workflow</groupId>
        <artifactId>imixs-micro</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
```

## Example

To run the engine you just need to define a BPMN model


<img src="doc/model-example-01.png" alt="Imixs Workflow"  />


and run the engine in your Java code. See the following code example:

```java
    private MicroWorkflowService workflowService;

    // setup
    workflowService = new MicroWorkflowService("sample");
    // load a model
    workflowService.loadBPMNModel("/bpmn/simple_plugins.bpmn");
    
    // process a workitem
    ItemCollection workItem = new ItemCollection();
    workItem.model("1.0.0")
            .task(1000)
            .event(10);
    // set some data..
    workItem.replaceItemValue("device.data", "Hello World");

    try {
        workflowService.processWorkItem(workItem);
        // next task is now 1100
        assertEquals(1100, workItem.getTaskID());
        assertEquals(workItem.getItemValueString(WorkflowKernel.WORKFLOWSTATUS), "Task 2");

    } catch (ModelException | ProcessingErrorException | PluginException e) {
        e.printStackTrace();
    }

```
