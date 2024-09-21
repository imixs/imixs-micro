# Imixs-Micro-Engine

Imixs-Micro-Engine is a lightweight BPMN Workflow engine built on top of the Quarkus framework. This project aims to provide a high-performance, low-footprint workflow solution that can be easily deployed in containerized environments.

At its core, Imixs-Micro-Engine leverages the power of WebSocket technology to enable real-time, bidirectional communication between the workflow engine and its clients. This allows for immediate updates and responsive workflow management, making it ideal for modern, distributed applications.

Built with Java 17 and the latest Quarkus framework, Imixs-Micro takes advantage of cutting-edge features for optimal performance. The project structure follows Maven conventions, making it familiar and easy to navigate for Java developers.

Key features of Imixs-Micro-Engine include:

- Real-time workflow updates using WebSocket technology
- Lightweight and fast Quarkus-based runtime
- Containerized deployment with Docker support
- GraalVM integration for potential native compilation
- Comprehensive JUnit tests ensuring reliability

The engine is designed with cloud-native principles in mind. It can be easily deployed as a standalone JAR or as a Docker container, making it versatile for various deployment scenarios - from development environments to production Kubernetes clusters.

The Imixs-Micro Docker integration uses a Red Hat Universal Base Image (UBI) with OpenJDK 17, ensuring a stable and secure foundation. The inclusion of GraalVM in our Docker image opens up possibilities for native compilation, potentially further reducing startup time and memory footprint.

Imixs-Micro-Engine represents a new approach to workflow management - one that embraces modern, cloud-native technologies while providing the robust BPMN support that businesses rely on. Whether you're building a small-scale workflow application or a large, distributed system, Imixs-Micro-Engine offers the flexibility, performance, and ease-of-use to meet your needs.

Get started with Imixs-Micro-Engine today and experience the future of lightweight, high-performance workflow management!

For development read the [Developer Guide](./DEVELOPMENT.md)






## Example of a local implementation 

The following example demonstrate how to use the MicroWorkflowService in a plain java project. 

To add the workflow engine into your Maven java project add the following maven dependencies:

```xml
    <dependency>
        <groupId>org.imixs.workflow</groupId>
        <artifactId>imixs-micro</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
```

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
