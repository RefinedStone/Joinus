package com.example.chatservice.tests;

import com.example.chatservice.config.security.user.UserDetailsImpl;
import com.example.chatservice.feignclient.MainServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    private MainServiceClient mainServiceClient;
    @Autowired
    public TestController(MainServiceClient mainServiceClient){
        this.mainServiceClient = mainServiceClient;
    }
    @GetMapping("/test")
    public String test() {
        return "test";
    }

    @GetMapping("/test2")
    public String test2(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return userDetails.getAccount();
    }
    @GetMapping("/test3")
    public Object test3(@RequestHeader("auth") String auth, @RequestHeader("ACCOUNT-VALUE") String accountValue) {
        Object myInfo = mainServiceClient.getInfo(auth,accountValue);
        return myInfo;
    }
    
}
