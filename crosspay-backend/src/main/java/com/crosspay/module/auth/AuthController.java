package com.crosspay.module.auth;

import com.crosspay.common.dto.ApiResponse;
import com.crosspay.module.admin.AdminService;
import com.crosspay.module.auth.dto.LoginRequest;
import com.crosspay.module.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AdminService adminService;

    public AuthController(AuthService authService, AdminService adminService) {
        this.authService = authService;
        this.adminService = adminService;
    }

    @PostMapping("/register")
    public ApiResponse<?> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResponse.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(authService.login(req));
    }

    /**
     * 管理员登录
     */
    @PostMapping("/admin/login")
    public ApiResponse<Map<String, String>> adminLogin(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        return ApiResponse.ok(adminService.adminLogin(username, password));
    }
}
