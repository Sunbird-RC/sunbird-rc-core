package dev.sunbirdrc.plugin.controller;

import dev.sunbirdrc.plugin.dto.FetchCredentialsDto;
import dev.sunbirdrc.plugin.dto.SendOTPDto;
import dev.sunbirdrc.plugin.services.MosipServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnExpression("#{(environment.MOSIP_ENABLED?:'false').equals('true')}")
@RestController
public class CredentialController {

    @Autowired
    MosipServices mosipServices;

    @RequestMapping(value = "/mosip/fetchCredential", method = RequestMethod.POST)
    public ResponseEntity<Object> fetchCredentials(@RequestBody FetchCredentialsDto requestCredentialsDto) {
        return ResponseEntity.ok(mosipServices.fetchCredentials(requestCredentialsDto));
    }

    @RequestMapping(value = "/mosip/sendOTP", method = RequestMethod.POST)
    public ResponseEntity<Object> sendOTP(@RequestBody SendOTPDto otpDto) {
        return ResponseEntity.ok().body(mosipServices.generateOTP(otpDto));
    }
}
