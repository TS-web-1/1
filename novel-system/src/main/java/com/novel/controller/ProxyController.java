package com.novel.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/proxy")
@CrossOrigin(origins = "*")
public class ProxyController {

    /**
     * 图片代理 - 解决 CORS 问题
     * 使用方式：/api/proxy/image?url=https://images.unsplash.com/photo-xxx
     * 支持的图片源:
     * - images.unsplash.com
     * - via.placeholder.com
     * - picsum.photos
     */
    @GetMapping("/image")
    public ResponseEntity<byte[]> proxyImage(@RequestParam String url) {
        try {
            // 验证 URL 必须是允许的图片源
            if (!url.startsWith("https://images.unsplash.com/") && 
                !url.startsWith("https://via.placeholder.com/") &&
                !url.startsWith("https://picsum.photos/")) {
                return ResponseEntity.badRequest().body("只允许代理 Unsplash、Placeholder 和 Picsum 图片".getBytes());
            }

            RestTemplate restTemplate = new RestTemplate();
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            headers.set("Accept", "image/webp,image/apng,image/*,*/*;q=0.8");
            headers.set("Referer", "http://localhost:8081/");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 获取图片
            ResponseEntity<byte[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                byte[].class
            );
            
            // 设置响应头
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.IMAGE_JPEG);
            responseHeaders.setCacheControl("public, max-age=31536000");
            responseHeaders.setAccessControlAllowOrigin("*");
            
            return new ResponseEntity<>(response.getBody(), responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(("图片加载失败：" + e.getMessage()).getBytes());
        }
    }
}
