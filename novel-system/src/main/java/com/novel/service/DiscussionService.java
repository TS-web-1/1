package com.novel.service;

import com.novel.dto.DiscussionRequest;
import com.novel.model.Discussion;
import com.novel.model.User;
import com.novel.repository.DiscussionRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiscussionService {

    private final DiscussionRepository discussionRepository;

    public DiscussionService(DiscussionRepository discussionRepository) {
        this.discussionRepository = discussionRepository;
    }

    public Discussion createTopic(DiscussionRequest request, User user) {
        Discussion topic = new Discussion();
        topic.setUser(user);
        topic.setTitle(request.getTitle());
        topic.setContent(request.getContent());

        return discussionRepository.save(topic);
    }

    public Discussion getTopicById(Long id) {
        return discussionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("主题不存在"));
    }

    public List<Discussion> getAllTopics() {
        return discussionRepository.findAll();
    }

    public Page<Discussion> getTopicsPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return discussionRepository.findAll(pageable);
    }

    public List<Discussion> getTopicsByUser(User user) {
        return discussionRepository.findByUserId(user.getId());
    }

    public void deleteTopic(Long id, User user) {
        Discussion topic = getTopicById(id);
        if (!topic.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("无权删除此主题");
        }
        discussionRepository.deleteById(id);
    }

    @Transactional
    public void deleteTopicById(Long id) {
        discussionRepository.deleteById(id);
    }
}
