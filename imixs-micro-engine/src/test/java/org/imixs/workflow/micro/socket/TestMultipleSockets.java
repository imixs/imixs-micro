package org.imixs.workflow.micro.socket;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Session;

/**
 * This junit test test different call and creates for each test method a new
 * Web Socket connection.
 * 
 */
@QuarkusTest
public class TestMultipleSockets {

    private static final Logger logger = Logger.getLogger(TestMultipleSockets.class.getName());

    // @TestHTTPResource("/workflow/1.0.0/1000/20")
    @TestHTTPResource("/workflow")
    URI uri;

    private Session session;
    private Client client = null;

    @BeforeEach
    public void setup() throws Exception {
        client = new Client();
        session = ContainerProvider.getWebSocketContainer().connectToServer(client, uri);
    }

    @AfterEach
    public void teardown() throws Exception {
        if (session != null && session.isOpen()) {
            session.close();
        }
    }

    /**
     * This test simulates the creation of a new Workitem on the remote workflow
     * engine.
     */
    // @Test
    // public void testInitialConnection() throws Exception {
    // ItemCollection workitem = client.receiveWorkitem();
    // Assertions.assertNotNull(workitem);
    // logger.info("MESSAGE=" + workitem.getUniqueID());
    // logger.info("TaskID=" + workitem.getTaskID());
    // Assertions.assertEquals(1100, workitem.getTaskID());
    // }

    /**
     * This test creates a new Workitem and send it to the server to be processed.
     * 
     * @throws Exception
     */
    @Test
    public void testSendAndReceiveWorkitem() throws Exception {
        // First, receive the initial workitem (we need to do this to clear the queue)
        // client.receiveWorkitem();
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

    private void sendItemCollection(ItemCollection item) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(item.getAllItems());
        oos.close();

        session.getBasicRemote().sendBinary(ByteBuffer.wrap(baos.toByteArray()));
    }

}
