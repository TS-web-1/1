package com.novel.service;

import com.novel.model.UserBehavior;
import com.novel.repository.UserBehaviorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class UserBehaviorService {

    private final UserBehaviorRepository userBehaviorRepository;
    private final RedisCacheService redisCacheService;

    public UserBehaviorService(UserBehaviorRepository userBehaviorRepository, 
                               RedisCacheService redisCacheService) {
        this.userBehaviorRepository = userBehaviorRepository;
        this.redisCacheService = redisCacheService;
    }

    @Transactional
    public void recordBehavior(Long userId, Long novelId, String behaviorType) {
        recordBehavior(userId, novelId, null, behaviorType, 1.0, null);
    }

    @Transactional
    public void recordBehavior(Long userId, Long novelId, Long chapterId, String behaviorType) {
        recordBehavior(userId, novelId, chapterId, behaviorType, 1.0, null);
    }

    @Transactional
    public void recordBehavior(Long userId, Long novelId, String behaviorType, Double weight) {
        recordBehavior(userId, novelId, null, behaviorType, weight, null);
    }

    @Transactional
    public void recordBehavior(Long userId, Long novelId, Long chapterId, String behaviorType, 
                               Double weight, Integer readDuration) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setNovelId(novelId);
        behavior.setChapterId(chapterId);
        behavior.setBehaviorType(behaviorType);
        behavior.setWeight(weight != null ? weight : 1.0);
        behavior.setReadDuration(readDuration != null ? readDuration : 0);
        behavior.setCreatedAt(new Date());
        
        userBehaviorRepository.save(behavior);
        
        clearUserRecommendationCache(userId);
        
        System.out.println(String.format("[用户行为记录] 用户ID: %d, 小说ID: %d, 行为类型: %s, 权重: %.1f", 
            userId, novelId, behaviorType, behavior.getWeight()));
    }

    public void recordView(Long userId, Long novelId) {
        recordBehavior(userId, novelId, "VIEW", 1.0);
    }

    public void recordBookmark(Long userId, Long novelId) {
        recordBehavior(userId, novelId, "BOOKMARK", 5.0);
    }

    public void recordLike(Long userId, Long novelId) {
        recordBehavior(userId, novelId, "LIKE", 3.0);
    }

    public void recordComment(Long userId, Long novelId) {
        recordBehavior(userId, novelId, "COMMENT", 4.0);
    }

    public void recordReadTime(Long userId, Long novelId, Long chapterId, Integer readDurationSeconds) {
        double weight = readDurationSeconds * 0.1;
        recordBehavior(userId, novelId, chapterId, "READ_TIME", weight, readDurationSeconds);
    }

    public List<UserBehavior> getUserBehaviors(Long userId) {
        return userBehaviorRepository.findByUserId(userId);
    }

    public List<UserBehavior> getUserBehaviorsByNovel(Long userId, Long novelId) {
        return userBehaviorRepository.findByUserIdAndNovelId(userId, novelId);
    }

    private void clearUserRecommendationCache(Long userId) {
        try {
            redisCacheService.delete("personalized_recommendations_user_" + userId + "_limit_100");
            redisCacheService.delete("personalized_recommendations_user_" + userId + "_limit_10");
            System.out.println("[缓存清理] 已清除用户 " + userId + " 的推荐缓存");
        } catch (Exception e) {
            System.out.println("[缓存清理失败] " + e.getMessage());
        }
    }
}
