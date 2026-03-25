package com.novel.service;

import com.novel.dto.ChapterDTO;
import com.novel.model.*;
import com.novel.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 管理员服务类
 * 处理管理员相关的所有业务逻辑，包括小说审核、章节审核、分类管理、用户管理等
 */
@Service
public class AdminService {

    @Autowired
    private NovelRepository novelRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserBehaviorRepository userBehaviorRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private ReadingProgressRepository readingProgressRepository;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private DraftChapterRepository draftChapterRepository;

    @Autowired
    private BookListRepository bookListRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private NovelStatsRepository novelStatsRepository;

    /**
     * 批量更新：将连载中字数超过指定值的小说改为完结状态
     * @param wordCountThreshold 字数阈值
     * @return 更新的小说数量
     */
    @Transactional
    public int updateOngoingToCompleted(Integer wordCountThreshold) {
        return novelRepository.updateOngoingToCompleted(wordCountThreshold);
    }

    /**
     * 查询连载中字数超过指定值的小说数量
     * @param wordCountThreshold 字数阈值
     * @return 符合条件的小说数量
     */
    public long countOngoingNovelsWithWordCount(Integer wordCountThreshold) {
        return novelRepository.countOngoingNovelsWithWordCount(wordCountThreshold);
    }

    // ==================== 管理员登录 ====================

