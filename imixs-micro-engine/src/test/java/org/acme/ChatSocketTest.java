package org.acme;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.websocket.*;
import java.net.URI;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;


@QuarkusTest
public class ChatSocketTest {

    private static final LinkedBlockingDeque<String> MESSAGES = new LinkedBlockingDeque<>();

    @TestHTTPResource("/chat")
    URI uri;

    @Test
    public void testWebSocket() throws Exception {
        try (Session session = ContainerProvider.getWebSocketContainer().connectToServer(Client.class, uri)) {
            String msg = MESSAGES.poll(10, TimeUnit.SECONDS);
            Assertions.assertNotNull(msg);
            Assertions.assertTrue(msg.matches("User .+ joined"));

            session.getBasicRemote().sendText("hello");
            msg = MESSAGES.poll(10, TimeUnit.SECONDS);
            Assertions.assertNotNull(msg);
            Assertions.assertTrue(msg.matches(">> .+: hello"));
        }
    }

    @ClientEndpoint
    public static class Client {

        @OnOpen
        public void open(Session session) {
            // No action needed
        }

        @OnMessage
        void message(String msg) {
            MESSAGES.add(msg);
        }
    }
}
