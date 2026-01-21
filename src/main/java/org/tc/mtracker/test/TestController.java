package org.tc.mtracker.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tests")
@Slf4j
public class TestController {

    @GetMapping("/best-endpoint")
    public String test() {
        log.info("Test endpoint was triggered!");
        return "Service is working. You are the best Frontend Developer!";
    }
}
