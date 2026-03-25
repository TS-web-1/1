package com.novel.controller;

import com.novel.dto.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

@RestController
@RequestMapping("/admin/test")
public class NovelCoverCrawlerController {

    private final JdbcTemplate jdbcTemplate;

    public NovelCoverCrawlerController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 爬取小说封面图片
     */
    @GetMapping("/crawl-novel-covers")
    public ApiResponse<Map<String, Object>> crawlNovelCovers(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String novelId) {
        
        try {
            System.out.println("=== 开始爬取小说封面 ===");
            
            // 查询需要爬取封面的小说
            String sql;
            List<Object> params = new ArrayList<>();
            
            if (novelId != null) {
                // 爬取指定小说
                sql = "SELECT id, title, author FROM novels WHERE id = ?";
                params.add(Long.parseLong(novelId));
            } else {
                // 爬取所有没有封面的小说
                sql = "SELECT id, title, author FROM novels WHERE cover_image IS NULL OR cover_image = ''";
                if (limit != null) {
                    sql += " LIMIT ?";
                    params.add(limit);
                }
            }
            
            List novels;
            if (params.isEmpty()) {
                novels = jdbcTemplate.queryForList(sql);
            } else {
                novels = jdbcTemplate.queryForList(sql, params.toArray(new Object[0]));
            }
            
            if (novels.isEmpty()) {
                Map<String, Object> emptyResult = new HashMap<>();
                emptyResult.put("message", "没有需要爬取封面的小说");
                return ApiResponse.success("没有需要爬取封面的小说", emptyResult);
            }
            
            System.out.println("找到 " + novels.size() + " 本需要爬取封面的小说");
            
            // 调用 Python 爬虫脚本
            Map<String, Object> results = new HashMap<>();
            int successCount = 0;
            int failCount = 0;
            
            for (Object novelObj : novels) {
                Map novel = (Map) novelObj;
                Object idObj = novel.get("id");
                Long id = (idObj instanceof Number) ? ((Number) idObj).longValue() : 0L;
                String title = (String) novel.get("title");
                String author = (String) novel.get("author");
                
                System.out.println("\n正在爬取：" + title + " - " + author);
                
                // 调用爬虫脚本
                String coverUrl = crawlSingleCover(title, author);
                
                if (coverUrl != null && !coverUrl.isEmpty()) {
                    // 更新数据库
                    String updateSql = "UPDATE novels SET cover_image = ? WHERE id = ?";
                    int updateResult = jdbcTemplate.update(updateSql, coverUrl, id.longValue());
                    
                    if (updateResult > 0) {
                        successCount++;
                        System.out.println("  ✓ 封面已更新：" + coverUrl);
                    }
                } else {
                    failCount++;
                    System.out.println("  ✗ 未找到封面");
                }
                
                // 限制爬取速度
                Thread.sleep(1000);
            }
            
            results.put("total", novels.size());
            results.put("success", successCount);
            results.put("failed", failCount);
            
            System.out.println("\n=== 爬取完成 ===");
            System.out.println("总计：" + novels.size() + " 本");
            System.out.println("成功：" + successCount + " 本");
            System.out.println("失败：" + failCount + " 本");
            
            return ApiResponse.success(
                "爬取完成！成功：" + successCount + " 本，失败：" + failCount + " 本",
                results
            );
            
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("爬取失败：" + e.getMessage());
        }
    }
    
    /**
     * 爬取单本小说封面
     */
    private String crawlSingleCover(String title, String author) {
        try {
            // 构建爬虫脚本路径
            String scriptPath = "c:\\BC\\project\\crawler\\crawl_landscape_simple.py";
            
            // 使用正确的 Python 路径
            String pythonPath = "C:\\Users\\Administrator\\AppData\\Local\\Microsoft\\WindowsApps\\python.exe";
            
            // 构建命令
            List<String> command = new ArrayList<>();
            command.add(pythonPath);
            command.add(scriptPath);
            command.add("--title");
            command.add(title);
            command.add("--author");
            command.add(author);
            
            System.out.println("执行命令：" + String.join(" ", command));
            
            // 执行命令
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    System.out.println("Python 输出：" + line);
                }
            }
            
            // 读取错误输出
            StringBuilder errorOutput = new StringBuilder();
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), "UTF-8"))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                    System.out.println("Python 错误：" + line);
                }
            }
            
            // 等待进程结束
            int exitCode = process.waitFor();
            System.out.println("Python 脚本退出码：" + exitCode);
            
            if (exitCode == 0) {
                // 解析 JSON 输出
                String outputStr = output.toString();
                System.out.println("完整输出：" + outputStr);
                
                if (errorOutput.length() > 0) {
                    System.out.println("错误输出：" + errorOutput.toString());
                }
                
                int jsonStart = outputStr.indexOf("{");
                int jsonEnd = outputStr.lastIndexOf("}");
                
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    String jsonStr = outputStr.substring(jsonStart, jsonEnd + 1);
                    System.out.println("JSON 字符串：" + jsonStr);
                    // 简单解析 JSON 获取封面 URL
                    if (jsonStr.contains("\"cover_url\"") && jsonStr.contains("\"success\": true")) {
                        int urlStart = jsonStr.indexOf("http");
                        if (urlStart >= 0 && urlStart < jsonStr.length()) {
                            int urlEnd = jsonStr.indexOf("\"", urlStart);
                            if (urlEnd > urlStart) {
                                String coverUrl = jsonStr.substring(urlStart, urlEnd);
                                System.out.println("提取到封面 URL: " + coverUrl);
                                return coverUrl;
                            }
                        }
                    }
                }
            } else {
                System.out.println("Python 脚本非正常退出，退出码：" + exitCode);
            }
            
        } catch (Exception e) {
            System.out.println("爬取失败：" + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * 使用简单 API 爬取封面（备用方案）
     */
    @GetMapping("/fetch-cover-from-api")
    public ApiResponse<Map<String, String>> fetchCoverFromApi(
            @RequestParam String title,
            @RequestParam(required = false) String author) {
        
        try {
            // 使用豆瓣 API 获取封面
            String doubanApiUrl = "https://api.douban.com/v2/book/search?q=" + 
                                  java.net.URLEncoder.encode(title, "UTF-8");
            
            // 这里简化处理，实际应该调用 API
            // 由于豆瓣 API 需要授权，这里仅作为示例
            
            Map<String, String> result = new HashMap<>();
            result.put("title", title);
            result.put("author", author != null ? author : "未知");
            result.put("note", "需要实现实际的 API 调用逻辑");
            
            return ApiResponse.success("获取成功", result);
            
        } catch (Exception e) {
            return ApiResponse.error("获取失败：" + e.getMessage());
        }
    }
}
