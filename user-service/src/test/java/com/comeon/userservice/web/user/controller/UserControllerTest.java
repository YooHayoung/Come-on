package com.comeon.userservice.web.user.controller;

import com.comeon.userservice.common.argresolver.JwtArgumentResolver;
import com.comeon.userservice.domain.user.entity.Account;
import com.comeon.userservice.domain.user.entity.OAuthProvider;
import com.comeon.userservice.domain.user.entity.User;
import com.comeon.userservice.domain.user.repository.UserRepository;
import com.comeon.userservice.web.common.exception.resolver.CommonControllerAdvice;
import com.comeon.userservice.web.user.request.UserModifyRequest;
import com.comeon.userservice.web.user.response.UserWithdrawResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import javax.servlet.ServletException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@Transactional
@SpringBootTest
class UserControllerTest {

    @Autowired
    UserController userController;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ObjectMapper objectMapper;

    MockMvc mockMvc;

    @BeforeEach
    void initMockMvc(final WebApplicationContext context) throws ServletException {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .setControllerAdvice(new CommonControllerAdvice())
                .setCustomArgumentResolvers(new JwtArgumentResolver(objectMapper))
                .build();
    }

    @Nested
    @DisplayName("회원 등록")
    class userSave {

        String oauthId;
        String provider;
        String email;
        String name;
        String profileImgUrl;

        Map<String, Object> generateRequestBody() {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("oauthId", oauthId);
            request.put("provider", provider);
            request.put("name", name);
            request.put("email", email);
            request.put("profileImgUrl", profileImgUrl);
            return request;
        }

        @Test
        @DisplayName("success - 요청 데이터 검증에 성공하면 회원 정보를 저장하고 userId와 role을 반환한다.")
        void userSave_success_1() throws Exception {
            // given
            oauthId = "12345";
            provider = "kakao".toUpperCase();
            name = "testName1";
            email = "email1@email.com";
            profileImgUrl = "profileImgUrl";

            // when
            Map<String, Object> requestBody = generateRequestBody();
            ResultActions perform = mockMvc.perform(
                    post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestBody))
            );

            User user = userRepository.findByOAuthIdAndProvider(
                    oauthId,
                    OAuthProvider.valueOf(provider)
            ).orElseThrow();

