package com.practice.url_shortner.controller;

import com.practice.url_shortner.model.UrlMapping;
import com.practice.url_shortner.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/url")
public class UrlController {

    @Autowired
    UrlService urlService;

    @PostMapping("/shorten")
    public ResponseEntity<String> createShortUrl(@RequestParam String originalUrl)
    {
        String shortCode = urlService.createShortUrl(originalUrl);

        String shortUrl = "https://url-shortner-pj51.onrender.com/" + shortCode;

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

    @GetMapping("/urls")
    public ResponseEntity<List<UrlMapping>> getUserUrls() {
        List<UrlMapping> reponses = urlService.getUserUrls();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(reponses);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteUrl(@PathVariable Long id) {
        urlService.deleteUrl(id);
        return ResponseEntity.status(HttpStatus.OK).body("URL deleted");
    }

    @GetMapping("/page/urls")
    public ResponseEntity<Page<UrlMapping>> getUrls(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "5") int size) {
        Page<UrlMapping> response = urlService.getUrls(page, size);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
