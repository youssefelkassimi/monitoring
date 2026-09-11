package com.elkassimi.monitoring_v2_0.service;

import com.elkassimi.monitoring_v2_0.dto.UserDto;
import com.elkassimi.monitoring_v2_0.model.User;
import com.elkassimi.monitoring_v2_0.repository.UserRepository;
import com.elkassimi.monitoring_v2_0.websocket.RealTimePushService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RealTimePushService pushService;

    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserDto register(UserDto userDto) throws Exception {
        log.info("Registering new user: email='{}', fullName='{}', role='{}'",
                userDto.email(), userDto.fullName(), userDto.role());

        if (userRepository.existsByEmail(userDto.email())){
            log.warn("Registration rejected: email already exists '{}'", userDto.email());
            throw new Exception("Email already exist");
        }
        User user = fromUserDto(userDto);
        User savedUser = userRepository.save(user);
        log.info("User registered: id={}, email='{}', role={}",
                savedUser.getId(), savedUser.getEmail(), savedUser.getRole());

        UserDto saved = UserDto.builder().
                id(savedUser.getId())
                .isOnline(false)
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .password("")
                .role(savedUser.getRole().name())
                .build();
        pushService.pushToAdmins(saved);
        log.debug("Pushed new user to admins: id={}", saved.id());
        return saved;
    }

    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void logout(String id) throws Exception {
        log.info("Logging out user id={}", id);
        User user = findOrThrow(id);
        user.setOnline(false);
        userRepository.save(user);
        log.info("User logged out: id={}, email='{}'", user.getId(), user.getEmail());
    }

    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserDto updateUser(UserDto userDto) throws Exception {
        log.info("Updating user id={}, email='{}', fullName='{}', role='{}'",
                userDto.id(), userDto.email(), userDto.fullName(), userDto.role());

        User dbUser = findOrThrow(userDto.id());
        if (!dbUser.getEmail().equalsIgnoreCase(userDto.email()) && userRepository.existsByEmail(userDto.email())){
            log.warn("Update rejected for user id={}: email already in use '{}'", userDto.id(), userDto.email());
            throw new Exception("Email already exist");
        }
        dbUser.setFullName(userDto.fullName());
        dbUser.setEmail(userDto.email());
        dbUser.setRole(User.role.valueOf(userDto.role()));
        User savedUser = userRepository.save(dbUser);
        log.info("User updated: id={}, email='{}', role={}",
                savedUser.getId(), savedUser.getEmail(), savedUser.getRole());

        UserDto saved = toUserDto(savedUser);
        pushService.pushToAdmins(saved);
        log.debug("Pushed updated user to admins: id={}", saved.id());
        return saved;
    }

    @Cacheable(value = "users", key = "'findAll'")
    public List<UserDto> findAll(){
        log.debug("Fetching all users");
        List<UserDto> users = userRepository.findAll()
                .stream().map(this::toUserDto)
                .toList();
        log.debug("Fetched {} users", users.size());
        return users;
    }

    // --- query methods ---

    @Cacheable(value = "users", key = "'id:' + #id")
    public UserDto getById(String id) throws Exception {
        log.debug("Fetching user by id={}", id);
        return toUserDto(findOrThrow(id));
    }

    @Cacheable(value = "users", key = "'email:' + #email")
    public UserDto getByEmail(String email) throws Exception {
        log.debug("Fetching user by email='{}'", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User lookup failed: no user with email='{}'", email);
                    return new Exception("No user with email: %s".formatted(email));
                });
        return toUserDto(user);
    }

    @Cacheable(value = "users", key = "'role:' + #role")
    public List<UserDto> findByRole(User.role role) {
        log.debug("Fetching users by role={}", role);
        List<UserDto> users = userRepository.findByRole(role)
                .stream().map(this::toUserDto)
                .toList();
        log.debug("Fetched {} users with role={}", users.size(), role);
        return users;
    }

    @Cacheable(value = "users", key = "'online'")
    public List<UserDto> findOnlineUsers() {
        log.debug("Fetching online users");
        List<UserDto> users = userRepository.findByIsOnlineIsTrue()
                .stream().map(this::toUserDto)
                .toList();
        log.debug("Fetched {} online users", users.size());
        return users;
    }

    @Cacheable(value = "users", key = "'offline'")
    public List<UserDto> findOfflineUsers() {
        log.debug("Fetching offline users");
        List<UserDto> users = userRepository.findByIsOnlineIsFalse()
                .stream().map(this::toUserDto)
                .toList();
        log.debug("Fetched {} offline users", users.size());
        return users;
    }

    @Cacheable(value = "users", key = "'existsEmail:' + #email")
    public boolean existsByEmail(String email) {
        log.trace("Checking existence of user by email='{}'", email);
        boolean exists = userRepository.existsByEmail(email);
        log.debug("Email existence check for '{}': {}", email, exists);
        return exists;
    }

    @Cacheable(value = "users", key = "'existsId:' + #id")
    public boolean existsById(String id) {
        log.trace("Checking existence of user by id={}", id);
        boolean exists = userRepository.existsById(id);
        log.debug("User id existence check for '{}': {}", id, exists);
        return exists;
    }

    @Cacheable(value = "users", key = "'count:all'")
    public long userCount() {
        log.trace("Counting all users");
        long count = userRepository.count();
        log.debug("Total user count={}", count);
        return count;
    }

    @Cacheable(value = "users", key = "'count:online'")
    public long onlineUserCount() {
        log.trace("Counting online users");
        long count = userRepository.countByIsOnlineIsTrue();
        log.debug("Online user count={}", count);
        return count;
    }

    @Cacheable(value = "users", key = "'count:offline'")
    public long offlineUserCount() {
        log.trace("Counting offline users");
        long count = userRepository.countByIsOnlineIsFalse();
        log.debug("Offline user count={}", count);
        return count;
    }

    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserDto setOnlineStatus(String id, boolean online) throws Exception {
        log.info("Setting user online status: id={}, online={}", id, online);
        User user = findOrThrow(id);
        user.setOnline(online);
        User savedUser = userRepository.save(user);
        UserDto saved = toUserDto(savedUser);
        pushService.pushToAdmins(saved);
        log.debug("Pushed user online status update to admins: id={}", saved.id());
        return saved;
    }


    private User findOrThrow(String id) throws Exception {
        log.trace("Looking up user by id={}", id);
        return userRepository.findById(id)
                .orElseThrow(()-> {
                    log.warn("User lookup failed: no user with id={}", id);
                    return new Exception("User with id: %s already exist".formatted(id));
                });
    }



    private User fromUserDto(UserDto userDto){
        log.trace("Mapping UserDto to entity: email='{}', fullName='{}', role='{}'",
                userDto.email(), userDto.fullName(), userDto.role());
        return  User.builder()
                .fullName(userDto.fullName())
                .email(userDto.email())
                .role(User.role.valueOf(userDto.role()))
                .isOnline(false)
                .build();
    }

    private UserDto toUserDto(User user){
        log.trace("Mapping User entity to DTO: id={}, email='{}'", user.getId(), user.getEmail());
        return  UserDto.builder().
                id(user.getId())
                .isOnline(user.isOnline())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .password("")
                .role(user.getRole().name())
                .build();
    }


}