            // then
            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").exists())
                    .andExpect(jsonPath("$.data.userId").isNotEmpty())
                    .andExpect(jsonPath("$.data.userId").value(user.getId()))
                    .andExpect(jsonPath("$.data.role").exists())
                    .andExpect(jsonPath("$.data.role").isNotEmpty())
                    .andExpect(jsonPath("$.data.role").value(user.getRole().getRoleValue()));
        }

        @Test
        @DisplayName("success - profileImgUrl은 선택값으로 null을 허용한다.")
        void userSave_success_2() throws Exception {
            // given
            oauthId = "12345";
            provider = "kakao".toUpperCase();
            name = "testName1";
            email = "email1@email.com";

            // when
            Map<String, Object> requestBody = generateRequestBody();

            ResultActions perform = mockMvc.perform(
                    post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestBody))
            );

            User user = userRepository.findByOAuthIdAndProvider(
                    oauthId,
                    OAuthProvider.valueOf(provider)
            ).orElseThrow();

            // then
            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").exists())
                    .andExpect(jsonPath("$.data.userId").isNotEmpty())
                    .andExpect(jsonPath("$.data.userId").value(user.getId()))
                    .andExpect(jsonPath("$.data.role").exists())
                    .andExpect(jsonPath("$.data.role").isNotEmpty())
                    .andExpect(jsonPath("$.data.role").value(user.getRole().getRoleValue()));
        }

        @Test
        @DisplayName("fail - email이 유효한 형식이 아니면 검증이 실패하여 http status 400 오류를 반환한다.")
        void userSave_fail_1() throws Exception {
            // given
            oauthId = "12345";
            provider = "kakao".toUpperCase();
            name = "testName1";
            email = "email1";

            // when
            Map<String, Object> requestBody = generateRequestBody();

            ResultActions perform = mockMvc.perform(
                    post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestBody))
            );

            perform.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("fail - 등록되지 않은 Provider가 요청으로 넘어오면, 검증이 실패하고 http status 400 오류를 반환한다.")
        void userSave_fail_2() throws Exception {
            oauthId = "12345";
            provider = "daum".toUpperCase();
            name = "testName1";
            email = "email1";

            Map<String, Object> requestBody = generateRequestBody();

            ResultActions perform = mockMvc.perform(
                    post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestBody))
            );

            perform.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("fail - oauthId, provider, email, name은 필수값으로, 입력하지 않으면 http status 400 오류를 반환한다.")
        void userSave_fail_3() throws Exception {
            Map<String, Object> requestBody = generateRequestBody();

            ResultActions perform = mockMvc.perform(
                    post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestBody))
            );

            perform.andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("유저 정보 조회")
    class userDetails {

        @Test
        @DisplayName("success - 존재하는 유저를 검색하면 해당 유저의 id, nickname, profileImgUrl 정보를 출력한다.")
        void userDetailSuccess() throws Exception {
            // given
            String profileImgUrl = "profileImgUrl1";
            String name = "name1";
            String email = "email1@email.com";
            OAuthProvider provider = OAuthProvider.KAKAO;
            String oauthId = "123123";

            User user = User.builder()
                    .account(
                            Account.builder()
                                    .oauthId(oauthId)
                                    .provider(provider)
                                    .email(email)
                                    .name(name)
                                    .profileImgUrl(profileImgUrl)
                                    .build()
                    )
                    .build();
            userRepository.save(user);

            Long userId = user.getId();

            // when
            ResultActions perform = mockMvc.perform(
                    get("/users/" + userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            // then
            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").exists())
                    .andExpect(jsonPath("$.data.userId").isNotEmpty())
                    .andExpect(jsonPath("$.data.userId").value(user.getId()))
                    .andExpect(jsonPath("$.data.nickname").exists())
                    .andExpect(jsonPath("$.data.nickname").isNotEmpty())
                    .andExpect(jsonPath("$.data.nickname").value(user.getNickname()))
                    .andExpect(jsonPath("$.data.profileImgUrl").isEmpty())
                    .andExpect(jsonPath("$.data.profileImgUrl").doesNotExist())
//                    .andExpect(jsonPath("$.data.profileImgUrl").value(user.getProfileImgUrl()))
                    // email과 name 필드는 없다.
                    .andExpect(jsonPath("$.data.email").doesNotExist())
                    .andExpect(jsonPath("$.data.name").doesNotExist());
        }

        @Test
        @DisplayName("success - 유저가 profileImgUrl 정보를 갖고있지 않으면, profileImgUrl 필드는 null 일 수 있다.")
        void userDetailSuccessNoProfileImgUrl() throws Exception {
            // given
            String profileImgUrl = null;
            String name = "name1";
            String email = "email1@email.com";
            OAuthProvider provider = OAuthProvider.KAKAO;
            String oauthId = "123123";

            User user = User.builder()
                    .account(
                            Account.builder()
                                    .oauthId(oauthId)
                                    .provider(provider)
                                    .email(email)
                                    .name(name)
                                    .profileImgUrl(profileImgUrl)
                                    .build()
                    )
                    .build();
            userRepository.save(user);

            Long userId = user.getId();

            // when
            ResultActions perform = mockMvc.perform(
                    get("/users/" + userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            // then
            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").exists())
                    .andExpect(jsonPath("$.data.userId").isNotEmpty())
                    .andExpect(jsonPath("$.data.userId").value(userId))
                    .andExpect(jsonPath("$.data.nickname").exists())
                    .andExpect(jsonPath("$.data.nickname").isNotEmpty())
                    .andExpect(jsonPath("$.data.nickname").value(user.getNickname()))
                    .andExpect(jsonPath("$.data.profileImgUrl").isEmpty())
                    // email과 name 필드는 없다.
                    .andExpect(jsonPath("$.data.email").doesNotExist())
                    .andExpect(jsonPath("$.data.name").doesNotExist());
        }

        @Test
        @DisplayName("fail - 존재하지 않는 유저를 검색하면 요청이 실패하고 http status 400 반환한다.")
        void userProfileSuccessNoProfileImgUrl() throws Exception {
            Long userId = 100L;

            ResultActions perform = mockMvc.perform(
                    get("/users/" + userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
            );

            perform.andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("내 상세정보 조회")
    class myDetails {

        String jwtSecretKey = "8490783c21034fd55f9cde06d539607f326356fa9732d93db12263dc4ce906a02ab20311228a664522bf7ed3ff66f0b3694e94513bdfa17bc631e57030c248ed";

        @Test
        @DisplayName("success - AccessToken을 통해 현재 유저의 상세정보를 조회한다. id, nickname, profileImgUrl, email, name 정보를 반환한다.")
        void success() throws Exception {
            String profileImgUrl = "profileImgUrl1";
            String name = "name1";
            String email = "email1@email.com";
            OAuthProvider provider = OAuthProvider.KAKAO;
            String oauthId = "123123";

            User user = User.builder()
                    .account(
                            Account.builder()
                                    .oauthId(oauthId)
                                    .provider(provider)
                                    .email(email)
                                    .name(name)
                                    .profileImgUrl(profileImgUrl)
                                    .build()
                    )
                    .build();
            userRepository.save(user);

            String accessToken = Jwts.builder()
                    .signWith(Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS512)
                    .claim("auth", user.getRole().getRoleValue())
                    .setIssuer("test")
                    .setIssuedAt(Date.from(Instant.now()))
                    .setExpiration(Date.from(Instant.now().plusSeconds(100)))
                    .setSubject(user.getId().toString())
                    .compact();

            ResultActions perform = mockMvc.perform(
                    get("/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            );

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").exists())
                    .andExpect(jsonPath("$.data.userId").isNotEmpty())
                    .andExpect(jsonPath("$.data.userId").value(user.getId()))
                    .andExpect(jsonPath("$.data.nickname").exists())
                    .andExpect(jsonPath("$.data.nickname").isNotEmpty())
                    .andExpect(jsonPath("$.data.nickname").value(user.getNickname()))
                    .andExpect(jsonPath("$.data.profileImgUrl").doesNotExist())
                    .andExpect(jsonPath("$.data.profileImgUrl").isEmpty())
//                    .andExpect(jsonPath("$.data.profileImgUrl").value(user.getProfileImgUrl()))
                    .andExpect(jsonPath("$.data.email").exists())
                    .andExpect(jsonPath("$.data.email").isNotEmpty())
                    .andExpect(jsonPath("$.data.email").value(user.getAccount().getEmail()))
                    .andExpect(jsonPath("$.data.name").exists())
                    .andExpect(jsonPath("$.data.name").isNotEmpty())
                    .andExpect(jsonPath("$.data.name").value(user.getAccount().getName()));
        }

        @Test
        @DisplayName("success - AccessToken을 통해 현재 유저의 상세정보를 조회한다. profileImgUrl은 null일 수 있다.")
        void success2() throws Exception {
            String profileImgUrl = null;
            String name = "name1";
            String email = "email1@email.com";
            OAuthProvider provider = OAuthProvider.KAKAO;
            String oauthId = "123123";

            User user = User.builder()
                    .account(
                            Account.builder()
                                    .oauthId(oauthId)
                                    .provider(provider)
                                    .email(email)
                                    .name(name)
                                    .profileImgUrl(profileImgUrl)
                                    .build()
                    )
                    .build();
            userRepository.save(user);

            String accessToken = Jwts.builder()
                    .signWith(Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS512)
                    .claim("auth", user.getRole().getRoleValue())
                    .setIssuer("test")
                    .setIssuedAt(Date.from(Instant.now()))
                    .setExpiration(Date.from(Instant.now().plusSeconds(100)))
                    .setSubject(user.getId().toString())
                    .compact();

            ResultActions perform = mockMvc.perform(
                    get("/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            );

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").exists())
                    .andExpect(jsonPath("$.data.userId").isNotEmpty())
                    .andExpect(jsonPath("$.data.userId").value(user.getId()))
                    .andExpect(jsonPath("$.data.nickname").exists())
                    .andExpect(jsonPath("$.data.nickname").isNotEmpty())
                    .andExpect(jsonPath("$.data.nickname").value(user.getNickname()))
                    .andExpect(jsonPath("$.data.profileImgUrl").doesNotExist())
                    .andExpect(jsonPath("$.data.profileImgUrl").isEmpty())
                    .andExpect(jsonPath("$.data.email").exists())
                    .andExpect(jsonPath("$.data.email").isNotEmpty())
                    .andExpect(jsonPath("$.data.email").value(user.getAccount().getEmail()))
                    .andExpect(jsonPath("$.data.name").exists())
                    .andExpect(jsonPath("$.data.name").isNotEmpty())
                    .andExpect(jsonPath("$.data.name").value(user.getAccount().getName()));
        }
        // Token 검증에 실패하면 API Gateway에서 걸러지는데.. 실패할 케이스의 경우를 작성할 필요가 있을까..
    }

    @Nested
    @DisplayName("회원 탈퇴")
    class userWithdraw {

        String jwtSecretKey = "8490783c21034fd55f9cde06d539607f326356fa9732d93db12263dc4ce906a02ab20311228a664522bf7ed3ff66f0b3694e94513bdfa17bc631e57030c248ed";
        String successMessage = UserWithdrawResponse.SUCCESS_MESSAGE;

        @Test
        @DisplayName("succss - 회원 탈퇴 요청을 성공적으로 처리하면, 요청 성공 처리 메시지를 반환한다.")
        void success() throws Exception {
            // given
            String profileImgUrl = null;
            String name = "name1";
            String email = "email1@email.com";
            OAuthProvider provider = OAuthProvider.KAKAO;
            String oauthId = "123123";

            User user = User.builder()
                    .account(
                            Account.builder()
                                    .oauthId(oauthId)
                                    .provider(provider)
                                    .email(email)
                                    .name(name)
                                    .profileImgUrl(profileImgUrl)
                                    .build()
                    )
                    .build();
            userRepository.save(user);

            String accessToken = Jwts.builder()
                    .signWith(Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS512)
                    .claim("auth", user.getRole().getRoleValue())
                    .setIssuer("test")
                    .setIssuedAt(Date.from(Instant.now()))
                    .setExpiration(Date.from(Instant.now().plusSeconds(100)))
                    .setSubject(user.getId().toString())
                    .compact();

            // when
            ResultActions perform = mockMvc.perform(
                    delete("/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            );

            // then
            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.message").value(successMessage));
        }

        @Test
        @DisplayName("succss - 회원 탈퇴 요청을 성공적으로 처리하면, 해당 회원을 더 이상 조회할 수 없다. http staus 400 반환한다.")
        void afterSuccess() throws Exception {
            // given
            String profileImgUrl = null;
            String name = "name1";
            String email = "email1@email.com";
            OAuthProvider provider = OAuthProvider.KAKAO;
            String oauthId = "123123";

            User user = User.builder()
                    .account(
                            Account.builder()
                                    .oauthId(oauthId)
                                    .provider(provider)
                                    .email(email)
                                    .name(name)
                                    .profileImgUrl(profileImgUrl)
                                    .build()
                    )
                    .build();
            userRepository.save(user);

            Long userId = user.getId();
            String accessToken = Jwts.builder()
                    .signWith(Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS512)
                    .claim("auth", user.getRole().getRoleValue())
                    .setIssuer("test")
                    .setIssuedAt(Date.from(Instant.now()))
                    .setExpiration(Date.from(Instant.now().plusSeconds(100)))
                    .setSubject(userId.toString())
                    .compact();

            // when
            mockMvc.perform(
                    delete("/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            ).andExpect(status().isOk());

            // then
            mockMvc.perform(
                    get("/users/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
            ).andExpect(status().isBadRequest());

            // 기존 AccessToken 은 사용 불가 처리되어 기존 AccessToken이 넘어올 일은 없다만..
            mockMvc.perform(
                    get("/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            ).andExpect(status().isBadRequest());

            mockMvc.perform(
                    delete("/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            ).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("유저 정보 수정")
    class userModify {

        String jwtSecretKey = "8490783c21034fd55f9cde06d539607f326356fa9732d93db12263dc4ce906a02ab20311228a664522bf7ed3ff66f0b3694e94513bdfa17bc631e57030c248ed";

        @Test
        @DisplayName("success - 유저 닉네임 변경에 성공하면 http status 200 반환한다.")
        void success() throws Exception {
            // given
            String profileImgUrl = "profileImgUrl";
            String oauthId = "oauthId";
            OAuthProvider provider = OAuthProvider.KAKAO;
            String name = "testName";
            String email = "testEmail";

            User user = User.builder()
                    .account(
                            Account.builder()
                                    .oauthId(oauthId)
                                    .provider(provider)
                                    .email(email)
                                    .name(name)
                                    .profileImgUrl(profileImgUrl)
                                    .build()
                    )
                    .build();
            userRepository.save(user);
            Long userId = user.getId();

            String accessToken = Jwts.builder()
                    .signWith(Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS512)
                    .claim("auth", user.getRole().getRoleValue())
                    .setIssuer("test")
                    .setIssuedAt(Date.from(Instant.now()))
                    .setExpiration(Date.from(Instant.now().plusSeconds(100)))
                    .setSubject(userId.toString())
                    .compact();

            String newNickname = "newNickname";
            UserModifyRequest request = new UserModifyRequest(newNickname);

            // when
            mockMvc.perform(
                    patch("/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .content(objectMapper.writeValueAsString(request))
            ).andExpect(status().isOk());
        }

        @Test
        @DisplayName("fail - 변경할 닉네임이 빈 문자열이면, 요청에 실패하고 http status 400 반환한다.")
        void fail_1() throws Exception {
            // given
            String profileImgUrl = "profileImgUrl";
            String oauthId = "oauthId";
            OAuthProvider provider = OAuthProvider.KAKAO;
            String name = "testName";
            String email = "testEmail";

            User user = User.builder()
                    .account(
                            Account.builder()
                                    .oauthId(oauthId)
                                    .provider(provider)
                                    .email(email)
                                    .name(name)
                                    .profileImgUrl(profileImgUrl)
                                    .build()
                    )
                    .build();
            userRepository.save(user);
            Long userId = user.getId();

            String accessToken = Jwts.builder()
                    .signWith(Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS512)
                    .claim("auth", user.getRole().getRoleValue())
                    .setIssuer("test")
                    .setIssuedAt(Date.from(Instant.now()))
                    .setExpiration(Date.from(Instant.now().plusSeconds(100)))
                    .setSubject(userId.toString())
                    .compact();

            String newNickname = "";
            UserModifyRequest request = new UserModifyRequest(newNickname);

            // when
            mockMvc.perform(
                    patch("/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .content(objectMapper.writeValueAsString(request))
            ).andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("fail - 변경할 닉네임이 null이면, 요청에 실패하고 http status 400 반환한다.")
        void fail_2() throws Exception {
            // given
            String profileImgUrl = "profileImgUrl";
            String oauthId = "oauthId";
            OAuthProvider provider = OAuthProvider.KAKAO;
            String name = "testName";
            String email = "testEmail";

            User user = User.builder()
                    .account(
                            Account.builder()
                                    .oauthId(oauthId)
                                    .provider(provider)
                                    .email(email)
                                    .name(name)
                                    .profileImgUrl(profileImgUrl)
                                    .build()
                    )
                    .build();
            userRepository.save(user);
            Long userId = user.getId();

            String accessToken = Jwts.builder()
                    .signWith(Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS512)
                    .claim("auth", user.getRole().getRoleValue())
                    .setIssuer("test")
                    .setIssuedAt(Date.from(Instant.now()))
                    .setExpiration(Date.from(Instant.now().plusSeconds(100)))
                    .setSubject(userId.toString())
                    .compact();

            UserModifyRequest request = new UserModifyRequest(null);

            // when
            mockMvc.perform(
                    patch("/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .content(objectMapper.writeValueAsString(request))
            ).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("유저 프로필 이미지 수정")
    class userImageSave {

        String jwtSecretKey = "8490783c21034fd55f9cde06d539607f326356fa9732d93db12263dc4ce906a02ab20311228a664522bf7ed3ff66f0b3694e94513bdfa17bc631e57030c248ed";

        @Test
        @DisplayName("success - 이미지 파일이 요청 파라미터로 넘어오면 해당 이미지를 저장하고, 저장된 이미지의 url을 반환한다.")
        void success() throws Exception {
            // given
            String profileImgUrl = "profileImgUrl";
            String oauthId = "oauthId";
            OAuthProvider provider = OAuthProvider.KAKAO;
            String name = "testName";
            String email = "testEmail";

            User user = User.builder()
                    .account(
                            Account.builder()
                                    .oauthId(oauthId)
                                    .provider(provider)
                                    .email(email)
                                    .name(name)
                                    .profileImgUrl(profileImgUrl)
                                    .build()
                    )
                    .build();
            userRepository.save(user);
            Long userId = user.getId();

            String accessToken = Jwts.builder()
                    .signWith(Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS512)
                    .claim("auth", user.getRole().getRoleValue())
                    .setIssuer("test")
                    .setIssuedAt(Date.from(Instant.now()))
                    .setExpiration(Date.from(Instant.now().plusSeconds(100)))
                    .setSubject(userId.toString())
                    .compact();

            File imgFile = ResourceUtils.getFile(this.getClass().getResource("/static/test-img.jpeg"));
            MockMultipartFile mockMultipartFile = new MockMultipartFile(
                    "imgFile",
                    "test-img.jpeg",
                    ContentType.IMAGE_JPEG.getMimeType(),
                    new FileInputStream(imgFile)
            );

            // when
            ResultActions perform = mockMvc.perform(
                    multipart("/users/me/image")
                            .file(mockMultipartFile)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            );

            perform.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.profileImgUrl").exists())
                    .andExpect(jsonPath("$.data.profileImgUrl").isNotEmpty());
        }

        @Test
        @DisplayName("fail - 요청 파라미터로 넘어온 파일이 없으면, 요청이 실패하고 http status 400 반환한다.")
        void fail() throws Exception {
            // given
            String profileImgUrl = "profileImgUrl";
            String oauthId = "oauthId";
            OAuthProvider provider = OAuthProvider.KAKAO;
            String name = "testName";
            String email = "testEmail";

            User user = User.builder()
                    .account(
                            Account.builder()
                                    .oauthId(oauthId)
                                    .provider(provider)
                                    .email(email)
                                    .name(name)
                                    .profileImgUrl(profileImgUrl)
                                    .build()
                    )
                    .build();
            userRepository.save(user);
            Long userId = user.getId();

            String accessToken = Jwts.builder()
                    .signWith(Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS512)
                    .claim("auth", user.getRole().getRoleValue())
                    .setIssuer("test")
                    .setIssuedAt(Date.from(Instant.now()))
                    .setExpiration(Date.from(Instant.now().plusSeconds(100)))
                    .setSubject(userId.toString())
                    .compact();

            // when
            ResultActions perform = mockMvc.perform(
                    multipart("/users/me/image")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            );

            perform.andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("유저 프로필 이미지 삭제")
    class userImageRemove {

        String jwtSecretKey = "8490783c21034fd55f9cde06d539607f326356fa9732d93db12263dc4ce906a02ab20311228a664522bf7ed3ff66f0b3694e94513bdfa17bc631e57030c248ed";

        @Test
        @DisplayName("success - 유저 프로필 이미지가 있다면, 이를 삭제하고 http status 200 반환한다.")
        void success() throws Exception {
            // given
            String profileImgUrl = "profileImgUrl";
            String oauthId = "oauthId";
            OAuthProvider provider = OAuthProvider.KAKAO;
            String name = "testName";
            String email = "testEmail";

            User user = User.builder()
                    .account(
                            Account.builder()
                                    .oauthId(oauthId)
                                    .provider(provider)
                                    .email(email)
                                    .name(name)
                                    .profileImgUrl(profileImgUrl)
                                    .build()
                    )
                    .build();
            userRepository.save(user);
            Long userId = user.getId();

            String accessToken = Jwts.builder()
                    .signWith(Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS512)
                    .claim("auth", user.getRole().getRoleValue())
                    .setIssuer("test")
                    .setIssuedAt(Date.from(Instant.now()))
                    .setExpiration(Date.from(Instant.now().plusSeconds(100)))
                    .setSubject(userId.toString())
                    .compact();

            File imgFile = ResourceUtils.getFile(this.getClass().getResource("/static/test-img.jpeg"));
            MockMultipartFile mockMultipartFile = new MockMultipartFile(
                    "imgFile",
                    "test-img.jpeg",
                    ContentType.IMAGE_JPEG.getMimeType(),
                    new FileInputStream(imgFile)
            );

            mockMvc.perform(
                    multipart("/users/me/image")
                            .file(mockMultipartFile)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            ).andExpect(status().isOk());

            // when
            ResultActions perform = mockMvc.perform(
                    delete("/users/me/image")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            );

            // then
            perform.andExpect(status().isOk());
        }

        @Test
        @DisplayName("fail - 유저가 기존 프로필 이미지가 없으면, 요청이 실패하고 http status 400 반환한다.")
        void fail() throws Exception {
            // given
            String profileImgUrl = "profileImgUrl";
            String oauthId = "oauthId";
            OAuthProvider provider = OAuthProvider.KAKAO;
            String name = "testName";
            String email = "testEmail";

            User user = User.builder()
                    .account(
                            Account.builder()
                                    .oauthId(oauthId)
                                    .provider(provider)
                                    .email(email)
                                    .name(name)
                                    .profileImgUrl(profileImgUrl)
                                    .build()
                    )
                    .build();
            userRepository.save(user);
            Long userId = user.getId();

            String accessToken = Jwts.builder()
                    .signWith(Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS512)
                    .claim("auth", user.getRole().getRoleValue())
                    .setIssuer("test")
                    .setIssuedAt(Date.from(Instant.now()))
                    .setExpiration(Date.from(Instant.now().plusSeconds(100)))
                    .setSubject(userId.toString())
                    .compact();

            // when
            ResultActions perform = mockMvc.perform(
                    delete("/users/me/image")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            );

            // then
            perform.andExpect(status().isBadRequest());
        }
    }
}