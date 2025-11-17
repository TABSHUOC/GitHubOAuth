package cn.edu.ccibe.githuboauth.controller;

import cn.edu.ccibe.githuboauth.dto.GithubUserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthMeController {

    /**
     * 前端 App.js 中 USER_INFO_URL = "/api/auth/me"
     * 用于获取当前登录的 GitHub 用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<GithubUserDto> me(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        GithubUserDto user = (GithubUserDto) session.getAttribute("GITHUB_USER");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(user);
    }

    /**
     * 退出登录：清除当前会话中的 GitHub 用户信息，并让前端可以重新发起授权
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }
}
