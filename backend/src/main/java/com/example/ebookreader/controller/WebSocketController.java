package com.example.ebookreader.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class WebSocketController {

    @Autowired
    private SimpMessagingTemplate template;

    // Пример отправки сообщения через REST API (для тестирования)
    @GetMapping("/send-notification")
    @ResponseBody
    public String sendNotification(@RequestParam String message) {
        template.convertAndSend("/topic/public", Map.of("content", message));
        return "Notification sent: " + message;
    }

    // Пример обработки сообщения от клиента и отправки ответа всем подписчикам
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public Map<String, String> sendMessage(Map<String, String> message) {
        System.out.println("Received message from client: " + message.get("content"));
        return Map.of("content", "Hello from server: " + message.get("content"));
    }
}