# Imixs-Micro

A lightweight workflow service running on plain Java VMs 

## Example

To run the engine, see the following code example

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

        int newTask=workItem.getTaskID();

    } catch (ModelException | ProcessingErrorException | PluginException e) {
        e.printStackTrace();
    }

```
