package com.ucd.urbanflow.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TraCIEventDispatcher 的单元测试 (高覆盖率版本)
 */
@ExtendWith(MockitoExtension.class)
class TraCIEventDispatcherTest {

    @Mock
    private WebSocketClient mockClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TraCIEventDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        // 为那些不测试 init() 方法的用例，预先注入一个 mockClient
        ReflectionTestUtils.setField(dispatcher, "client", mockClient);
    }

    // --- 已有的 sendEvent 测试用例保持不变 ---
    @Test
    void testSendEvent_WhenConnected_ShouldSendSuccessfully() throws Exception {
        ReflectionTestUtils.setField(dispatcher, "connected", true);
        when(mockClient.isOpen()).thenReturn(true);
        Object event = new Object() { public final String name = "testEvent"; };
        String expectedJson = objectMapper.writeValueAsString(event);

        boolean result = dispatcher.sendEvent(event);

        assertTrue(result);
        verify(mockClient).send(expectedJson);
    }

    @Test
    void testSendEvent_WhenNotConnected_ShouldFail() {
        ReflectionTestUtils.setField(dispatcher, "connected", false);
        boolean result = dispatcher.sendEvent(new Object());
        assertFalse(result);
        verify(mockClient, never()).send(anyString());
    }

    @Test
    void testSendEvent_WhenClientNotOpen_ShouldFail() {
        ReflectionTestUtils.setField(dispatcher, "connected", true);
        when(mockClient.isOpen()).thenReturn(false);
        boolean result = dispatcher.sendEvent(new Object());
        assertFalse(result);
        verify(mockClient, never()).send(anyString());
    }

    @Test
    void testSendEvent_CatchesSerializationException() throws JsonProcessingException {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        ReflectionTestUtils.setField(dispatcher, "objectMapper", mockMapper);
        ReflectionTestUtils.setField(dispatcher, "connected", true);
        when(mockClient.isOpen()).thenReturn(true);
        when(mockMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Test Exception") {});

        boolean result = dispatcher.sendEvent(new Object());

        assertFalse(result);
    }

    @Test
    void testIsConnected_VariousScenarios() {
        ReflectionTestUtils.setField(dispatcher, "connected", true);
        when(mockClient.isOpen()).thenReturn(true);
        assertTrue(dispatcher.isConnected(), "应该返回 true 当两者都为 true 时");

        ReflectionTestUtils.setField(dispatcher, "connected", false);
        when(mockClient.isOpen()).thenReturn(true);
        assertFalse(dispatcher.isConnected(), "应该返回 false 当 connected 为 false 时");

        ReflectionTestUtils.setField(dispatcher, "connected", true);
        when(mockClient.isOpen()).thenReturn(false);
        assertFalse(dispatcher.isConnected(), "应该返回 false 当 isOpen 为 false 时");
    }

    @Test
    void testInit_And_AllCallbacks() {
        // Act: 调用 init 方法。它会创建一个真实的 WebSocketClient 实例。
        dispatcher.init();

        // Assert: 验证客户端实例被创建
        Object clientObject = ReflectionTestUtils.getField(dispatcher, "client");
        assertNotNull(clientObject, "Client 实例应该在 init 后被创建");
        assertTrue(clientObject instanceof WebSocketClient, "创建的对象应该是 WebSocketClient 类型");
        WebSocketClient clientInstance = (WebSocketClient) clientObject;

        // --- 现在，手动触发回调方法来测试它们的逻辑 ---

        // 1. 测试 onOpen
        ServerHandshake mockHandshake = mock(ServerHandshake.class);
        clientInstance.onOpen(mockHandshake);
        assertTrue((Boolean) ReflectionTestUtils.getField(dispatcher, "connected"), "onOpen 应该将 connected 状态设为 true");

        // 2. 测试 onClose
        clientInstance.onClose(1000, "Normal closure", true);
        assertFalse((Boolean) ReflectionTestUtils.getField(dispatcher, "connected"), "onClose 应该将 connected 状态设为 false");

        // 3. 测试 onError
        clientInstance.onOpen(mockHandshake); // 先重新打开以测试 onError 的效果
        assertTrue((Boolean) ReflectionTestUtils.getField(dispatcher, "connected"), "在测试 onError 前，状态应为 true");
        clientInstance.onError(new Exception("Test connection error"));
        assertFalse((Boolean) ReflectionTestUtils.getField(dispatcher, "connected"), "onError 应该将 connected 状态设为 false");

        // 4. 测试 onMessage
        assertDoesNotThrow(() -> clientInstance.onMessage("Test message from TraCI"),
                "onMessage 应该只记录日志，不抛出异常");
    }

    /*
     * **变更点**: 移除了 testInit_CatchesException 测试用例。
     * 理由: 在不修改 TraCIEventDispatcher 源代码的情况下，无法通过外部测试来可靠地触发
     * init() 方法内部的 catch 块。保留一个基于错误假设的测试是没有意义的。
     */
}