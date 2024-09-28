package org.imixs.workflow.micro.socket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.micro.MicroWorkflowService;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

@Startup
// @ServerEndpoint("/workflow/{modelversion}/{task}/{event}")
@ServerEndpoint("/workflow")
@ApplicationScoped
public class WorkflowEngine {

    private Set<Session> sessions;

    private static final Logger logger = Logger.getLogger(WorkflowEngine.class.getName());

    @Inject
    private MicroWorkflowService workflowService;

    /**
     * Creates a new MicroWorkflowService and load the default models.
     * 
     * @throws PluginException
     */
    @PostConstruct
    void init() throws PluginException {
        logger.info("├── Init Workflow-Engine...");
        sessions = ConcurrentHashMap.newKeySet();
        // workflowService = new MicroWorkflowService();

        workflowService.setDevice("sample");
        // load default models
        loadDefaultModels();
    }

    /**
     * Starts a new workflow based on the given model information
     */

    @OnOpen
    public void onOpen(Session session) {

        logger.info("---OPEN");
        sessions.add(session);
        // broadcast("User " + session.getId() + " joined"); // Add this line

        // ItemCollection workitem = new ItemCollection();
        // workitem.model(modelversion).task(Integer.parseInt(taskid)).event(Integer.parseInt(eventid));
        // try {
        // workflowService.processWorkItem(workitem);
        // logger.info("---PROCESS OK");
        // sendItemCollection(session, workitem); // Send the processed workitem
        // logger.info("---SEND OK");
        // } catch (PluginException | AccessDeniedException | ProcessingErrorException |
        // ModelException e) {
        // logger.info("---ERROR: " + e.getMessage());
        // // session.getAsyncRemote().sendText("Unknown action: " + e.getMessage());
        // }
    }

    // private void onOpenOLD(Session session, @PathParam("modelversion") String
    // modelversion,
    // @PathParam("task") String taskid,
    // @PathParam("event") String eventid) {

    // logger.info("---OPEN");
    // sessions.add(session);
    // // broadcast("User " + session.getId() + " joined"); // Add this line

    // ItemCollection workitem = new ItemCollection();
    // workitem.model(modelversion).task(Integer.parseInt(taskid)).event(Integer.parseInt(eventid));
    // try {
    // workflowService.processWorkItem(workitem);
    // logger.info("---PROCESS OK");
    // sendItemCollection(session, workitem); // Send the processed workitem
    // logger.info("---SEND OK");
    // } catch (PluginException | AccessDeniedException | ProcessingErrorException |
    // ModelException e) {
    // logger.info("---ERROR: " + e.getMessage());
    // // session.getAsyncRemote().sendText("Unknown action: " + e.getMessage());
    // }
    // }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        // broadcast("User " + session.getId() + " left");
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.info("---ERROR : " + throwable.getMessage());
        sessions.remove(session);

    }

    /**
     * This method receives a serialized hash map containing the items of a
     * Workitem. The Workitem is converted into a ItemCollection and stored in the
     * MESSAGES queue
     * 
     * @param bytes
     */
    @SuppressWarnings("unchecked")
    @OnMessage
    public void onMessage(ByteBuffer bytes, Session session) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes.array());
            ObjectInputStream ois = new ObjectInputStream(bais);
            Map<String, List<Object>> data = (Map<String, List<Object>>) ois.readObject();
            ItemCollection workitem = new ItemCollection(data);
            logger.info("├── Received Workitem: " + workitem.getItemValueString("$uniqueid"));
            logger.info("│   ├── ModelVersion: " + workitem.getModelVersion() + " Task: " + workitem.getTaskID());
            // Process the received workitem
            workflowService.processWorkItem(workitem);

            logger.info("│   ├── Workitem processed: " + workitem.getItemValueString("$uniqueid"));
            // Send back the processed workitem
            logger.info("└───┴ Sending result...");
            sendItemCollection(session, workitem);
        } catch (IOException | ClassNotFoundException | AccessDeniedException | ProcessingErrorException
                | PluginException | ModelException e) {
            logger.severe("Error receiving ItemCollection: " + e.getMessage());
        }
    }

    /**
     * This method scans for .bpmn files in the current working directory and loads
     * these models into the workflow service.
     */
    private void loadDefaultModels() throws PluginException {
        logger.info("├── Loading default Models");
        // Scan for .bpmn files in current class path
        loadModelsFromClasspath();
        // Scan for .bpmn files in the /bpmn/ directory
        // Load from external directory
        loadModelsFromDirectory("/bpmn");
    }

    /**
     * Helper method to load models form class path
     */
    private void loadModelsFromClasspath() {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL bpmnDirUrl = classLoader.getResource("bpmn");
            if (bpmnDirUrl != null && "file".equals(bpmnDirUrl.getProtocol())) {
                File bpmnDir = new File(bpmnDirUrl.toURI());
                File[] bpmnFiles = bpmnDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".bpmn"));
                if (bpmnFiles != null) {
                    for (File bpmnFile : bpmnFiles) {
                        String bpmnPath = "/bpmn/" + bpmnFile.getName();
                        logger.info("│   ├── loading BPMN model: " + bpmnPath);
                        workflowService.loadBPMNModel(bpmnPath);
                    }
                }
            }
        } catch (URISyntaxException e) {
            logger.severe("Error scanning for BPMN files: " + e.getMessage());
        }
    }

    /**
     * Helper method to load models form absolut path /bpmn
     */
    private void loadModelsFromDirectory(String path) {
        logger.info("│   ├── scanning directory: " + path);
        File bpmnDir = new File(path);
        File[] bpmnFiles = bpmnDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".bpmn"));
        if (bpmnFiles != null) {
            for (File bpmnFile : bpmnFiles) {
                String bpmnPath = bpmnFile.getAbsolutePath();
                logger.info("│   ├── loading BPMN model: " + bpmnPath);

                try (FileInputStream fileInputStream = new FileInputStream(bpmnFile)) {
                    workflowService.loadBPMNModel(fileInputStream);
                } catch (IOException e) {
                    logger.severe("│ │ ├── error reading file: " + bpmnPath + " - " + e.getMessage());
                }

            }
        }
    }

    /**
     * This method sends a Workitem as a message object in a WebSocket connects. The
     * method sends the message asynchronously which is needed for performance and
     * stability reasons.
     * 
     * @param session
     * @param item
     */
    private void sendItemCollection(Session session, ItemCollection item) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(item.getAllItems());
            oos.close();

            final ByteBuffer byteBuffer = ByteBuffer.wrap(baos.toByteArray());

            // Use getAsyncRemote() instead of getBasicRemote()
            session.getAsyncRemote().sendBinary(byteBuffer, result -> {
                if (result.isOK()) {
                    logger.info("sendBinary OK!");
                } else {
                    logger.severe("Error sending ItemCollection: " + result.getException().getMessage());
                }
            });
        } catch (Exception e) {
            logger.severe("Error preparing ItemCollection: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
