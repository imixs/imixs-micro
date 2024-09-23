package org.imixs.workflow.micro.socket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;

/**
 * WebSocket client to be used to connect to a Imixs-Micro WorkflowEngine.
 */
@ClientEndpoint
public class Client {

    private static final Logger logger = Logger.getLogger(Client.class.getName());
    private LinkedBlockingDeque<ItemCollection> MESSAGES = null;

    public Client() {
        super();
        MESSAGES = new LinkedBlockingDeque<>();
    }

    @OnOpen
    public void open(Session session) {
        // No action needed
    }

    /**
     * This method receives a serialized hash map containing the items of a
     * Workitem. The Workitem is converted into a ItemCollection and stored in the
     * MESSAGES queue
     * 
     * @param bytes
     */
    @OnMessage
    public void onMessage(ByteBuffer bytes) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes.array());
            ObjectInputStream ois = new ObjectInputStream(bais);
            Map<String, List<Object>> data = (Map<String, List<Object>>) ois.readObject();
            ItemCollection workitem = new ItemCollection(data);
            MESSAGES.add(workitem); // Add the deserialized ItemCollection to the queue
        } catch (IOException | ClassNotFoundException e) {
            logger.severe("Error receiving ItemCollection: " + e.getMessage());
        }
    }

    public ItemCollection receiveWorkitem() throws Exception {
        return MESSAGES.poll(10, TimeUnit.SECONDS);
    }

}