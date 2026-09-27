package com.example.demo.api.userDevice;

import com.example.demo.api.userDevice.controller.UserDeviceController;
import com.example.demo.api.userDevice.service.UserDeviceUseCase;
import com.example.demo.common.annotation.AuthUser;
import com.example.demo.common.exception.ExceptionAdvice;
import com.example.demo.domain.user.entity.User;
import org.junit.jupiter.api.*;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.*;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class UserDeviceControllerTest {
    UserDeviceUseCase useCase;
    MockMvc mvc;
    User user = User.builder().id(42L).build();

    @BeforeEach
    void setUp() {
        useCase = mock(UserDeviceUseCase.class);
        mvc = MockMvcBuilders.standaloneSetup(new UserDeviceController(useCase))
                .setControllerAdvice(new ExceptionAdvice())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    public boolean supportsParameter(MethodParameter p) { return p.hasParameterAnnotation(AuthUser.class); }
                    public Object resolveArgument(MethodParameter p, ModelAndViewContainer c, NativeWebRequest r, WebDataBinderFactory f) { return user; }
                }).build();
    }

    @Test
    void registerUsesAuthenticatedUser() throws Exception {
        mvc.perform(patch("/api/v1/users/me/devices/install-1").contentType(APPLICATION_JSON)
                .content("{\"fcmToken\":\"token-1\",\"platform\":\"ANDROID\"}"))
                .andExpect(status().isOk());
        verify(useCase).register(eq(user), eq("install-1"), argThat(r -> r.fcmToken().equals("token-1")));
    }

    @Test
    void rejectsBlankTokenMissingPlatformAndInvalidPlatform() throws Exception {
        for (String body : new String[]{"{\"fcmToken\":\" \",\"platform\":\"IOS\"}",
                "{\"fcmToken\":\"token\"}", "{\"fcmToken\":\"token\",\"platform\":\"WEB\"}"}) {
            mvc.perform(patch("/api/v1/users/me/devices/install-1").contentType(APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(useCase);
    }

    @Test
    void rejectsInvalidInstallationId() throws Exception {
        mvc.perform(patch("/api/v1/users/me/devices/bad.id").contentType(APPLICATION_JSON)
                .content("{\"fcmToken\":\"token\",\"platform\":\"IOS\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(useCase);
    }

    @Test
    void deleteUsesAuthenticatedUser() throws Exception {
        mvc.perform(delete("/api/v1/users/me/devices/install-1")).andExpect(status().isOk());
        verify(useCase).unregister(42L, "install-1");
    }
}
