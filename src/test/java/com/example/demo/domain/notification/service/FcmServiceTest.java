package com.example.demo.domain.notification.service;

import com.example.demo.domain.userDevice.service.UserDevicePushService;
import com.example.demo.domain.userDevice.service.UserDevicePushService.PushTarget;
import com.google.firebase.messaging.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class FcmServiceTest {
    private final FirebaseMessaging messaging = mock(FirebaseMessaging.class);
    private final UserDevicePushService devices = mock(UserDevicePushService.class);
    private final FcmService service = new FcmService(messaging, devices);
    private final PushTarget first = new PushTarget(1L, "token-1", LocalDateTime.now());
    private final PushTarget second = new PushTarget(2L, "token-2", LocalDateTime.now());

    @Test
    void sendsTitleBodyAndNavigationDataToEveryDevice() throws Exception {
        when(devices.getTargets(10L)).thenReturn(List.of(first, second));
        var data = Map.of("notificationType", "RESULT", "sourceId", "123");
        assertThat(service.sendToUser(10L, "제목", "본문", data)).isEqualTo(new FcmSendResult(2, 0));
        var captor = ArgumentCaptor.forClass(Message.class);
        verify(messaging, times(2)).send(captor.capture());
        assertThat(captor.getAllValues()).extracting(m -> ReflectionTestUtils.getField(m, "token"))
                .containsExactly("token-1", "token-2");
        for (Message message : captor.getAllValues()) {
            assertThat(ReflectionTestUtils.getField(message, "data")).isEqualTo(data);
            Object notification = ReflectionTestUtils.getField(message, "notification");
            assertThat(ReflectionTestUtils.getField(notification, "title")).isEqualTo("제목");
            assertThat(ReflectionTestUtils.getField(notification, "body")).isEqualTo("본문");
        }
    }

    @Test
    void noRegisteredDevicesReturnsZeroWithoutCallingFirebase() {
        when(devices.getTargets(10L)).thenReturn(List.of());
        assertThat(service.sendToUser(10L, "제목", "본문", null)).isEqualTo(new FcmSendResult(0, 0));
        verifyNoInteractions(messaging);
    }

    @Test
    void unregisteredTokenIsCleanedAndNextDeviceStillReceivesMessage() throws Exception {
        when(devices.getTargets(10L)).thenReturn(List.of(first, second));
        var error = failure(MessagingErrorCode.UNREGISTERED);
        when(messaging.send(any(Message.class))).thenThrow(error).thenReturn("message-id");
        assertThat(service.sendToUser(10L, "제목", "본문", Map.of())).isEqualTo(new FcmSendResult(1, 1));
        verify(devices).removeExpiredToken(10L, first);
        verify(messaging, times(2)).send(any(Message.class));
    }

    @Test
    void temporaryAndPayloadAndCredentialErrorsDoNotDeleteTokens() throws Exception {
        for (MessagingErrorCode code : List.of(MessagingErrorCode.UNAVAILABLE,
                MessagingErrorCode.INVALID_ARGUMENT, MessagingErrorCode.THIRD_PARTY_AUTH_ERROR)) {
            reset(messaging, devices);
            when(devices.getTargets(10L)).thenReturn(List.of(first, second));
            var error = failure(code);
            when(messaging.send(any(Message.class))).thenThrow(error).thenReturn("message-id");
            assertThat(service.sendToUser(10L, "제목", "본문", Map.of())).isEqualTo(new FcmSendResult(1, 1));
            verify(devices, never()).removeExpiredToken(any(), any());
        }
    }

    @Test
    void cleanupFailureDoesNotStopNextDevice() throws Exception {
        when(devices.getTargets(10L)).thenReturn(List.of(first, second));
        var error = failure(MessagingErrorCode.UNREGISTERED);
        when(messaging.send(any(Message.class))).thenThrow(error).thenReturn("message-id");
        doThrow(new IllegalStateException("DB unavailable")).when(devices).removeExpiredToken(10L, first);
        assertThat(service.sendToUser(10L, "제목", "본문", Map.of())).isEqualTo(new FcmSendResult(1, 1));
        verify(messaging, times(2)).send(any(Message.class));
    }

    @Test
    void unexpectedFailureDoesNotStopNextDevice() throws Exception {
        when(devices.getTargets(10L)).thenReturn(List.of(first, second));
        when(messaging.send(any(Message.class))).thenThrow(new IllegalStateException()).thenReturn("message-id");
        assertThat(service.sendToUser(10L, "제목", "본문", Map.of())).isEqualTo(new FcmSendResult(1, 1));
        verify(devices, never()).removeExpiredToken(any(), any());
    }

    private FirebaseMessagingException failure(MessagingErrorCode code) {
        var exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(code);
        return exception;
    }
}