    /**
     * 验证管理员账号密码
     * @param username 用户名
     * @param password 密码
     * @return 验证通过的管理员用户，验证失败返回null
     */
    public User validateAdmin(String username, String password) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null && user.getPassword().equals(password) && "ADMIN".equals(user.getRole())) {
            return user;
        }
        return null;
    }

    // ==================== 小说审核功能 ====================

    /**
     * 获取待审核的小说列表
     * @return 待审核的小说列表
     */
    public List<Novel> getPendingNovels() {
        // 只查询状态为 PENDING 或 null 的小说（真正需要审核的）
        // 已通过审核（APPROVED）或已拒绝（REJECTED）的不显示
        return novelRepository.findPendingNovels();
    }

    /**
     * 分页获取待审核的小说列表
     * @param page 页码
     * @param size 每页大小
     * @return 待审核的小说分页列表
     */
    public Page<Novel> getPendingNovelsPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return novelRepository.findPendingNovels(pageable);
    }

    /**
     * 获取所有小说（用于管理）
     * @param keyword 搜索关键词
     * @param status 审核状态
     * @return 小说列表
     */
    public List<Novel> getAllNovels(String keyword, String status) {
        List<Novel> novels = novelRepository.findAll();

        // 按关键词过滤
        if (keyword != null && !keyword.isEmpty()) {
            String searchTerm = keyword.toLowerCase();
            novels = novels.stream()
                    .filter(n -> n.getTitle().toLowerCase().contains(searchTerm) ||
                            n.getAuthor().toLowerCase().contains(searchTerm))
                    .toList();
        }

        // 按审核状态过滤
        if (status != null && !status.isEmpty()) {
            novels = novels.stream()
                    .filter(n -> status.equals(n.getReviewStatus()))
                    .toList();
        }

        return novels;
    }

    /**
     * 分页获取所有小说（用于管理）
     * @param keyword 搜索关键词
     * @param status 审核状态
     * @param page 页码
     * @param size 每页大小
     * @return 小说分页列表
     */
    public Page<Novel> getAllNovelsPaginated(String keyword, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        
        // 如果有关键词或状态，先过滤再分页（使用内存分页）
        if ((keyword != null && !keyword.isEmpty()) || (status != null && !status.isEmpty())) {
            List<Novel> novels = getAllNovels(keyword, status);
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), novels.size());
            
            if (start >= novels.size()) {
                return new org.springframework.data.domain.PageImpl<>(novels, pageable, novels.size());
            }
            
            List<Novel> subList = novels.subList(start, end);
            // 批量加载字数
            loadNovelWordCountsBatch(subList);
            return new org.springframework.data.domain.PageImpl<>(subList, pageable, novels.size());
        }
        
        // 没有过滤条件，直接使用数据库分页
        Page<Novel> novelPage = novelRepository.findAll(pageable);
        // 批量加载字数
        loadNovelWordCountsBatch(novelPage.getContent());
        return novelPage;
    }
    
    /**
     * 批量为小说加载字数（从 NovelStats 表或章节表）
     * @param novels 小说列表
     */
    private void loadNovelWordCountsBatch(List<Novel> novels) {
        if (novels == null || novels.isEmpty()) {
            return;
        }
        
        // 获取所有小说 ID
        List<Long> novelIds = novels.stream().map(Novel::getId).toList();
        
        // 批量查询 NovelStats
        List<NovelStats> statsList = novelStatsRepository.findByNovelIdIn(novelIds);
        Map<Long, Integer> wordCountMap = new HashMap<>();
        
        for (NovelStats stats : statsList) {
            if (stats.getTotalWords() != null) {
                wordCountMap.put(stats.getNovelId(), stats.getTotalWords().intValue());
            }
        }
        
        // 为每个小说设置字数
        for (Novel novel : novels) {
            Integer wordCount = wordCountMap.get(novel.getId());
            if (wordCount != null) {
                novel.setWordCount(wordCount);
            } else {
                // 如果没有统计信息，计算章节总字数
                List<Chapter> chapters = chapterRepository.findByNovelId(novel.getId());
                int totalWords = chapters.stream()
                    .mapToInt(ch -> ch.getWordCount() != null ? ch.getWordCount() : 0)
                    .sum();
                novel.setWordCount(totalWords);
            }
        }
    }

    /**
     * 封禁小说
     * @param novelId 小说ID
     * @return 操作是否成功
     */
    public boolean banNovel(Long novelId) {
        Novel novel = novelRepository.findById(novelId).orElse(null);
        if (novel == null) {
            return false;
        }
        novel.setStatus("BANNED");
        novelRepository.save(novel);
        return true;
    }

    /**
     * 解封小说
     * @param novelId 小说ID
     * @return 操作是否成功
     */
    public boolean unbanNovel(Long novelId) {
        Novel novel = novelRepository.findById(novelId).orElse(null);
        if (novel == null) {
            return false;
        }
        novel.setStatus("ONGOING");
        novelRepository.save(novel);
        return true;
    }

    /**
     * 删除小说 (包括所有章节及相关数据)
     * @param novelId 小说ID
     * @return 操作是否成功
     */
    @Transactional
    public boolean deleteNovel(Long novelId) {
        if (!novelRepository.existsById(novelId)) {
            return false;
        }
        
        // 先删除小说相关的评论
        commentRepository.deleteByNovelId(novelId);
        
        // 删除小说相关的书签
        bookmarkRepository.deleteByNovelId(novelId);
        
        // 删除小说相关的阅读进度
        readingProgressRepository.deleteByNovelId(novelId);
        
        // 删除所有章节 (使用级联删除)
        chapterRepository.deleteByNovelId(novelId);
        
        // 最后删除小说
        novelRepository.deleteById(novelId);
        
        return true;
    }

    /**
     * 审核小说
     * @param novelId 小说ID
     * @param status 审核状态：APPROVED-通过, REJECTED-拒绝
     * @param reviewComment 审核意见
     * @return 操作是否成功
     */
    public boolean reviewNovel(Long novelId, String status, String reviewComment) {
        Novel novel = novelRepository.findById(novelId).orElse(null);
        if (novel == null) {
            return false;
        }
        // 更新审核状态：APPROVED-通过, REJECTED-拒绝
        novel.setReviewStatus(status);
        novel.setReviewComment(reviewComment);
        novel.setReviewedAt(new Date());
        
        // 审核通过的小说设置为上架状态
        if ("APPROVED".equals(status)) {
            novel.setStatus("ONGOING");
        } else if ("REJECTED".equals(status)) {
            // 审核失败的小说设置为草稿状态，不上架
            novel.setStatus("DRAFT");
        }
        
        novelRepository.save(novel);
        return true;
    }

    /**
     * 更新小说状态（连载状态和审核状态）
     * @param novelId 小说ID
     * @param status 连载状态
     * @param reviewStatus 审核状态
     * @return 操作是否成功
     */
    public boolean updateNovelStatus(Long novelId, String status, String reviewStatus) {
        Optional<Novel> optional = novelRepository.findById(novelId);
        if (optional.isEmpty()) {
            return false;
        }
        Novel novel = optional.get();
        if (status != null && !status.isEmpty()) {
            novel.setStatus(status);
        }
        if (reviewStatus != null && !reviewStatus.isEmpty()) {
            novel.setReviewStatus(reviewStatus);
        }
        novelRepository.save(novel);
        return true;
    }

    // ==================== 章节审核功能 ====================

    /**
     * 获取待审核的章节列表
     * @return 待审核的章节列表
     */
    public List<Chapter> getPendingChapters() {
        return chapterRepository.findPendingChapters();
    }

    /**
     * 分页获取待审核的章节列表
     * @param page 页码
     * @param size 每页大小
     * @return 待审核的章节分页列表
     */
    public Page<ChapterDTO> getPendingChaptersPaginated(int page, int size) {
        System.out.println("AdminService - getPendingChaptersPaginated called");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ChapterDTO> pageResult = chapterRepository.findPendingChapters(pageable);
        System.out.println("AdminService - Query completed, found " + pageResult.getTotalElements() + " chapters");
        return pageResult;
    }

    /**
     * 获取所有章节（用于管理）
     * @param keyword 搜索关键词
     * @param status 审核状态
     * @return 章节列表
     */
    public List<Chapter> getAllChapters(String keyword, String status) {
        List<Chapter> chapters = chapterRepository.findAll();

        // 按关键词过滤（按章节标题或小说标题）
        if (keyword != null && !keyword.isEmpty()) {
            String searchTerm = keyword.toLowerCase();
            chapters = chapters.stream()
                    .filter(c -> {
                        Novel novel = novelRepository.findById(c.getNovelId()).orElse(null);
                        boolean titleMatch = c.getTitle().toLowerCase().contains(searchTerm);
                        boolean novelTitleMatch = novel != null && novel.getTitle().toLowerCase().contains(searchTerm);
                        return titleMatch || novelTitleMatch;
                    })
                    .toList();
        }

        // 按审核状态过滤
        if (status != null && !status.isEmpty()) {
            chapters = chapters.stream()
                    .filter(c -> status.equals(c.getReviewStatus()))
                    .toList();
        }

        return chapters;
    }

    /**
     * 分页获取所有章节（用于管理）
     * @param keyword 搜索关键词
     * @param status 审核状态
     * @param page 页码
     * @param size 每页大小
     * @return 章节分页列表
     */
    public Page<Chapter> getAllChaptersPaginated(String keyword, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        
        Page<Chapter> pageResult;
        
        // 如果有关键词或状态，先过滤再分页（使用内存分页）
        if ((keyword != null && !keyword.isEmpty()) || (status != null && !status.isEmpty())) {
            List<Chapter> chapters = getAllChapters(keyword, status);
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), chapters.size());
            
            if (start >= chapters.size()) {
                pageResult = new org.springframework.data.domain.PageImpl<>(chapters, pageable, chapters.size());
            } else {
                List<Chapter> subList = chapters.subList(start, end);
                pageResult = new org.springframework.data.domain.PageImpl<>(subList, pageable, chapters.size());
            }
        } else {
            // 没有过滤条件，直接使用数据库分页
            pageResult = chapterRepository.findAll(pageable);
        }
        
        // 清除章节内容，减少数据传输量
        pageResult.getContent().forEach(chapter -> chapter.setContent(null));
        
        return pageResult;
    }

    /**
     * 根据ID获取章节详情（包含章节内容）
     * @param chapterId 章节ID
     * @return 章节详情
     */
    public Chapter getChapterById(Long chapterId) {
        return chapterRepository.findById(chapterId).orElse(null);
    }

    /**
     * 审核章节
     * @param chapterId 章节ID
     * @param status 审核状态："APPROVED"-通过, "PENDING"-不通过（退回待修改）
     * @param reviewComment 审核意见（不通过时必填）
     * @param reviewedBy 审核人ID
     * @return 操作是否成功
     */
    public boolean reviewChapter(Long chapterId, String status, String reviewComment, Long reviewedBy) {
        Chapter chapter = chapterRepository.findById(chapterId).orElse(null);
        if (chapter == null) {
            return false;
        }
        
        // 更新审核状态
        chapter.setReviewStatus(status);
        chapter.setReviewComment(reviewComment);
        chapter.setReviewedAt(new Date());
        chapter.setReviewedBy(reviewedBy);
        
        // 如果审核通过，将小说状态改为"连载中"，并检查是否需要更新小说的审核状态
        if ("APPROVED".equals(status)) {
            Novel novel = novelRepository.findById(chapter.getNovelId()).orElse(null);
            if (novel != null) {
                // 将小说状态改为"连载中"
                if ("COMPLETED".equals(novel.getStatus())) {
                    novel.setStatus("ONGOING");
                }
                
                // 检查该小说的所有章节是否都通过了审核
                List<Chapter> allChapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novel.getId());
                boolean allApproved = allChapters.stream()
                        .allMatch(c -> "APPROVED".equals(c.getReviewStatus()));
                
                // 如果所有章节都通过了审核，将小说的 reviewStatus 改为 APPROVED
                if (allApproved && allChapters.size() > 0) {
                    novel.setReviewStatus("APPROVED");
                }
                
                novelRepository.save(novel);
            }
        }
        
        // 如果审核不通过（退回待修改），设置一个标记，让作者知道需要修改
        if ("PENDING".equals(status)) {
            // 可以在这里添加额外的逻辑，比如发送通知给作者
            System.out.println("章节 " + chapterId + " 被退回修改，原因：" + reviewComment);
        }
        
        chapterRepository.save(chapter);
        return true;
    }

    /**
     * 将所有章节状态设置为通过（使用批量更新）
     * @return 更新的章节数量
     */
    @Transactional
    public int approveAllChapters() {
        return chapterRepository.updateAllChaptersToApproved();
    }

    // ==================== 分类与标签管理 ====================

    /**
     * 添加分类
     * @param name 分类名称
     * @param description 分类描述
     * @param icon 分类图标
     * @return 添加的分类
     */
    public Category addCategory(String name, String description, String icon) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setIcon(icon);
        return categoryRepository.save(category);
    }

    /**
     * 更新分类
     * @param categoryId 分类ID
     * @param name 分类名称
     * @param description 分类描述
     * @param icon 分类图标
     * @return 操作是否成功
     */
    public boolean updateCategory(Long categoryId, String name, String description, String icon) {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category == null) {
            return false;
        }
        category.setName(name);
        category.setDescription(description);
        category.setIcon(icon);
        categoryRepository.save(category);
        return true;
    }

    /**
     * 删除分类
     * @param categoryId 分类ID
     * @return 操作是否成功
     */
    public boolean deleteCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            return false;
        }
        categoryRepository.deleteById(categoryId);
        return true;
    }

    /**
     * 获取所有分类
     * @return 分类列表
     */
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * 获取分页分类列表
     * @param page 页码
     * @param size 每页大小
     * @return 分类分页列表
     */
    public Page<Category> getAllCategories(int page, int size) {
        return categoryRepository.findAll(PageRequest.of(page, size));
    }

    /**
     * 获取分类统计
     * @return 分类统计数据
     */
    public Map<String, Object> getCategoryStats() {
        Map<String, Object> stats = new HashMap<>();
        List<Category> categories = categoryRepository.findAll();
        
        for (Category category : categories) {
            long count = novelRepository.findByCategory(category.getName()).size();
            stats.put(category.getName(), count);
        }
        
        return stats;
    }

    // ==================== 用户与权限管理 ====================

    /**
     * 获取所有用户
     * @param page 页码
     * @param size 每页大小
     * @return 用户分页列表
     */
    public Page<User> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return userRepository.findAll(pageable);
    }

    /**
     * 搜索用户
     * @param keyword 搜索关键词
     * @return 用户列表
     */
    public List<User> searchUsers(String keyword) {
        return userRepository.findByUsernameContaining(keyword);
    }

    /**
     * 封禁用户
     * @param userId 用户ID
     * @return 操作是否成功
     */
    public boolean banUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        user.setIsBanned(true);
        userRepository.save(user);
        return true;
    }

    /**
     * 解封用户
     * @param userId 用户ID
     * @return 操作是否成功
     */
    public boolean unbanUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        user.setIsBanned(false);
        userRepository.save(user);
        return true;
    }

    /**
     * 删除所有用户
     */
    public void deleteAllUsers() {
        userRepository.deleteAll();
    }

    /**
     * 删除单个用户
     * @param userId 用户ID
     * @return 操作是否成功
     */
    @Transactional
    public boolean deleteUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        // 不能删除管理员账号
        if ("ADMIN".equals(user.getRole())) {
            return false;
        }

        // 先删除用户关联的数据
        // 1. 删除阅读进度
        readingProgressRepository.findByUserIdOrderByLastReadAtDesc(userId).forEach(rp -> readingProgressRepository.delete(rp));

        // 2. 删除书签
        bookmarkRepository.deleteByUserId(userId);

        // 3. 删除评论点赞
        commentLikeRepository.findByUserId(userId).forEach(cl -> commentLikeRepository.delete(cl));

        // 4. 删除用户行为记录
        userBehaviorRepository.deleteByUserId(userId);

        // 5. 删除草稿章节（如果是作者）
        draftChapterRepository.findByAuthorIdOrderByUpdatedAtDesc(userId).forEach(dc -> draftChapterRepository.delete(dc));

        // 6. 删除书单
        bookListRepository.findByUserIdOrderByCreatedAtDesc(userId).forEach(bl -> bookListRepository.delete(bl));

        // 7. 删除评论
        commentRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 1000)).forEach(c -> commentRepository.delete(c));

        // 8. 删除小说（如果是作者）- 先删除相关章节和评论
        List<Novel> novels = novelRepository.findByAuthorId(userId);
        for (Novel novel : novels) {
            // 删除小说相关的评论
            commentRepository.deleteByNovelId(novel.getId());
            // 删除小说章节
            // chapterRepository.deleteByNovelId(novel.getId());
            // 删除小说
            novelRepository.delete(novel);
        }

        // 最后删除用户
        userRepository.deleteById(userId);
        return true;
    }

    /**
     * 创建用户（支持 USER, AUTHOR, ADMIN 角色）
     * @param username 用户名
     * @param password 密码
     * @param email 邮箱
     * @param role 角色
     * @return 创建的用户
     */
    public User createUser(String username, String password, String email, String role) {
        // 验证角色是否有效
        if (!Arrays.asList("USER", "AUTHOR", "ADMIN").contains(role)) {
            return null;
        }
        // 检查用户名是否已存在
        if (userRepository.findByUsername(username).isPresent()) {
            return null;
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        // 如果邮箱为空字符串，设置为 null 避免唯一约束冲突
        user.setEmail((email == null || email.trim().isEmpty()) ? null : email.trim());
        user.setRole(role);
        user.setIsBanned(false);
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());

        return userRepository.save(user);
    }

    /**
     * 设置用户角色（作者权限）
     * @param userId 用户ID
     * @param role 角色
     * @return 操作是否成功
     */
    public boolean setUserRole(Long userId, String role) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        // 验证角色是否有效
        if (!Arrays.asList("USER", "AUTHOR", "ADMIN").contains(role)) {
            return false;
        }
        user.setRole(role);
        user.setUpdatedAt(new Date());
        userRepository.save(user);
        return true;
    }

    // ==================== 数据统计与分析 ====================

    /**
     * 获取平台统计数据
     * @return 平台统计数据
     */
    public Map<String, Object> getPlatformStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 用户统计
        stats.put("totalUsers", userRepository.count());
        stats.put("activeUsers", userRepository.count()); // 简化实现
        
        // 小说统计
        stats.put("totalNovels", novelRepository.count());
        
        // 总阅读量 - 统计所有小说的阅读量
        long totalViews = 0;
        List<Novel> novels = novelRepository.findAll();
        for (Novel novel : novels) {
            totalViews += novel.getViews() != null ? novel.getViews() : 0;
        }
        stats.put("totalViews", totalViews);
        
        // 评论统计
        stats.put("totalComments", commentRepository.count());
        
        // 分类统计
        stats.put("categories", getCategoryStats());
        
        return stats;
    }

    /**
     * 获取阅读行为报告
     * @return 阅读行为报告
     */
    public Map<String, Object> getReadingBehaviorReport() {
        Map<String, Object> report = new HashMap<>();
        
        // 获取最近30天的行为数据
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        Date since = cal.getTime();
        
        List<Object[]> popularNovels = userBehaviorRepository.findPopularNovels(since);
        report.put("popularNovels", popularNovels);
        
        // 热门分类
        Map<String, Object> categoryStats = getCategoryStats();
        report.put("categoryStats", categoryStats);
        
        return report;
    }

    /**
     * 获取热门题材趋势
     * @return 热门题材趋势
     */
    public List<Map<String, Object>> getTrendingGenres() {
        List<Map<String, Object>> trends = new ArrayList<>();
        
        // 简化实现：返回分类统计
        Map<String, Object> categoryStats = getCategoryStats();
        for (Map.Entry<String, Object> entry : categoryStats.entrySet()) {
            Map<String, Object> trend = new HashMap<>();
            trend.put("genre", entry.getKey());
            trend.put("count", entry.getValue());
            trends.add(trend);
        }
        
        return trends;
    }

    /**
     * 获取所有评论
     * @return 评论列表
     */
    public List<Map<String, Object>> getAllComments() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Comment> comments = commentRepository.findAll();
        
        for (Comment comment : comments) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", comment.getId());
            map.put("content", comment.getContent());
            map.put("userId", comment.getUserId());
            map.put("novelId", comment.getNovelId());
            map.put("chapterId", comment.getChapterId());
            map.put("parentId", comment.getParentId());
            map.put("createdAt", comment.getCreatedAt());
            map.put("updatedAt", comment.getUpdatedAt());
            
            // 获取用户信息
            User user = userRepository.findById(comment.getUserId()).orElse(null);
            if (user != null) {
                map.put("userName", user.getUsername());
            }
            
            // 获取小说信息
            Novel novel = novelRepository.findById(comment.getNovelId()).orElse(null);
            if (novel != null) {
                map.put("novelTitle", novel.getTitle());
            }
            
            result.add(map);
        }
        
        return result;
    }

    /**
     * 分页获取所有评论
     * @param page 页码
     * @param size 每页大小
     * @return 评论分页列表
     */
    public Page<Map<String, Object>> getAllCommentsPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> commentPage = commentRepository.findAll(pageable);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Comment comment : commentPage.getContent()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", comment.getId());
            map.put("content", comment.getContent());
            map.put("userId", comment.getUserId());
            map.put("novelId", comment.getNovelId());
            map.put("chapterId", comment.getChapterId());
            map.put("parentId", comment.getParentId());
            map.put("createdAt", comment.getCreatedAt());
            map.put("updatedAt", comment.getUpdatedAt());
            
            // 获取用户信息
            User user = userRepository.findById(comment.getUserId()).orElse(null);
            if (user != null) {
                map.put("userName", user.getUsername());
            }
            
            // 获取小说信息
            Novel novel = novelRepository.findById(comment.getNovelId()).orElse(null);
            if (novel != null) {
                map.put("novelTitle", novel.getTitle());
            }
            
            result.add(map);
        }
        
        return new org.springframework.data.domain.PageImpl<>(result, pageable, commentPage.getTotalElements());
    }

    /**
     * 删除评论
     * @param commentId 评论ID
     * @return 操作是否成功
     */
    public boolean deleteComment(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            return false;
        }
        commentRepository.deleteById(commentId);
        return true;
    }

    /**
     * 删除所有小说及相关数据
     */
    public void deleteAllNovels() {
        commentRepository.deleteAll();
        bookmarkRepository.deleteAll();
        readingProgressRepository.deleteAll();
        chapterRepository.deleteAll();
        novelRepository.deleteAll();
    }
}
