package com.stockhub.alert.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import java.util.Map;

@FeignClient(name = "auth-service")
public interface AuthClient {

    //  GET /api/auth/users/role/{role}
    @GetMapping("/api/auth/users/role/{role}")
    List<Map<String, Object>> getUsersByRole(
            @PathVariable("role") String role);
}