package com.novel.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novel.dto.NovelImportDTO;
import com.novel.model.Category;
import com.novel.model.Chapter;
import com.novel.model.Novel;
import com.novel.model.NovelStats;
import com.novel.repository.CategoryRepository;
import com.novel.repository.ChapterRepository;
import com.novel.repository.NovelRepository;
import com.novel.repository.NovelStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class NovelImportService {

    private static final Logger logger = LoggerFactory.getLogger(NovelImportService.class);

    private final NovelRepository novelRepository;
    private final ChapterRepository chapterRepository;
    private final CategoryRepository categoryRepository;
    private final NovelStatsRepository novelStatsRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public NovelImportService(NovelRepository novelRepository,
                              ChapterRepository chapterRepository,
                              CategoryRepository categoryRepository,
                              NovelStatsRepository novelStatsRepository) {
        this.novelRepository = novelRepository;
        this.chapterRepository = chapterRepository;
        this.categoryRepository = categoryRepository;
        this.novelStatsRepository = novelStatsRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        this.objectMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public List<NovelImportDTO> loadNovelsFromJson() throws IOException {
        ClassPathResource resource = new ClassPathResource("novels_data.json");
        return objectMapper.readValue(resource.getInputStream(), 
                new TypeReference<List<NovelImportDTO>>() {});
    }

    public List<NovelImportDTO> loadNovelsFromTxtJson() throws IOException {
        ClassPathResource resource = new ClassPathResource("novels_from_txt.json");
        return objectMapper.readValue(resource.getInputStream(), 
                new TypeReference<List<NovelImportDTO>>() {});
    }

    public List<NovelImportDTO> loadNovelsFromTxt100Json() throws IOException {
        ClassPathResource resource = new ClassPathResource("novels_from_txt_100.json");
        return objectMapper.readValue(resource.getInputStream(), 
                new TypeReference<List<NovelImportDTO>>() {});
    }

    public List<NovelImportDTO> loadNovelsFromTxt100WithChaptersJson() throws IOException {
        ClassPathResource resource = new ClassPathResource("novels_from_txt_100_with_chapters.json");
        return objectMapper.readValue(resource.getInputStream(), 
                new TypeReference<List<NovelImportDTO>>() {});
    }

    public List<NovelImportDTO> loadNovelsFromNovelSqlJson() throws IOException {
        ClassPathResource resource = new ClassPathResource("novels_from_novelsql.json");
        return objectMapper.readValue(resource.getInputStream(), 
                new TypeReference<List<NovelImportDTO>>() {});
    }

    public List<NovelImportDTO> loadNovelsFromNovelSqlWithChaptersJson(int batchNumber) throws IOException {
        ClassPathResource resource = new ClassPathResource("novels_from_novelsql_with_chapters_batch" + batchNumber + ".json");
        return objectMapper.readValue(resource.getInputStream(), 
                new TypeReference<List<NovelImportDTO>>() {});
    }

    public List<NovelImportDTO> loadNovelsWithChaptersJson() throws IOException {
        ClassPathResource resource = new ClassPathResource("novels_with_chapters.json");
        return objectMapper.readValue(resource.getInputStream(), 
                new TypeReference<List<NovelImportDTO>>() {});
    }

    public List<Novel> importNovelsFromJson(Long authorId) throws IOException {
        List<NovelImportDTO> novelDTOs = loadNovelsFromJson();
        List<Novel> importedNovels = new ArrayList<>();

        for (NovelImportDTO dto : novelDTOs) {
            try {
                Novel novel = importNovel(dto, authorId);
                importedNovels.add(novel);
                logger.info("成功导入小说: {}", novel.getTitle());
            } catch (Exception e) {
                logger.error("导入小说失败: {} - {}", dto.getTitle(), e.getMessage());
            }
        }

        return importedNovels;
    }

    public List<Novel> importNovelsFromTxtJson(Long authorId) throws IOException {
        List<NovelImportDTO> novelDTOs = loadNovelsFromTxtJson();
        List<Novel> importedNovels = new ArrayList<>();

        for (NovelImportDTO dto : novelDTOs) {
            try {
                Novel novel = importNovel(dto, authorId);
                importedNovels.add(novel);
                logger.info("成功导入小说: {}", novel.getTitle());
            } catch (Exception e) {
                logger.error("导入小说失败: {} - {}", dto.getTitle(), e.getMessage());
            }
        }

        return importedNovels;
    }

    public List<Novel> importNovelsFromTxt100Json(Long authorId) throws IOException {
        List<NovelImportDTO> novelDTOs = loadNovelsFromTxt100Json();
        List<Novel> importedNovels = new ArrayList<>();

        for (NovelImportDTO dto : novelDTOs) {
            try {
                Novel novel = importNovel(dto, authorId);
                importedNovels.add(novel);
                logger.info("成功导入小说: {}", novel.getTitle());
            } catch (Exception e) {
                logger.error("导入小说失败: {} - {}", dto.getTitle(), e.getMessage());
            }
        }

        return importedNovels;
    }

    public List<Novel> importNovelsFromTxt100WithChaptersJson(Long authorId) throws IOException {
        List<NovelImportDTO> novelDTOs = loadNovelsFromTxt100WithChaptersJson();
        List<Novel> importedNovels = new ArrayList<>();

        for (NovelImportDTO dto : novelDTOs) {
            try {
                Novel novel = importNovel(dto, authorId);
                importedNovels.add(novel);
                logger.info("成功导入小说: {} ({} 个章节)", novel.getTitle(), dto.getChapters() != null ? dto.getChapters().size() : 0);
            } catch (Exception e) {
                logger.error("导入小说失败: {} - {}", dto.getTitle(), e.getMessage());
            }
        }

        return importedNovels;
    }

    public List<Novel> importNovelsFromNovelSqlWithChaptersJson(int batchNumber, Long authorId) throws IOException {
        List<NovelImportDTO> novelDTOs = loadNovelsFromNovelSqlWithChaptersJson(batchNumber);
        List<Novel> importedNovels = new ArrayList<>();

        for (NovelImportDTO dto : novelDTOs) {
            try {
                Novel novel = importNovel(dto, authorId);
                importedNovels.add(novel);
                logger.info("成功导入小说: {} ({} 个章节)", novel.getTitle(), dto.getChapters() != null ? dto.getChapters().size() : 0);
            } catch (Exception e) {
                logger.error("导入小说失败: {} - {}", dto.getTitle(), e.getMessage());
            }
        }

        return importedNovels;
    }

    public List<Novel> importNovelsFromNovelSqlJson(Long authorId) throws IOException {
        List<NovelImportDTO> novelDTOs = loadNovelsFromNovelSqlJson();
        List<Novel> importedNovels = new ArrayList<>();

        for (NovelImportDTO dto : novelDTOs) {
            try {
                Novel novel = importNovel(dto, authorId);
                importedNovels.add(novel);
                logger.info("成功导入小说: {}", novel.getTitle());
            } catch (Exception e) {
                logger.error("导入小说失败: {} - {}", dto.getTitle(), e.getMessage());
            }
        }

        return importedNovels;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Novel importNovel(NovelImportDTO dto, Long authorId) {
        Optional<Novel> existingNovel = novelRepository.findByTitle(dto.getTitle());
        if (existingNovel.isPresent()) {
            logger.info("小说已存在: {}", dto.getTitle());
            Novel novel = existingNovel.get();
            if (dto.getChapters() != null && !dto.getChapters().isEmpty()) {
                importChapters(novel.getId(), dto.getChapters());
            }
            return novel;
        }

        Novel novel = new Novel();
        novel.setTitle(dto.getTitle());
        novel.setAuthor(dto.getAuthor());
        novel.setDescription(dto.getDescription());
        novel.setCoverImage(dto.getCoverImage());
        novel.setAuthorId(authorId);
        novel.setViews(0);
        novel.setBookmarks(0);
        novel.setReviewStatus("APPROVED");
        novel.setStatus(dto.getStatus() != null ? dto.getStatus() : "连载中");
        novel.setCreatedAt(new Date());
        novel.setUpdatedAt(new Date());

        String categoryName = dto.getCategory();
        if (categoryName != null && !categoryName.isEmpty()) {
            Optional<Category> category = categoryRepository.findByName(categoryName);
            if (category.isPresent()) {
                novel.setCategory(category.get().getName());
            } else {
                Category newCategory = new Category();
                newCategory.setName(categoryName);
                newCategory.setDescription(categoryName + "类小说");
                categoryRepository.save(newCategory);
                novel.setCategory(categoryName);
            }
        }

        novel = novelRepository.save(novel);

        NovelStats stats = new NovelStats();
        stats.setNovelId(novel.getId());
        novelStatsRepository.save(stats);

        if (dto.getChapters() != null && !dto.getChapters().isEmpty()) {
            importChapters(novel.getId(), dto.getChapters());
        }

        return novel;
    }

    private void importChapters(Long novelId, List<NovelImportDTO.ChapterDTO> chapterDTOs) {
        for (NovelImportDTO.ChapterDTO chapterDTO : chapterDTOs) {
            Chapter chapter = new Chapter();
            chapter.setNovelId(novelId);
            chapter.setChapterNumber(chapterDTO.getChapterNumber());
            chapter.setTitle(chapterDTO.getTitle());
            chapter.setContent(chapterDTO.getContent());
            chapter.setWordCount(chapterDTO.getContent() != null ? chapterDTO.getContent().length() : 0);
            chapter.setCreatedAt(new Date());
            chapter.setUpdatedAt(new Date());
            chapterRepository.save(chapter);
        }
        logger.info("导入 {} 个章节到小说 ID: {}", chapterDTOs.size(), novelId);
    }

    public int getImportCount() throws IOException {
        List<NovelImportDTO> novels = loadNovelsFromJson();
        return novels.size();
    }

    public int getImportFromTxtCount() throws IOException {
        List<NovelImportDTO> novels = loadNovelsFromTxtJson();
        return novels.size();
    }

    public int getImportFromTxt100Count() throws IOException {
        List<NovelImportDTO> novels = loadNovelsFromTxt100Json();
        return novels.size();
    }

    public int getImportFromTxt100WithChaptersCount() throws IOException {
        List<NovelImportDTO> novels = loadNovelsFromTxt100WithChaptersJson();
        return novels.size();
    }

    public int getImportFromNovelSqlCount() throws IOException {
        List<NovelImportDTO> novels = loadNovelsFromNovelSqlJson();
        return novels.size();
    }

    public int getImportWithChaptersCount() throws IOException {
        List<NovelImportDTO> novels = loadNovelsWithChaptersJson();
        return novels.size();
    }

    public List<Novel> importNovelsWithChaptersJson(Long authorId) throws IOException {
        List<NovelImportDTO> novelDTOs = loadNovelsWithChaptersJson();
        List<Novel> importedNovels = new ArrayList<>();

        for (NovelImportDTO dto : novelDTOs) {
            try {
                Novel novel = importNovel(dto, authorId);
                importedNovels.add(novel);
                logger.info("成功导入小说: {} ({} 个章节)", novel.getTitle(), dto.getChapters() != null ? dto.getChapters().size() : 0);
            } catch (Exception e) {
                logger.error("导入小说失败: {} - {}", dto.getTitle(), e.getMessage());
            }
        }

        return importedNovels;
    }
}
