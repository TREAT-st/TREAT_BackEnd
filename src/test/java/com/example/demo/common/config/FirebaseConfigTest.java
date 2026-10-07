package com.example.demo.common.config;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class FirebaseConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(FirebaseConfig.class);

    @Test
    void disabledByDefaultWithoutReadingCredentials() {
        try (var credentials = mockStatic(GoogleCredentials.class)) {
            runner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(FirebaseApp.class);
                assertThat(context).doesNotHaveBean(FirebaseMessaging.class);
            });
            credentials.verifyNoInteractions();
        }
    }

    @Test
    void enabledCreatesBeansAndDeletesAppOnShutdown() {
        GoogleCredentials fakeCredentials = GoogleCredentials.create(
                new AccessToken("test-only-token", new Date(System.currentTimeMillis() + 3600000)));
        try (var credentials = mockStatic(GoogleCredentials.class)) {
            credentials.when(GoogleCredentials::getApplicationDefault).thenReturn(fakeCredentials);
            // Reopening a context verifies the default Firebase app was released at shutdown.
            for (int i = 0; i < 2; i++) {
                runner.withPropertyValues("firebase.enabled=true", "firebase.project-id=test-project")
                        .run(context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context).hasSingleBean(FirebaseApp.class);
                            assertThat(context).hasSingleBean(FirebaseMessaging.class);
                            assertThat(context.getBean(FirebaseApp.class).getOptions().getProjectId())
                                    .isEqualTo("test-project");
                        });
            }
        }
    }

    @Test
    void enabledWithoutProjectIdFailsAtStartup() {
        runner.withPropertyValues("firebase.enabled=true").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage(
                    "FIREBASE_PROJECT_ID must be set when Firebase is enabled");
        });
    }

    @Test
    void enabledWithUnreadableCredentialsFailsAtStartup() {
        try (var credentials = mockStatic(GoogleCredentials.class)) {
            credentials.when(GoogleCredentials::getApplicationDefault)
                    .thenThrow(new IOException("credentials unavailable"));
            runner.withPropertyValues("firebase.enabled=true", "firebase.project-id=test-project")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IOException.class);
                    });
        }
    }
}
