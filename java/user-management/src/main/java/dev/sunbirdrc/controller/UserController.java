package dev.sunbirdrc.controller;


import dev.sunbirdrc.dto.*;
import dev.sunbirdrc.service.UserService;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.List;

@RestController
@RequestMapping(path = "/api/v1")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<UserTokenDetailsDTO> loginUser(@Valid @RequestBody UserLoginDTO userLoginDTO) {
        UserTokenDetailsDTO keycloakTokenDetailsDTO = userService.loginAndGenerateKeycloakToken(userLoginDTO);

        return new ResponseEntity<>(keycloakTokenDetailsDTO, HttpStatus.OK);
    }

    @PostMapping("/registerUser")
    public ResponseEntity<String> registerUser(@Valid @RequestBody UserDetailsDTO userDTO) {
        boolean status = userService.registerUser(userDTO);

        if (status) {
            return new ResponseEntity<>("Successfully added user", HttpStatus.CREATED);
        }else {
            return new ResponseEntity<>("Unable to create user", HttpStatus.FAILED_DEPENDENCY);
        }
    }

    @PostMapping("/verifyAndUpdate/otp")
    public ResponseEntity<String> verifyUserMailOTP(@Valid @RequestBody UserOtpDTO userOtpDTO) {
        boolean verified = false;
        try {
            verified = userService.verifyMailOTP(userOtpDTO);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (verified) {
            return new ResponseEntity<>("Successfully verified user", HttpStatus.CREATED);
        }else {
            return new ResponseEntity<>("Unable to verify", HttpStatus.FAILED_DEPENDENCY);
        }
    }

    @PostMapping("/admin/generateOtp")
    public ResponseEntity<String> generateAdminOtp(@Valid @RequestBody AdminDTO adminDTO) {
        try {
            userService.generateAdminOtp(adminDTO);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new ResponseEntity<>("Sending OTP to user mail", HttpStatus.OK);
    }

    @PostMapping("/admin/login")
    public ResponseEntity<UserTokenDetailsDTO> loginAdminUser(@Valid @RequestBody AdminLoginDTO adminLoginDTO) {
        UserTokenDetailsDTO tokenDetailsDTO = null;
        try {
            tokenDetailsDTO = userService.getAdminTokenByOtp(adminLoginDTO);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new ResponseEntity<>(tokenDetailsDTO, HttpStatus.OK);
    }

    @GetMapping(path = "/keycloak")
    public ResponseEntity<String> getUser(){


        return new ResponseEntity<>("Role base access", HttpStatus.OK);
    }

    @PostMapping("/keycloak/createBulkUser")
    public ResponseEntity<BulkCustomUserResponseDTO> createBulkUser(@Valid @RequestBody List<CustomUserDTO> customUserDTOList) {
        BulkCustomUserResponseDTO bulkCustomUserResponseDTO = userService.addBulkUser(customUserDTOList);

        return new ResponseEntity<>(bulkCustomUserResponseDTO, HttpStatus.CREATED);
    }

    @PostMapping("/user/generateOtp")
    public ResponseEntity<String> generateUserOtp(@Valid @RequestBody CustomUsernameDTO customUsernameDTO) {
        userService.generateCustomUserOtp(customUsernameDTO);

        return new ResponseEntity<>("Sending OTP to user mail", HttpStatus.OK);
    }

    @PostMapping("/user/login")
    public ResponseEntity<UserTokenDetailsDTO> loginCustomUser(@Valid @RequestBody CustomUserLoginDTO customUserLoginDTO) {
        UserTokenDetailsDTO tokenDetailsDTO = null;
        try {
            tokenDetailsDTO = userService.getCustomUserTokenByOtp(customUserLoginDTO);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new ResponseEntity<>(tokenDetailsDTO, HttpStatus.OK);
    }

    @PostMapping(path = "/keycloak/user/delete")
    public ResponseEntity<String> deleteUser(@Valid @RequestBody List<CustomUserDeleteDTO> customUserDeleteDTOList){
        userService.deleteBulkUSer(customUserDeleteDTOList);

        return new ResponseEntity<>("Successfully delete the user", HttpStatus.OK);
    }

    @PostMapping("/keycloak/user/update")
    public ResponseEntity<String> updateUser(@Valid @RequestBody CustomUserUpdateDTO customUserUpdateDTO) {
        userService.updateUser(customUserUpdateDTO);

        return new ResponseEntity<>("Successfully updated user", HttpStatus.OK);
    }

    @PostMapping("/keycloak/user/create")
    public ResponseEntity<CustomUserResponseDTO> createCustomUser(@Valid @RequestBody CustomUserDTO customUserDTO) {
        CustomUserResponseDTO customUserResponseDTO = userService.createCustomUser(customUserDTO);

        return new ResponseEntity<>(customUserResponseDTO, HttpStatus.OK);
    }

    @PostMapping("/keycloak/persist/userCredential")
    public ResponseEntity<String> persistUserCredential(@RequestBody CustomUserDTO customUserDTO) {
        try {
            userService.persistUserDetailsWithCredentials(customUserDTO);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to persist", HttpStatus.EXPECTATION_FAILED);
        }

        return new ResponseEntity<>("Persist successfully", HttpStatus.OK);
    }
}
