package com.cinefinder.global.websocket.listener;

import com.cinefinder.chat.data.entity.ChatMessage;
import com.cinefinder.chat.data.entity.ChatType;
import com.cinefinder.chat.service.ChatRoomService;
import com.cinefinder.chat.service.RedisSessionService;
import com.cinefinder.chat.data.dto.SessionInfo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final RedisSessionService redisSessionService;
    private final ChatRoomService chatRoomService;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = StompHeaderAccessor.wrap(event.getMessage()).getSessionId();

        SessionInfo info = redisSessionService.getSessionInfo(sessionId);
        if (info != null) {
            ChatMessage systemMessage = ChatMessage.builder()
                    .type(ChatType.SYSTEM)
                    .senderId(String.valueOf(info.getUserId()))
                    .nickName(info.getNickname())
                    .movieId(info.getMovieId())
                    .message(info.getNickname() + "님이 퇴장하셨습니다.")
                    .build();

            chatRoomService.handleLeave(info.getMovieId(), systemMessage);
            redisSessionService.removeSession(sessionId);
            logger.info("❌ WebSocket 종료: sessionId={}", sessionId);
        }
    }
}
