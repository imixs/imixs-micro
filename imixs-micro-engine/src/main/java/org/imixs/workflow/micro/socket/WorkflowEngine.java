package org.imixs.workflow.micro.socket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import org.imixs.workflow.micro.plugins.ResultPlugin;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

@Startup
@ServerEndpoint("/workflow/{modelversion}/{task}/{event}")
@ApplicationScoped
public class WorkflowEngine {

    private Set<Session> sessions;

    private static final Logger logger = Logger.getLogger(ResultPlugin.class.getName());

    private MicroWorkflowService workflowService;

    /**
     * Creates a new MicroWorkflowService and load the default models.
     * 
     * @throws PluginException
     */
    @PostConstruct
    void init() throws PluginException {
        sessions = ConcurrentHashMap.newKeySet();
        workflowService = new MicroWorkflowService("sample");
        // load default models
        loadDefaultModels();
    }

    /**
     * Starts a new workflow based on the given model information
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("modelversion") String modelversion,
            @PathParam("task") String taskid,
            @PathParam("event") String eventid) {

        logger.info("---OPEN");
        sessions.add(session);
        // broadcast("User " + session.getId() + " joined"); // Add this line

        ItemCollection workitem = new ItemCollection();
        workitem.model(modelversion).task(Integer.parseInt(taskid)).event(Integer.parseInt(eventid));
        try {
            workflowService.processWorkItem(workitem);
            logger.info("---PROCESS OK");
            sendItemCollection(session, workitem); // Send the processed workitem
            logger.info("---SEND OK");
        } catch (PluginException | AccessDeniedException | ProcessingErrorException | ModelException e) {
            logger.info("---ERROR: " + e.getMessage());
            // session.getAsyncRemote().sendText("Unknown action: " + e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        // broadcast("User " + session.getId() + " left");
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.info("---ERROR");
        sessions.remove(session);

    }

    /**
     * This method receives a serialized hash map containing the items of a
     * Workitem. The Workitem is converted into a ItemCollection and stored in the
     * MESSAGES queue
     * 
     * @param bytes
     */
    @OnMessage
    public void onMessage(ByteBuffer bytes, Session session) {
        try {
            logger.info("on Message........");
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes.array());
            ObjectInputStream ois = new ObjectInputStream(bais);
            Map<String, List<Object>> data = (Map<String, List<Object>>) ois.readObject();
            ItemCollection workitem = new ItemCollection(data);

            logger.info("Received Workitem: " + workitem.getItemValueString("$uniqueid"));
            // Process the received workitem
            workflowService.processWorkItem(workitem);
            // Send back the processed workitem
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

        // Scan for .bpmn files in the /bpmn/ directory
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL bpmnDirUrl = classLoader.getResource("bpmn");
            if (bpmnDirUrl != null && "file".equals(bpmnDirUrl.getProtocol())) {
                File bpmnDir = new File(bpmnDirUrl.toURI());
                File[] bpmnFiles = bpmnDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".bpmn"));

                if (bpmnFiles != null) {
                    for (File bpmnFile : bpmnFiles) {
                        String bpmnPath = "/bpmn/" + bpmnFile.getName();
                        logger.info("Loading BPMN model: " + bpmnPath);
                        workflowService.loadBPMNModel(bpmnPath);
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("Error scanning for BPMN files: " + e.getMessage());
            // throw new PluginException(WorkflowEngine.class.getName(),
            // ModelException.INVALID_MODEL, e.getMessage());
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
