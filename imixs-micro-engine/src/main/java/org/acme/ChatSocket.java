package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/chat")
@ApplicationScoped
public class ChatSocket {

    Map<String, Session> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        sessions.put(session.getId(), session);
        broadcast("User " + session.getId() + " joined");
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session.getId());
        broadcast("User " + session.getId() + " left");
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        sessions.remove(session.getId());
        broadcast("User " + session.getId() + " left on error: " + throwable);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        broadcast(">> " + session.getId() + ": " + message);
    }

    private void broadcast(String message) {
        sessions.values().forEach(s -> {
            s.getAsyncRemote().sendObject(message, result ->  {
                if (result.getException() != null) {
                    System.out.println("Unable to send message: " + result.getException());
                }
            });
        });
    }
}

