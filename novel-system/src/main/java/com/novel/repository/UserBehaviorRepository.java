package com.novel.repository;

import com.novel.model.UserBehavior;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface UserBehaviorRepository extends JpaRepository<UserBehavior, Long> {

    // 获取用户的所有行为记录
    List<UserBehavior> findByUserId(Long userId);

    // 获取用户对特定小说的行为记录
    List<UserBehavior> findByUserIdAndNovelId(Long userId, Long novelId);

    // 获取特定时间范围内的行为记录
    List<UserBehavior> findByUserIdAndCreatedAtAfter(Long userId, Date date);

    // 获取用户最近的行为记录（按时间倒序）
    List<UserBehavior> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 统计用户对某类小说的行为次数
    @Query("SELECT ub.novelId, COUNT(ub) as count FROM UserBehavior ub WHERE ub.userId = :userId GROUP BY ub.novelId ORDER BY count DESC")
    List<Object[]> countBehaviorsByUserId(@Param("userId") Long userId);

    // 获取热门小说（根据行为次数）
    @Query("SELECT ub.novelId, SUM(ub.weight) as score FROM UserBehavior ub WHERE ub.createdAt > :since GROUP BY ub.novelId ORDER BY score DESC")
    List<Object[]> findPopularNovels(@Param("since") Date since);

    // 删除用户的所有行为记录
    void deleteByUserId(Long userId);

    // 根据小说ID查找行为记录
    List<UserBehavior> findByNovelId(Long novelId);

    // 根据小说ID和时间查找行为记录
    List<UserBehavior> findByNovelIdAndCreatedAtAfter(Long novelId, Date date);
}
