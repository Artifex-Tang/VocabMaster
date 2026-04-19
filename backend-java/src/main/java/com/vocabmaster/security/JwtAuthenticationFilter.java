package com.vocabmaster.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocabmaster.common.constant.RedisKey;
import com.vocabmaster.common.exception.BizException;
import com.vocabmaster.common.result.ErrorCode;
import com.vocabmaster.common.result.R;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        // 检查 Token 黑名单（已登出）
        if (Boolean.TRUE.equals(redisTemplate.hasKey(RedisKey.TOKEN_BLACKLIST + token))) {
            writeError(response, ErrorCode.UNAUTHORIZED);
            return;
        }

        try {
            Claims claims = jwtUtil.parse(token);

            // 只接受 accessToken（type=access）
            if (!"access".equals(claims.get("type", String.class))) {
                writeError(response, ErrorCode.UNAUTHORIZED);
                return;
            }

            String uuid = claims.getSubject();
            Long userId = claims.get("uid", Long.class);
            String role = claims.get("role", String.class);

            UserContext.set(new UserContext.Current(userId, uuid, role));

            var auth = new UsernamePasswordAuthenticationToken(
                    uuid, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            chain.doFilter(request, response);
        } catch (BizException e) {
            writeError(response, e.getErrorCode());
        } finally {
            UserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private void writeError(HttpServletResponse response, ErrorCode ec) throws IOException {
        response.setStatus(ec == ErrorCode.TOKEN_EXPIRED
                ? HttpServletResponse.SC_UNAUTHORIZED : HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(ec)));
    }
}
