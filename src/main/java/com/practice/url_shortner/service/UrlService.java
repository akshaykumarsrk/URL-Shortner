package com.practice.url_shortner.service;

import com.practice.url_shortner.model.UrlMapping;
import com.practice.url_shortner.repository.UrlRepository;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class UrlService {

    public final String BASE62 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    @Autowired
    UrlRepository urlRepository;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    public String createShortUrl(String originalUrl) {

        // 1️⃣ Save first to generate ID
        UrlMapping url = new UrlMapping();
        url.setOriginalUrl(originalUrl);
        urlRepository.save(url);   // ID generated here

        // 2️⃣ Convert ID → Base62
        Long id = url.getId();
        String shortUrl = encodeBase62(id);

        // 3️⃣ Update shortcode
        url.setShortCode(shortUrl);
        urlRepository.save(url);

        return shortUrl;

    }
    public String encodeBase62(Long id) {

        if (id == 0)
            return "a";

        StringBuilder sb = new StringBuilder();

        while (id > 0) {
            sb.append(BASE62.charAt((int)(id%62)));
            id = id / 62;
        }
        return sb.reverse().toString();
    }

    public String getOriginalUrl(String shortCode, HttpServletRequest request) {

        // 1️⃣ Check Redis
        String cacheUrl = redisTemplate.opsForValue().get(shortCode);
        if(cacheUrl != null){
            // Click analytics for cached hit
            redisTemplate.opsForValue().increment("click:" + shortCode); // Count clicks in Redis (fast) Later sync to DB (batch update)
            return cacheUrl;
        }

        // REDIS FOR RATE LIMITING
        // 👉 Per IP only 100 requests per minute allowed
        String ip = request.getRemoteAddr();
        String key = "rate" + ip;
        Long count = redisTemplate.opsForValue().increment(key);

        // “First request”
        if(count == 1)
        {
            // “Reset counter after 1 minute”
            redisTemplate.expire(key, 1, TimeUnit.MINUTES);
        }
        if(count > 100)
        {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests");
        }

        // 2️⃣ Fetch from DB
        UrlMapping url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND ,"Short Url not found"));

        LocalDateTime expiry = url.getExpiryAt();
        if(expiry != null && LocalDateTime.now().isAfter(expiry))
        {
            throw new ResponseStatusException(HttpStatus.GONE ,"Expired Url");
        }

        // 3️⃣ Store in Redis
        // Add 10 minutes TTL
        // 📌 Cache expires after 10 minutes
        redisTemplate.opsForValue().set(shortCode,url.getOriginalUrl() ,10, TimeUnit.MINUTES);

        // ✅ Increase total click count
        url.setClickCount(url.getClickCount() + 1);  // Total number of times this short URL was opened
        urlRepository.save(url); // to update in database
        return url.getOriginalUrl();
    }
}
