package com.novel.service;

import com.novel.model.Bookmark;
import com.novel.model.Novel;
import com.novel.model.UserBehavior;
import com.novel.repository.BookmarkRepository;
import com.novel.repository.NovelRepository;
import com.novel.repository.UserBehaviorRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final UserBehaviorRepository userBehaviorRepository;
    private final NovelRepository novelRepository;
    private final BookmarkRepository bookmarkRepository;
    private final RedisCacheService redisCacheService;

    public RecommendationService(UserBehaviorRepository userBehaviorRepository,
                                 NovelRepository novelRepository,
                                 BookmarkRepository bookmarkRepository,
                                 RedisCacheService redisCacheService) {
        this.userBehaviorRepository = userBehaviorRepository;
        this.novelRepository = novelRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.redisCacheService = redisCacheService;
    }

    // 行为权重配置
    private static final Map<String, Double> BEHAVIOR_WEIGHTS = new HashMap<>();
    static {
        BEHAVIOR_WEIGHTS.put("VIEW", 1.0);
        BEHAVIOR_WEIGHTS.put("BOOKMARK", 5.0);
        BEHAVIOR_WEIGHTS.put("LIKE", 3.0);
        BEHAVIOR_WEIGHTS.put("COMMENT", 4.0);
        BEHAVIOR_WEIGHTS.put("READ_TIME", 0.1); // 每10秒阅读时长为1个权重单位
    }

    /**
     * 混合推荐：协同过滤（收藏相似用户的书） + 基于内容（分类/标签/热度）
     * 冷启动处理：新用户返回热门推荐
     * 缓存支持：使用Redis缓存推荐结果
     */
    @SuppressWarnings("unchecked")
    public List<Novel> getPersonalizedRecommendations(Long userId, int limit) {
        System.out.println("\n========== 个性化推荐算法开始 ==========");
        System.out.println("用户 ID: " + userId);
        System.out.println("请求推荐数量：" + limit);
        
        // 先尝试从缓存获取（缓存键包含 limit 参数）
        Object cached = redisCacheService.get("personalized_recommendations_user_" + userId + "_limit_" + limit);
        if (cached != null) {
            System.out.println("【缓存命中】从 Redis 获取到推荐结果，共 " + ((List<Novel>)cached).size() + " 本");
            System.out.println("========== 推荐结束 ==========\n");
            return (List<Novel>) cached;
        }
        
        System.out.println("【缓存未命中】开始计算推荐...");
        
        List<UserBehavior> userBehaviors = userBehaviorRepository.findByUserId(userId);
        Set<Long> bookmarkedNovelIds = bookmarkRepository.findByUserId(userId).stream()
                .map(Bookmark::getNovelId)
                .collect(Collectors.toSet());
        
        System.out.println("\n【用户数据分析】");
        System.out.println("  - 用户行为数：" + userBehaviors.size());
        System.out.println("  - 收藏小说数：" + bookmarkedNovelIds.size());
        
        // 冷启动处理：如果用户没有行为数据和收藏数据，返回热门推荐
        if (userBehaviors.isEmpty() && bookmarkedNovelIds.isEmpty()) {
            System.out.println("\n【冷启动检测】新用户，返回热门推荐");
            List<Novel> popularRecommendations = getPopularRecommendations(limit);
            // 缓存结果
            redisCacheService.setPersonalizedRecommendations(userId, popularRecommendations);
            System.out.println("========== 推荐结束 ==========\n");
            return popularRecommendations;
        }
        
        System.out.println("\n【步骤 1】计算分类偏好...");
        Map<String, Double> categoryPreferences = calculateCategoryPreferences(userBehaviors);
        System.out.println("  分类偏好结果：");
        categoryPreferences.forEach((cat, score) -> 
            System.out.println("    - " + cat + ": " + String.format("%.4f", score)));
        
        System.out.println("\n【步骤 2】计算标签偏好...");
        Map<String, Double> tagPreferences = calculateTagPreferences(userBehaviors);
        System.out.println("  标签偏好结果：");
        tagPreferences.forEach((tag, score) -> 
            System.out.println("    - " + tag + ": " + String.format("%.4f", score)));
        
        Set<Long> interactedNovelIds = userBehaviors.stream()
                .map(UserBehavior::getNovelId)
                .collect(Collectors.toSet());
        interactedNovelIds.addAll(bookmarkedNovelIds);
        System.out.println("\n【步骤 3】已交互小说 ID 数量：" + interactedNovelIds.size());

        // 协同过滤：找"相似用户"（也收藏了当前用户收藏的小说），汇总他们收藏的其他小说
        System.out.println("\n【步骤 4】计算协同过滤分数...");
        Map<Long, Double> collaborativeScores = getCollaborativeScores(userId, bookmarkedNovelIds, interactedNovelIds);
        System.out.println("  协同过滤推荐小说数：" + collaborativeScores.size());
        if (!collaborativeScores.isEmpty()) {
            collaborativeScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> 
                    System.out.println("    - 小说 ID " + entry.getKey() + ": " + String.format("%.4f", entry.getValue())));
        }

        System.out.println("\n【步骤 5】获取候选小说列表...");
        List<Novel> allNovels = novelRepository.findAll();
        List<Novel> candidateNovels = allNovels.stream()
                .filter(novel -> !interactedNovelIds.contains(novel.getId()))
                .collect(Collectors.toList());
        System.out.println("  - 总小说数：" + allNovels.size());
        System.out.println("  - 候选小说数：" + candidateNovels.size());

        System.out.println("\n【步骤 6】计算混合推荐分数...");
        List<NovelScore> novelScores = candidateNovels.stream()
                .map(novel -> {
                    NovelScore contentScore = calculateNovelScore(novel, categoryPreferences, tagPreferences);
                    double collabScore = collaborativeScores.getOrDefault(novel.getId(), 0.0);
                    // 协同过滤分数归一化
                    double normalizedCollabScore = collabScore > 0 ? Math.min(1.0, collabScore / 10.0) : 0.0;
                    double mixed = contentScore.getScore() * 0.7 + normalizedCollabScore * 0.3;
                    return new NovelScore(novel, mixed);
                })
                .collect(Collectors.toList());

        System.out.println("\n【步骤 7】排序并返回 Top " + limit + "...");
        novelScores.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        // 打印前 10 名的详细分数
        System.out.println("\n【推荐结果详情】");
        novelScores.stream().limit(Math.min(10, novelScores.size())).forEach(ns -> {
            Novel n = ns.getNovel();
            System.out.println(String.format("  - [%d] %s - 分数：%.4f - 浏览：%d - 收藏：%d", 
                n.getId(), n.getTitle(), ns.getScore(), 
                n.getViews() != null ? n.getViews() : 0,
                n.getBookmarks() != null ? n.getBookmarks() : 0));
        });
        
        List<Novel> recommendations = novelScores.stream()
                .limit(limit)
                .map(NovelScore::getNovel)
                .collect(Collectors.toList());
        
        System.out.println("\n【缓存结果】将推荐结果存入 Redis");
        // 缓存结果（缓存键包含 limit 参数）
        redisCacheService.set("personalized_recommendations_user_" + userId + "_limit_" + limit, recommendations);
        
        System.out.println("\n========== 个性化推荐算法结束 ==========\n");
        
        return recommendations;
    }
    
    /**
     * 重磅推荐：按收藏数排序
     * 缓存支持：使用 Redis 缓存推荐结果
     */
    @SuppressWarnings("unchecked")
    public List<Novel> getBookmarkRecommendations(int limit) {
        System.out.println("\n【重磅推荐】尝试从缓存获取，limit=" + limit);
        
        // 先尝试从缓存获取（缓存键包含 limit 参数）
        Object cached = redisCacheService.get("bookmark_recommendations_limit_" + limit);
        if (cached != null) {
            System.out.println("【重磅推荐】缓存命中，返回 " + ((List<Novel>)cached).size() + " 本");
            return (List<Novel>) cached;
        }
        
        System.out.println("【重磅推荐】缓存未命中，从数据库查询...");
        System.out.println("\n========== 重磅推荐（按收藏数）开始 ==========");
        List<Novel> allNovels = novelRepository.findAll();
        System.out.println("数据库中共有 " + allNovels.size() + " 本小说");
        
        List<Novel> recommendations = allNovels.stream()
                .sorted((a, b) -> {
                    int bookmarksA = a.getBookmarks() != null ? a.getBookmarks() : 0;
                    int bookmarksB = b.getBookmarks() != null ? b.getBookmarks() : 0;
                    return Integer.compare(bookmarksB, bookmarksA); // 降序
                })
                .limit(limit)
                .collect(Collectors.toList());
        
        System.out.println("推荐小说数量：" + recommendations.size());
        recommendations.forEach(novel -> 
            System.out.println(String.format("  - [%d] %s - 收藏：%d", 
                novel.getId(), novel.getTitle(), 
                novel.getBookmarks() != null ? novel.getBookmarks() : 0)));
        System.out.println("========== 重磅推荐结束 ==========\n");
        
        // 缓存结果（缓存键包含 limit 参数）
        redisCacheService.set("bookmark_recommendations_limit_" + limit, recommendations);
        
        return recommendations;
    }
    
    /**
     * 热门推荐：按阅读历史记录数排序
     * 缓存支持：使用 Redis 缓存推荐结果
     */
    @SuppressWarnings("unchecked")
    public List<Novel> getReadingHistoryRecommendations(int limit) {
        System.out.println("\n【热门推荐】尝试从缓存获取，limit=" + limit);
        
        // 先尝试从缓存获取（缓存键包含 limit 参数）
        Object cached = redisCacheService.get("reading_history_recommendations_limit_" + limit);
        if (cached != null) {
            System.out.println("【热门推荐】缓存命中，返回 " + ((List<Novel>)cached).size() + " 本");
            return (List<Novel>) cached;
        }
        
        System.out.println("【热门推荐】缓存未命中，从数据库查询...");
        System.out.println("\n========== 热门推荐（按阅读历史）开始 ==========");
        List<Novel> allNovels = novelRepository.findAll();
        System.out.println("数据库中共有 " + allNovels.size() + " 本小说");
        
        // 统计每本小说的阅读历史记录数
        Map<Long, Long> novelViewCounts = new HashMap<>();
        List<UserBehavior> allBehaviors = userBehaviorRepository.findAll();
        for (UserBehavior behavior : allBehaviors) {
            if ("VIEW".equals(behavior.getBehaviorType())) {
                novelViewCounts.merge(behavior.getNovelId(), 1L, Long::sum);
            }
        }
        
        List<Novel> recommendations = allNovels.stream()
                .sorted((a, b) -> {
                    long viewsA = novelViewCounts.getOrDefault(a.getId(), 0L);
                    long viewsB = novelViewCounts.getOrDefault(b.getId(), 0L);
                    return Long.compare(viewsB, viewsA); // 降序
                })
                .limit(limit)
                .collect(Collectors.toList());
        
        System.out.println("推荐小说数量：" + recommendations.size());
        recommendations.forEach(novel -> 
            System.out.println(String.format("  - [%d] %s - 阅读次数：%d", 
                novel.getId(), novel.getTitle(), 
                novelViewCounts.getOrDefault(novel.getId(), 0L))));
        System.out.println("========== 热门推荐结束 ==========\n");
        
        // 缓存结果（缓存键包含 limit 参数）
        redisCacheService.set("reading_history_recommendations_limit_" + limit, recommendations);
        
        return recommendations;
    }
    
    /**
     * 完结精品：按观看人数与收藏数排序的完结小说
     * 缓存支持：使用 Redis 缓存推荐结果
     */
    @SuppressWarnings("unchecked")
    public List<Novel> getCompletedRecommendations(int limit) {
        System.out.println("\n【完结精品】尝试从缓存获取，limit=" + limit);
        
        // 先尝试从缓存获取（缓存键包含 limit 参数）
        Object cached = redisCacheService.get("completed_recommendations_limit_" + limit);
        if (cached != null) {
            System.out.println("【完结精品】缓存命中，返回 " + ((List<Novel>)cached).size() + " 本");
            return (List<Novel>) cached;
        }
        
        System.out.println("【完结精品】缓存未命中，从数据库查询...");
        System.out.println("\n========== 完结精品推荐开始 ==========");
        List<Novel> allNovels = novelRepository.findAll();
        System.out.println("数据库中共有 " + allNovels.size() + " 本小说");
        
        List<Novel> recommendations = allNovels.stream()
                .filter(novel -> "完结".equals(novel.getStatus()))
                .sorted((a, b) -> {
                    int viewsA = a.getViews() != null ? a.getViews() : 0;
                    int viewsB = b.getViews() != null ? b.getViews() : 0;
                    int bookmarksA = a.getBookmarks() != null ? a.getBookmarks() : 0;
                    int bookmarksB = b.getBookmarks() != null ? b.getBookmarks() : 0;
                    // 综合得分 = 浏览量 + 收藏量 * 10
                    int scoreA = viewsA + bookmarksA * 10;
                    int scoreB = viewsB + bookmarksB * 10;
                    return Integer.compare(scoreB, scoreA); // 降序
                })
                .limit(limit)
                .collect(Collectors.toList());
        
        System.out.println("完结小说总数：" + allNovels.stream().filter(n -> "完结".equals(n.getStatus())).count());
        System.out.println("推荐小说数量：" + recommendations.size());
        recommendations.forEach(novel -> 
            System.out.println(String.format("  - [%d] %s - 浏览：%d - 收藏：%d - 状态：%s", 
                novel.getId(), novel.getTitle(), 
                novel.getViews() != null ? novel.getViews() : 0,
                novel.getBookmarks() != null ? novel.getBookmarks() : 0,
                novel.getStatus())));
        System.out.println("========== 完结精品推荐结束 ==========\n");
        
        // 缓存结果（缓存键包含 limit 参数）
        redisCacheService.set("completed_recommendations_limit_" + limit, recommendations);
        
        return recommendations;
    }
    
    /**
     * 获取热门推荐（基于全局热门度）
     * 热门度 = 阅读量 × 0.6 + 收藏量 × 0.4
     * 缓存支持：使用Redis缓存推荐结果
     */
    @SuppressWarnings("unchecked")
    public List<Novel> getPopularRecommendations(int limit) {
        // 先尝试从缓存获取
        Object cached = redisCacheService.getPopularRecommendations();
        if (cached != null) {
            return (List<Novel>) cached;
        }
        
        List<Novel> allNovels = novelRepository.findAll();
        
        List<Novel> recommendations = allNovels.stream()
                .map(novel -> {
                    double popularityScore = calculateNormalizedPopularityScore(novel);
                    return new NovelScore(novel, popularityScore);
                })
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(limit)
                .map(NovelScore::getNovel)
                .collect(Collectors.toList());
        
        // 缓存结果
        redisCacheService.setPopularRecommendations(recommendations);
        
        return recommendations;
    }

    /**
     * 协同过滤：基于收藏找相似用户，返回他们收藏的（当前用户未读）小说的得分
     */
    private Map<Long, Double> getCollaborativeScores(Long userId, Set<Long> userBookmarkedNovelIds, Set<Long> excludeNovelIds) {
        Map<Long, Double> novelScores = new HashMap<>();
        if (userBookmarkedNovelIds.isEmpty()) return novelScores;
        Set<Long> similarUserIds = new HashSet<>();
        for (Long novelId : userBookmarkedNovelIds) {
            List<Bookmark> bookmarks = bookmarkRepository.findByNovelId(novelId);
            if (bookmarks != null) {
                for (Bookmark b : bookmarks) {
                    if (b != null && b.getUserId() != null && !userId.equals(b.getUserId())) {
                        similarUserIds.add(b.getUserId());
                    }
                }
            }
        }
        for (Long otherUserId : similarUserIds) {
            List<Bookmark> userBookmarks = bookmarkRepository.findByUserId(otherUserId);
            if (userBookmarks != null) {
                for (Bookmark b : userBookmarks) {
                    if (b != null && b.getNovelId() != null && !excludeNovelIds.contains(b.getNovelId())) {
                        novelScores.merge(b.getNovelId(), 1.0, Double::sum);
                    }
                }
            }
        }
        return novelScores;
    }

    /**
     * 计算用户对不同分类的偏好
     */
    private Map<String, Double> calculateCategoryPreferences(List<UserBehavior> userBehaviors) {
        Map<String, Double> categoryScores = new HashMap<>();
        
        for (UserBehavior behavior : userBehaviors) {
            Novel novel = novelRepository.findById(behavior.getNovelId()).orElse(null);
            if (novel != null && novel.getCategory() != null) {
                double weight = getBehaviorWeight(behavior);
                categoryScores.merge(novel.getCategory(), weight, Double::sum);
            }
        }
        
        // 归一化
        double totalScore = categoryScores.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalScore > 0) {
            categoryScores.replaceAll((k, v) -> v / totalScore);
        }
        
        return categoryScores;
    }

    /**
     * 计算用户对不同标签的偏好
     */
    private Map<String, Double> calculateTagPreferences(List<UserBehavior> userBehaviors) {
        Map<String, Double> tagScores = new HashMap<>();
        
        for (UserBehavior behavior : userBehaviors) {
            Novel novel = novelRepository.findById(behavior.getNovelId()).orElse(null);
            if (novel != null && novel.getTags() != null) {
                double weight = getBehaviorWeight(behavior);
                String[] tags = novel.getTags().split(",");
                for (String tag : tags) {
                    tag = tag.trim();
                    if (!tag.isEmpty()) {
                        tagScores.merge(tag, weight, Double::sum);
                    }
                }
            }
        }
        
        // 归一化
        double totalScore = tagScores.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalScore > 0) {
            tagScores.replaceAll((k, v) -> v / totalScore);
        }
        
        return tagScores;
    }

    /**
     * 计算小说的推荐分数
     */
    private NovelScore calculateNovelScore(Novel novel, Map<String, Double> categoryPreferences, Map<String, Double> tagPreferences) {
        double score = 0.0;
        double personalizationScore = 0.0;
        
        // 1. 分类匹配分数（权重 50%）
        if (novel.getCategory() != null && categoryPreferences.containsKey(novel.getCategory())) {
            double categoryScore = categoryPreferences.get(novel.getCategory());
            personalizationScore += categoryScore;
        }
        
        // 2. 标签匹配分数
        if (novel.getTags() != null && !novel.getTags().isEmpty()) {
            String[] tags = novel.getTags().split(",");
            double tagScore = 0.0;
            int matchedTags = 0;
            
            for (String tag : tags) {
                tag = tag.trim();
                if (!tag.isEmpty() && tagPreferences.containsKey(tag)) {
                    tagScore += tagPreferences.get(tag);
                    matchedTags++;
                }
            }
            
            if (matchedTags > 0) {
                personalizationScore += (tagScore / matchedTags);
            }
        }
        
        // 个性化分数权重 60%
        score += personalizationScore * 0.6;
        
        // 3. 热门度分数（权重 40%，归一化到 0-1 范围）
        double popularityScore = calculateNormalizedPopularityScore(novel);
        score += popularityScore * 0.4;
        
        return new NovelScore(novel, score);
    }

    /**
     * 计算归一化的热门度分数（0-1 范围）
     */
    private double calculateNormalizedPopularityScore(Novel novel) {
        int views = novel.getViews() != null ? novel.getViews() : 0;
        int bookmarks = novel.getBookmarks() != null ? novel.getBookmarks() : 0;
        double rating = novel.getRating() != null ? novel.getRating() : 0;
        
        // 使用 sigmoid 函数归一化，避免热门度主导分数
        // 浏览量归一化：100000 次浏览 = 0.9 分
        double viewsScore = 1.0 / (1.0 + Math.exp(-views / 30000.0));
        // 收藏量归一化：5000 次收藏 = 0.9 分
        double bookmarksScore = 1.0 / (1.0 + Math.exp(-bookmarks / 1500.0));
        // 评分归一化：已经是 0-5 范围
        double ratingScore = rating / 5.0;
        
        return viewsScore * 0.3 + bookmarksScore * 0.4 + ratingScore * 0.3;
    }

    /**
     * 获取行为的权重
     */
    private double getBehaviorWeight(UserBehavior behavior) {
        double baseWeight = BEHAVIOR_WEIGHTS.getOrDefault(behavior.getBehaviorType(), 1.0);
        
        // 对于阅读时长，根据实际时长计算权重
        if ("READ_TIME".equals(behavior.getBehaviorType()) && behavior.getReadDuration() != null) {
            return behavior.getReadDuration() * 0.1;
        }
        
        // 考虑行为权重字段
        if (behavior.getWeight() != null) {
            return baseWeight * behavior.getWeight();
        }
        
        return baseWeight;
    }

    /**
     * 小说分数包装类
     */
    private static class NovelScore {
        private final Novel novel;
        private final double score;

        public NovelScore(Novel novel, double score) {
            this.novel = novel;
            this.score = score;
        }

        public Novel getNovel() {
            return novel;
        }

        public double getScore() {
            return score;
        }
    }
}
