package com.SA.StudyApp.controller.authentication;

import com.SA.StudyApp.constant.APIConstant;
import com.SA.StudyApp.dto.request.authentication.AuthenticationRequest;
import com.SA.StudyApp.dto.request.authentication.RegisterRequest;
import com.SA.StudyApp.dto.response.authentication.AuthenticationResponse;
import com.SA.StudyApp.dto.response.authentication.CookieResponse;
import com.SA.StudyApp.dto.response.user.UserResponse;
import com.SA.StudyApp.model.user.RefreshToken;
import com.SA.StudyApp.model.user.User;
import com.SA.StudyApp.service.authentication.AuthenticationService;
import com.SA.StudyApp.service.token.JwtService;
import com.SA.StudyApp.service.token.RefreshTokenService;
import com.SA.StudyApp.service.user.service.UserDetailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.expression.ExpressionException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RequestMapping(APIConstant.AUTH)
@RestController
@AllArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailService userDetailService;
    @GetMapping(APIConstant.AUTH_ME)
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse authenticatedUser(){
        return authenticationService.me();
    }
    @PostMapping(APIConstant.SIGNUP)
    public ResponseEntity<User> register(@RequestBody @Valid RegisterRequest registerRequest) {
        User registeredUser = authenticationService.signup(registerRequest);

        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping(APIConstant.LOGIN)
    public ResponseEntity<?> authenticate(@RequestBody @Valid AuthenticationRequest authenticationRequest) {
        AuthenticationResponse authenticationResponse = authenticationService.authenticate(authenticationRequest);
        return ResponseEntity.ok()
                .body(authenticationResponse);
    }
    @PostMapping(APIConstant.LOGOUT)
    public ResponseEntity<?> logoutUser() {
        CookieResponse cookieResponse = authenticationService.signout();
        return ResponseEntity.ok()
                .body("You've been signed out!");
    }
    @PostMapping(APIConstant.REFRESH_TOKEN)
    public ResponseEntity<?> refreshToken(HttpServletRequest httpServletRequest){
        String refreshToken = jwtService.getJwtRefreshFromCookies(httpServletRequest);
        System.out.println(refreshToken);
        if ((refreshToken != null) && (refreshToken.length() > 0)) {
            return refreshTokenService.findByToken(refreshToken)
                    .map(refreshTokenService::verifyExpiration)
                    .map(RefreshToken::getUser)
                    .map(user -> {
                        ResponseCookie jwtCookie = jwtService.generateJwtCookie(user);

                        return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                                .body(jwtCookie);
                    })
                    .orElseThrow(() -> new ExpressionException(refreshToken,
                            "Refresh token is not in database!"));
        }

        return ResponseEntity.badRequest().body("Refresh Token is empty!");
    }
}