package com.elkassimi.monitoring_v2_0.service;

import com.elkassimi.monitoring_v2_0.dto.UserDto;
import com.elkassimi.monitoring_v2_0.model.User;
import com.elkassimi.monitoring_v2_0.repository.UserRepository;
import com.elkassimi.monitoring_v2_0.websocket.RealTimePushService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RealTimePushService pushService;

    @InjectMocks
    private UserService userService;

    private UserDto newUserDto(String id, String email, String role) {
        return UserDto.builder()
                .id(id)
                .fullName("Alex Chen")
                .email(email)
                .password("temp-pass")
                .isOnline(false)
                .role(role)
                .build();
    }

    // --- register ---

    @Test
    void register_newEmail_savesAndPushesToAdminsAndClearsPassword() throws Exception {
        when(userRepository.existsByEmail("alex@fleet.internal")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("u-1");
            return u;
        });

        UserDto result = userService.register(newUserDto(null, "alex@fleet.internal", "ADMIN"));

        assertThat(result.id()).isEqualTo("u-1");
        assertThat(result.email()).isEqualTo("alex@fleet.internal");
        assertThat(result.password()).isEmpty();
        assertThat(result.isOnline()).isFalse();
        verify(pushService).pushToAdmins(result);
    }

    @Test
    void register_duplicateEmail_throwsAndDoesNotSave() {
        when(userRepository.existsByEmail("dup@fleet.internal")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(newUserDto(null, "dup@fleet.internal", "VIEWER")))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("already exist");

        verify(userRepository, never()).save(any());
    }

    // --- logout ---

    @Test
    void logout_setsOnlineFalseAndSaves() throws Exception {
        User user = User.builder().id("u-2").email("e@x.com").fullName("E").isOnline(true).build();
        when(userRepository.findById("u-2")).thenReturn(Optional.of(user));

        userService.logout("u-2");

        assertThat(user.isOnline()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void logout_unknownId_throws() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.logout("missing")).isInstanceOf(Exception.class);
    }

    // --- updateUser ---

    @Test
    void updateUser_sameEmail_updatesInPlacePreservingId() throws Exception {
        User existing = User.builder()
                .id("u-3").email("marcus@infra.internal").fullName("Marcus V")
                .role(User.role.VIEWER).isOnline(true).build();
        when(userRepository.findById("u-3")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto dto = newUserDto("u-3", "marcus@infra.internal", "ADMIN");
        UserDto result = userService.updateUser(dto);

        assertThat(result.id()).isEqualTo("u-3");
        assertThat(result.role()).isEqualTo("ADMIN");
        // isOnline is preserved from the persisted entity, not overwritten
        // by the incoming DTO's default - confirms the fix that stopped
        // updateUser from re-inserting a brand new row.
        assertThat(result.isOnline()).isTrue();
        verify(pushService).pushToAdmins(result);
    }

    @Test
    void updateUser_emailChangedToFreeEmail_succeeds() throws Exception {
        User existing = User.builder().id("u-4").email("old@x.com").fullName("Old Name")
                .role(User.role.VIEWER).build();
        when(userRepository.findById("u-4")).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("new@x.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = userService.updateUser(newUserDto("u-4", "new@x.com", "VIEWER"));

        assertThat(result.email()).isEqualTo("new@x.com");
    }

    @Test
    void updateUser_emailChangedToTakenEmail_throws() {
        User existing = User.builder().id("u-5").email("old@x.com").fullName("Old Name")
                .role(User.role.VIEWER).build();
        when(userRepository.findById("u-5")).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("taken@x.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUser(newUserDto("u-5", "taken@x.com", "VIEWER")))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("already exist");
    }

    @Test
    void updateUser_unknownId_throws() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(newUserDto("missing", "e@x.com", "VIEWER")))
                .isInstanceOf(Exception.class);
    }

    // --- reads ---

    @Test
    void findAll_mapsAllUsersToDtos() {
        User u = User.builder().id("u-6").email("e@x.com").fullName("E").role(User.role.VIEWER).build();
        when(userRepository.findAll()).thenReturn(List.of(u));

        List<UserDto> result = userService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("u-6");
        assertThat(result.get(0).password()).isEmpty();
    }

    @Test
    void getById_found_returnsDto() throws Exception {
        User u = User.builder().id("u-7").email("e@x.com").fullName("E").role(User.role.ADMIN).build();
        when(userRepository.findById("u-7")).thenReturn(Optional.of(u));

        assertThat(userService.getById("u-7").email()).isEqualTo("e@x.com");
    }

    @Test
    void getById_notFound_throws() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById("missing")).isInstanceOf(Exception.class);
    }

    @Test
    void getByEmail_found_returnsDto() throws Exception {
        User u = User.builder().id("u-8").email("found@x.com").fullName("F").role(User.role.VIEWER).build();
        when(userRepository.findByEmail("found@x.com")).thenReturn(Optional.of(u));

        assertThat(userService.getByEmail("found@x.com").id()).isEqualTo("u-8");
    }

    @Test
    void getByEmail_notFound_throws() {
        when(userRepository.findByEmail("missing@x.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByEmail("missing@x.com")).isInstanceOf(Exception.class);
    }

    @Test
    void findByRole_delegatesToRepositoryAndMapsToDtos() {
        User admin = User.builder().id("u-9").email("a@x.com").fullName("A").role(User.role.ADMIN).build();
        when(userRepository.findByRole(User.role.ADMIN)).thenReturn(List.of(admin));

        List<UserDto> result = userService.findByRole(User.role.ADMIN);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).role()).isEqualTo("ADMIN");
    }

    @Test
    void findOnlineUsers_delegatesToRepositoryAndMapsToDtos() {
        User online = User.builder().id("u-10").email("o@x.com").fullName("O")
                .role(User.role.VIEWER).isOnline(true).build();
        when(userRepository.findByIsOnlineIsTrue()).thenReturn(List.of(online));

        List<UserDto> result = userService.findOnlineUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isOnline()).isTrue();
    }

    @Test
    void findOfflineUsers_delegatesToRepositoryAndMapsToDtos() {
        User offline = User.builder().id("u-11").email("offline@x.com").fullName("Offline")
                .role(User.role.VIEWER).isOnline(false).build();
        when(userRepository.findByIsOnlineIsFalse()).thenReturn(List.of(offline));

        List<UserDto> result = userService.findOfflineUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isOnline()).isFalse();
    }

    @Test
    void existsByEmail_delegatesToRepository() {
        when(userRepository.existsByEmail("e@x.com")).thenReturn(true);

        assertThat(userService.existsByEmail("e@x.com")).isTrue();
    }

    @Test
    void existsById_delegatesToRepository() {
        when(userRepository.existsById("u-12")).thenReturn(true);

        assertThat(userService.existsById("u-12")).isTrue();
    }

    @Test
    void userCount_delegatesToRepositoryCount() {
        when(userRepository.count()).thenReturn(12L);

        assertThat(userService.userCount()).isEqualTo(12L);
    }

    @Test
    void onlineUserCount_delegatesToRepositoryCount() {
        when(userRepository.countByIsOnlineIsTrue()).thenReturn(4L);

        assertThat(userService.onlineUserCount()).isEqualTo(4L);
    }

    @Test
    void offlineUserCount_delegatesToRepositoryCount() {
        when(userRepository.countByIsOnlineIsFalse()).thenReturn(8L);

        assertThat(userService.offlineUserCount()).isEqualTo(8L);
    }

    @Test
    void setOnlineStatus_updatesStatusSavesAndPushes() throws Exception {
        User user = User.builder().id("u-13").email("status@x.com").fullName("Status")
                .role(User.role.ADMIN).isOnline(false).build();
        when(userRepository.findById("u-13")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = userService.setOnlineStatus("u-13", true);

        assertThat(result.isOnline()).isTrue();
        assertThat(user.isOnline()).isTrue();
        verify(userRepository).save(user);
        verify(pushService).pushToAdmins(result);
    }

    @Test
    void setOnlineStatus_unknownId_throws() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.setOnlineStatus("missing", true))
                .isInstanceOf(Exception.class);
        verify(userRepository, never()).save(any());
    }
}
