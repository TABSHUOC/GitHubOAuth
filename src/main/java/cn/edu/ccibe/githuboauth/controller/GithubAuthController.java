package cn.edu.ccibe.githuboauth.controller;

import cn.edu.ccibe.githuboauth.dto.GithubUserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/github")
public class GithubAuthController {

    @Value("${app.github.client-id}")
    private String clientId;

    @Value("${app.github.client-secret}")
    private String clientSecret;

    @Value("${app.github.callback-url}")
    private String callbackUrl;

    @Value("${frontend.url}")
    private String frontendUrl;

    private final WebClient webClient = WebClient.create("https://api.github.com");

    /**
     * 前端点击 GitHub 登录按钮后，会跳转到 /api/auth/github/login
     * 这里负责重定向到 GitHub 授权页
     */
    @GetMapping("/login")
    public void login(HttpServletResponse response) throws IOException {
        String githubAuthorizeUrl = UriComponentsBuilder
                .fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", callbackUrl)
                // 使用逗号分隔 scope，避免空格导致的 QUERY_PARAM 校验错误
                .queryParam("scope", "read:user,user:email")
                .build(true)
                .toUriString();

        response.sendRedirect(githubAuthorizeUrl);
    }

    /**
     * GitHub 授权成功后回调此接口，带上 code
     * 1. 用 code 换 access_token
     * 2. 用 access_token 访问 GitHub API 获取用户信息
     * 3. 将用户信息存入 session
     * 4. 重定向回前端页面
     */
    @GetMapping("/callback")
    public void callback(@RequestParam("code") String code,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {

        // 1. 用 code 换 access_token
        Map<String, String> tokenResp = WebClient.create("https://github.com")
                .post()
                .uri("/login/oauth/access_token")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(Map.of(
                        "client_id", clientId,
                        "client_secret", clientSecret,
                        "code", code,
                        "redirect_uri", callbackUrl
                ))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {})
                .block();

        if (tokenResp == null || !tokenResp.containsKey("access_token")) {
            response.sendRedirect(frontendUrl + "?error=github_oauth_failed");
            return;
        }

        String accessToken = tokenResp.get("access_token");

        // 2. 使用 access_token 获取 GitHub 用户信息
        Map<String, Object> userResp = webClient.get()
                .uri("/user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (userResp == null) {
            response.sendRedirect(frontendUrl + "?error=github_user_failed");
            return;
        }

        GithubUserDto userDto = new GithubUserDto();
        userDto.setName((String) userResp.get("name"));
        userDto.setLogin((String) userResp.get("login"));
        userDto.setAvatar_url((String) userResp.get("avatar_url"));
        userDto.setEmail((String) userResp.get("email"));

        // 3. 保存用户信息到 session
        HttpSession session = request.getSession(true);
        session.setAttribute("GITHUB_USER", userDto);

        // 4. 回到前端（你的 React 登录页），并附带一个标记参数，提示前端这是一次成功登录
        response.sendRedirect(frontendUrl + "?login=success");
    }
}
