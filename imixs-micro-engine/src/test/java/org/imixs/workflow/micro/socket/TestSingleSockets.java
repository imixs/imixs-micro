package org.imixs.workflow.micro.socket;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Session;

/**
 * This junit test test different calls by using one single websocket connection
 * 
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class TestSingleSockets {

    private static final Logger logger = Logger.getLogger(TestMultipleSockets.class.getName());

    @TestHTTPResource("/workflow/1.0.0/1000/20")
    URI uri;

    private Session session;
    private Client client = null;

    /**
     * Create one single webSocket connection for all tests
     * 
     * @throws Exception
     */
    @BeforeAll
    public void setup() throws Exception {
        client = new Client();
        session = ContainerProvider.getWebSocketContainer().connectToServer(client, uri);
    }

    @AfterAll
    public void teardown() throws Exception {
        if (session != null && session.isOpen()) {
            session.close();
        }
    }

    /**
     * This test simulates the creation of a new Workitem on the remote workflow
     * engine.
     */
    @Test
    @Order(1)
    public void testInitialConnection() throws Exception {

        ItemCollection workitem = client.receiveWorkitem();
        Assertions.assertNotNull(workitem, "No initial ItemCollection received");
        logger.info("Received initial workitem: " + workitem.getItemValueString("$uniqueid"));

        Assertions.assertNotNull(workitem);
        logger.info("MESSAGE=" + workitem.getUniqueID());
        logger.info("TaskID=" + workitem.getTaskID());
        Assertions.assertEquals(1100, workitem.getTaskID());

    }

    /**
     * This test creates a new Workitem and send it to the server to be processed.
     * 
     * @throws Exception
     */
    @Test
    @Order(2)
    public void testSendAndReceiveWorkitem() throws Exception {
        // Create a new ItemCollection to send
        ItemCollection workitemToSend = new ItemCollection();
        workitemToSend.model("1.0.0").task(1000).event(10);
        workitemToSend.replaceItemValue("subject", "Test Workitem");

        // Create and send a new workitem
        sendItemCollection(workitemToSend);

        // Receive the processed workitem
        ItemCollection processedWorkitem = client.receiveWorkitem();
        Assertions.assertNotNull(processedWorkitem, "No processed ItemCollection received");
        logger.info("Received processed workitem: " + processedWorkitem.getItemValueString("$uniqueid"));

        // Assert the processed workitem properties
        Assertions.assertEquals("1.0.0", processedWorkitem.getItemValueString("$modelversion"));
        Assertions.assertEquals(1000, processedWorkitem.getItemValueInteger("$taskid"));
        Assertions.assertEquals("Test Workitem", processedWorkitem.getItemValueString("subject"));
    }

    /**
     * This method sends a given Workitem to the client using a given WebSocket
     * session .
     * 
     * @param workItem
     * @param session
     * @throws Exception
     */
    private void sendItemCollection(ItemCollection item) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(item.getAllItems());
        oos.close();

        session.getBasicRemote().sendBinary(ByteBuffer.wrap(baos.toByteArray()));
    }

}
