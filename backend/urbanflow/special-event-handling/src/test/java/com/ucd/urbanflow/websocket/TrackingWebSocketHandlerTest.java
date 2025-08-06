package com.ucd.urbanflow.websocket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * TrackingWebSocketHandler 的单元测试
 * 目的: 验证 WebSocket 的生命周期回调方法 (连接建立/关闭) 是否正确地调用了 publisher 的相应方法。
 */
@ExtendWith(MockitoExtension.class)
class TrackingWebSocketHandlerTest {

    @Mock // 模拟依赖的 Publisher
    private TrackingDataPublisher publisher;

    @Mock // 模拟一个 WebSocket 会话对象
    private WebSocketSession session;

    @InjectMocks // 将 mock publisher 注入到 handler 实例中
    private TrackingWebSocketHandler trackingWebSocketHandler;

    @Test
    void testAfterConnectionEstablished() throws Exception {
        // Arrange: 准备工作 (已通过 @Mock 和 @InjectMocks 完成)

        // Act: 调用连接建立的回调方法
        trackingWebSocketHandler.afterConnectionEstablished(session);

        // Assert: 验证 publisher.addSession(session) 方法是否被用正确的参数调用了 1 次
        verify(publisher, times(1)).addSession(session);
    }

    @Test
    void testAfterConnectionClosed() throws Exception {
        // Arrange: 准备 CloseStatus 对象
        CloseStatus closeStatus = CloseStatus.NORMAL;

        // Act: 调用连接关闭的回调方法
        trackingWebSocketHandler.afterConnectionClosed(session, closeStatus);

        // Assert: 验证 publisher.removeSession(session) 方法是否被用正确的参数调用了 1 次
        verify(publisher, times(1)).removeSession(session);
    }
}