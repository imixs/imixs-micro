package org.imixs.workflow.micro.socket;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.micro.MicroWorkflowService;
import org.imixs.workflow.micro.plugins.ResultPlugin;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint("/workflow{action}")
@ApplicationScoped
public class WorkflowEngine {

    private Set<Session> sessions;

    private static final Logger logger = Logger.getLogger(ResultPlugin.class.getName());

    private MicroWorkflowService workflowService;

    @PostConstruct
    void init() throws PluginException {
        sessions = ConcurrentHashMap.newKeySet();

        workflowService = new MicroWorkflowService("sample");
        // load default model
        workflowService.loadBPMNModel("/bpmn/simple.bpmn");
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("action") String action) {
        sessions.add(session);
        switch (action) {
            case "start":
                process(session);
                break;
            case "update":
                process(session);
                break;
            default:
                session.getAsyncRemote().sendText("Unknown action: " + action);
        }
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        // broadcast("User " + session.getId() + " left");
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        sessions.remove(session);

    }

    @OnMessage
    public void onMessage(String message, Session session) {

        logger.info(">> " + session.getId() + ": " + message);
    }

    private void broadcast(String message) {
        for (Session session : sessions) {
            session.getAsyncRemote().sendText(message);
        }
    }

    /**
     * This method processes a workitem instance
     */
    protected void process(Session session) {

        ItemCollection workItem = new ItemCollection();
        workItem.model("1.0.0")
                .task(1000)
                .event(10);
        workItem.replaceItemValue("device.data", "Hello World");

        try {
            workflowService.processWorkItem(workItem);

        } catch (ModelException | ProcessingErrorException | PluginException e) {
            e.printStackTrace();

        }

    }

}
