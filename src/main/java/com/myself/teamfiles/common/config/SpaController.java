package com.myself.teamfiles.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * SPA fallback: serve index.html for non-API routes (only when frontend dist exists)
 */
@Controller
public class SpaController {

    @RequestMapping(value = {
            "/",
            "/login",
            "/change-password",
            "/dashboard",
            "/files/**",
            "/tags",
            "/search",
            "/recycle",
            "/profile",
            "/admin/**",
            "/ai/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
