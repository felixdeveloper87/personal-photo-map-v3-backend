// package com.personalphotomap.controller;

// import org.springframework.messaging.simp.SimpMessagingTemplate;
// import org.springframework.stereotype.Controller;

// @Controller
// public class WebSocketController {

//     private final SimpMessagingTemplate messagingTemplate;

//     public WebSocketController(SimpMessagingTemplate messagingTemplate) {
//         this.messagingTemplate = messagingTemplate;
//     }

//     public void sendNotification(String email, String message) {
//         String destination = "/topic/user/" + email;
//         messagingTemplate.convertAndSend(destination, message);
//     }
// }
