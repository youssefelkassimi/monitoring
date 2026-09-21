package com.elkassimi.monitoring_v2_0.controller;

import com.elkassimi.monitoring_v2_0.config.GlobalExceptionHandler;
import com.elkassimi.monitoring_v2_0.dto.UserDto;
import com.elkassimi.monitoring_v2_0.model.User;
import com.elkassimi.monitoring_v2_0.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listUsers_withoutFilters_returnsAllUsers() throws Exception {
        when(userService.findAll()).thenReturn(List.of(userDto("u-1", "ADMIN", true)));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("u-1"));
    }

    @Test
    void listUsers_withRoleFilter_returnsUsersByRole() throws Exception {
        when(userService.findByRole(User.role.VIEWER)).thenReturn(List.of(userDto("u-2", "VIEWER", false)));

        mockMvc.perform(get("/api/users").param("role", "viewer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].role").value("VIEWER"));
    }

    @Test
    void listUsers_withOnlineFilter_returnsOnlineUsers() throws Exception {
        when(userService.findOnlineUsers()).thenReturn(List.of(userDto("u-3", "ADMIN", true)));

        mockMvc.perform(get("/api/users").param("online", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].isOnline").value(true));
    }

    @Test
    void getUser_returnsUserById() throws Exception {
        when(userService.getById("u-4")).thenReturn(userDto("u-4", "AGENT", false));

        mockMvc.perform(get("/api/users/u-4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("u-4"));
    }

    @Test
    void getUserByEmail_returnsUserByEmail() throws Exception {
        when(userService.getByEmail("alex@fleet.internal")).thenReturn(userDto("u-5", "VIEWER", false));

        mockMvc.perform(get("/api/users/by-email").param("email", "alex@fleet.internal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("u-5"));
    }

    @Test
    void existsByEmail_returnsBoolean() throws Exception {
        when(userService.existsByEmail("alex@fleet.internal")).thenReturn(true);

        mockMvc.perform(get("/api/users/exists").param("email", "alex@fleet.internal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void userCounts_returnsAllCounts() throws Exception {
        when(userService.userCount()).thenReturn(3L);
        when(userService.onlineUserCount()).thenReturn(1L);
        when(userService.offlineUserCount()).thenReturn(2L);

        mockMvc.perform(get("/api/users/counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.online").value(1))
                .andExpect(jsonPath("$.data.offline").value(2));
    }

    @Test
    void register_returnsCreatedUser() throws Exception {
        when(userService.register(org.mockito.ArgumentMatchers.any(UserDto.class)))
                .thenReturn(userDto("u-6", "ADMIN", false));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Alex Chen","email":"alex@fleet.internal","password":"secret","role":"ADMIN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("u-6"));
    }

    @Test
    void updateUser_usesPathId() throws Exception {
        when(userService.updateUser(org.mockito.ArgumentMatchers.any(UserDto.class)))
                .thenReturn(userDto("u-7", "VIEWER", false));

        mockMvc.perform(put("/api/users/u-7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":"ignored","fullName":"Alex Chen","email":"alex@fleet.internal","role":"VIEWER"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("u-7"));

        ArgumentCaptor<UserDto> captor = ArgumentCaptor.forClass(UserDto.class);
        verify(userService).updateUser(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo("u-7");
    }

    @Test
    void setOnlineStatus_returnsUpdatedUser() throws Exception {
        when(userService.setOnlineStatus("u-8", true)).thenReturn(userDto("u-8", "ADMIN", true));

        mockMvc.perform(patch("/api/users/u-8/online").param("online", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isOnline").value(true));
    }

    @Test
    void logout_returnsOk() throws Exception {
        mockMvc.perform(post("/api/users/u-9/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        verify(userService).logout("u-9");
    }

    private UserDto userDto(String id, String role, boolean online) {
        return UserDto.builder()
                .id(id)
                .fullName("Alex Chen")
                .email("alex@fleet.internal")
                .password("")
                .isOnline(online)
                .role(role)
                .build();
    }
}
