package com.ucd.urbanflow.websocket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * WebSocketConfig 的单元测试
 * 目的: 仅为满足100%覆盖率要求，验证配置方法是否被调用。
 * 注意：此测试不验证配置是否实际生效，集成测试是更好的选择。
 */
@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

    @Mock
    private TrackingWebSocketHandler trackingWebSocketHandler;

    @Mock
    private EventResultHandler eventResultHandler;

    @InjectMocks
    private WebSocketConfig webSocketConfig;

    @Test
    void testRegisterWebSocketHandlers() {
        // Arrange
        // Mock a WebSocketHandlerRegistry and the chain of calls
        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
        WebSocketHandlerRegistration registration = mock(WebSocketHandlerRegistration.class);
        // 让 registry.addHandler(...) 返回我们 mock 的 registration 对象，以支持链式调用 .setAllowedOrigins()
        when(registry.addHandler(any(), anyString())).thenReturn(registration);

        // ArgumentCaptor 用于捕获传递给方法的参数
        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        webSocketConfig.registerWebSocketHandlers(registry);

        // Assert
        // 验证 addHandler 被调用了两次
        verify(registry, times(2)).addHandler(any(), pathCaptor.capture());

        // 验证 .setAllowedOrigins("*") 也被调用了两次
        verify(registration, times(2)).setAllowedOrigins("*");

        // 验证注册的路径是否正确
        assertThat(pathCaptor.getAllValues()).containsExactlyInAnyOrder("/ws/result", "/ws/tracking");
    }

    @Test
    void testServerEndpointExporterBean() {
        // Arrange & Act
        ServerEndpointExporter exporter = webSocketConfig.serverEndpointExporter();

        // Assert
        // 验证 @Bean 方法返回了一个非空实例
        assertThat(exporter).isNotNull();
        assertThat(exporter).isInstanceOf(ServerEndpointExporter.class);
    }
}