package com.hometusk.mobile.api;

import com.hometusk.mobile.dto.MobileDeviceDto;
import com.hometusk.mobile.dto.RegisterMobileDeviceRequest;
import com.hometusk.mobile.dto.UpdateMobileDeviceRequest;
import com.hometusk.mobile.service.MobileDeviceService;
import com.hometusk.shared.security.CurrentUser;
import com.hometusk.users.domain.User;
import com.hometusk.users.service.UserResolver;
import com.hometusk.users.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mobile/devices")
public class MobileDeviceController {

    private final MobileDeviceService mobileDeviceService;
    private final UserResolver userResolver;
    private final UserService userService;

    public MobileDeviceController(
            MobileDeviceService mobileDeviceService, UserResolver userResolver, UserService userService) {
        this.mobileDeviceService = mobileDeviceService;
        this.userResolver = userResolver;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<MobileDeviceDto> register(@Valid @RequestBody RegisterMobileDeviceRequest request) {
        User user = currentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MobileDeviceDto.from(mobileDeviceService.register(user, request)));
    }

    @PatchMapping("/{deviceId}")
    public ResponseEntity<MobileDeviceDto> update(
            @PathVariable UUID deviceId, @Valid @RequestBody UpdateMobileDeviceRequest request) {
        User user = currentUser();
        return ResponseEntity.ok(MobileDeviceDto.from(mobileDeviceService.update(user.getId(), deviceId, request)));
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> delete(@PathVariable UUID deviceId) {
        User user = currentUser();
        mobileDeviceService.deactivate(user.getId(), deviceId);
        return ResponseEntity.noContent().build();
    }

    private User currentUser() {
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        return userService.getById(currentUser.id());
    }
}
