package org.imixs.workflow.micro.socket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;

/**
 * WebSocket client to be used to connect to a Imixs-Micro WorkflowEngine.
 */
@ClientEndpoint
public class WebSocketClient {

    private static final Logger logger = Logger.getLogger(WebSocketClient.class.getName());
    private LinkedBlockingDeque<ItemCollection> MESSAGES = null;
    private Session session = null;
    private String endpoint = null;

    public WebSocketClient(String endpoint) throws DeploymentException, IOException {
        super();
        this.endpoint = endpoint;
        MESSAGES = new LinkedBlockingDeque<>();

        session = ContainerProvider.getWebSocketContainer().connectToServer(this,
                URI.create(endpoint));
    }

    /**
     * Returns the current web socket connections
     * 
     * @return
     */
    public Session getSession() {
        return session;
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

    /**
     * This method sends a given Workitem to the client using a given WebSocket
     * session .
     * 
     * @param workItem
     * @param session
     * @throws IOException
     */
    public void sendItemCollection(ItemCollection item) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        oos = new ObjectOutputStream(baos);
        oos.writeObject(item.getAllItems());
        oos.close();
        session.getBasicRemote().sendBinary(ByteBuffer.wrap(baos.toByteArray()));
    }

    /**
     * Close the current webSocket connection
     * 
     * @throws PluginException
     */
    public void close() throws PluginException {
        if (session != null && session.isOpen()) {

            try {
                session.close();
            } catch (IOException e) {
                throw new PluginException(WebSocketAdapter.class.getSimpleName(), "ERROR_API",
                        "Failed to close webSocket connection to endpoint '" + endpoint + "' : "
                                + e.getMessage(),
                        e);
            }
        }
    }
}