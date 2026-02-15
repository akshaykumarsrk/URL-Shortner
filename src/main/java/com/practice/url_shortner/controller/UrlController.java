package com.practice.url_shortner.controller;

import com.practice.url_shortner.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/url")
public class UrlController {

    @Autowired
    UrlService urlService;

    @PostMapping("/shorten")
    public ResponseEntity<String> createShortUrl(@RequestParam String originalUrl)
    {
        String shortCode = urlService.createShortUrl(originalUrl);

        String shortUrl = "http://localhost:8080/" + shortCode;

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(shortUrl);
    }

    @GetMapping("/shortCode/{shortCode}")
    public ResponseEntity<Void> getShortCode(@PathVariable String shortCode, HttpServletRequest request)
    {
        String originalUrl = urlService.getOriginalUrl(shortCode, request);

        return  ResponseEntity
                .status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, originalUrl)
                .build();
    }
}
