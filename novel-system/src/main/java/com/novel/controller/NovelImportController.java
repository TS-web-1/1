package com.novel.controller;

import com.novel.dto.ApiResponse;
import com.novel.model.Novel;
import com.novel.service.NovelImportService;
import com.novel.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/import")
public class NovelImportController {

    private final NovelImportService novelImportService;
    private final JwtUtil jwtUtil;

    @Autowired
    public NovelImportController(NovelImportService novelImportService, JwtUtil jwtUtil) {
        this.novelImportService = novelImportService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/preview")
    public ApiResponse<Integer> previewImport() {
        try {
            int count = novelImportService.getImportCount();
            return ApiResponse.success(count);
        } catch (Exception e) {
            return ApiResponse.error("预览失败: " + e.getMessage());
        }
    }

    @GetMapping("/preview/txt")
    public ApiResponse<Integer> previewImportFromTxt() {
        try {
            int count = novelImportService.getImportFromTxtCount();
            return ApiResponse.success(count);
        } catch (Exception e) {
            return ApiResponse.error("预览失败: " + e.getMessage());
        }
    }

    @GetMapping("/preview/txt100")
    public ApiResponse<Integer> previewImportFromTxt100() {
        try {
            int count = novelImportService.getImportFromTxt100Count();
            return ApiResponse.success(count);
        } catch (Exception e) {
            return ApiResponse.error("预览失败: " + e.getMessage());
        }
    }

    @GetMapping("/preview/txt100-with-chapters")
    public ApiResponse<Integer> previewImportFromTxt100WithChapters() {
        try {
            int count = novelImportService.getImportFromTxt100WithChaptersCount();
            return ApiResponse.success(count);
        } catch (Exception e) {
            return ApiResponse.error("预览失败: " + e.getMessage());
        }
    }

    @GetMapping("/preview/novelsql")
    public ApiResponse<Integer> previewImportFromNovelSql() {
        try {
            int count = novelImportService.getImportFromNovelSqlCount();
            return ApiResponse.success(count);
        } catch (Exception e) {
            return ApiResponse.error("预览失败: " + e.getMessage());
        }
    }

    @PostMapping("/execute")
    public ApiResponse<List<Novel>> executeImport(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") Long authorId) {
        try {
            List<Novel> novels = novelImportService.importNovelsFromJson(authorId);
            return ApiResponse.success("成功导入 " + novels.size() + " 部小说", novels);
        } catch (Exception e) {
            return ApiResponse.error("导入失败: " + e.getMessage());
        }
    }

    @PostMapping("/execute/txt")
    public ApiResponse<List<Novel>> executeImportFromTxt(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") Long authorId) {
        try {
            List<Novel> novels = novelImportService.importNovelsFromTxtJson(authorId);
            return ApiResponse.success("成功导入 " + novels.size() + " 部小说", novels);
        } catch (Exception e) {
            return ApiResponse.error("导入失败: " + e.getMessage());
        }
    }

    @PostMapping("/execute/txt100")
    public ApiResponse<List<Novel>> executeImportFromTxt100(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") Long authorId) {
        try {
            List<Novel> novels = novelImportService.importNovelsFromTxt100Json(authorId);
            return ApiResponse.success("成功导入 " + novels.size() + " 部小说", novels);
        } catch (Exception e) {
            return ApiResponse.error("导入失败: " + e.getMessage());
        }
    }

    @PostMapping("/execute/txt100-with-chapters")
    public ApiResponse<List<Novel>> executeImportFromTxt100WithChapters(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") Long authorId) {
        try {
            List<Novel> novels = novelImportService.importNovelsFromTxt100WithChaptersJson(authorId);
            return ApiResponse.success("成功导入 " + novels.size() + " 部小说", novels);
        } catch (Exception e) {
            return ApiResponse.error("导入失败: " + e.getMessage());
        }
    }

    @PostMapping("/execute/novelsql")
    public ApiResponse<List<Novel>> executeImportFromNovelSql(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") Long authorId) {
        try {
            List<Novel> novels = novelImportService.importNovelsFromNovelSqlJson(authorId);
            return ApiResponse.success("成功导入 " + novels.size() + " 部小说", novels);
        } catch (Exception e) {
            return ApiResponse.error("导入失败: " + e.getMessage());
        }
    }

    @PostMapping("/execute/novelsql-with-chapters")
    public ApiResponse<List<Novel>> executeImportFromNovelSqlWithChapters(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") Integer batchNumber,
            @RequestParam(defaultValue = "1") Long authorId) {
        try {
            List<Novel> novels = novelImportService.importNovelsFromNovelSqlWithChaptersJson(batchNumber, authorId);
            return ApiResponse.success("成功导入 " + novels.size() + " 部小说", novels);
        } catch (Exception e) {
            return ApiResponse.error("导入失败: " + e.getMessage());
        }
    }

    @GetMapping("/preview/with-chapters")
    public ApiResponse<Integer> previewImportWithChapters() {
        try {
            int count = novelImportService.getImportWithChaptersCount();
            return ApiResponse.success(count);
        } catch (Exception e) {
            return ApiResponse.error("预览失败: " + e.getMessage());
        }
    }

    @PostMapping("/execute/with-chapters")
    public ApiResponse<List<Novel>> executeImportWithChapters(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") Long authorId) {
        try {
            List<Novel> novels = novelImportService.importNovelsWithChaptersJson(authorId);
            return ApiResponse.success("成功导入 " + novels.size() + " 部小说", novels);
        } catch (Exception e) {
            return ApiResponse.error("导入失败: " + e.getMessage());
        }
    }
}
