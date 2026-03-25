package com.novel.service;

import com.novel.model.BookList;
import com.novel.model.BookListItem;
import com.novel.model.Novel;
import com.novel.model.User;
import com.novel.repository.BookListRepository;
import com.novel.repository.BookListItemRepository;
import com.novel.repository.NovelRepository;
import com.novel.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BookListService {

    private final BookListRepository bookListRepository;
    private final BookListItemRepository bookListItemRepository;
    private final NovelRepository novelRepository;
    private final UserRepository userRepository;

    @Autowired
    public BookListService(BookListRepository bookListRepository, BookListItemRepository bookListItemRepository,
                          NovelRepository novelRepository, UserRepository userRepository) {
        this.bookListRepository = bookListRepository;
        this.bookListItemRepository = bookListItemRepository;
        this.novelRepository = novelRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BookList createBookList(BookList bookList, List<Long> novelIds) {
        // 保存书单
        BookList savedBookList = bookListRepository.save(bookList);
        
        // 保存书单中的小说
        if (novelIds != null && !novelIds.isEmpty()) {
            for (int i = 0; i < novelIds.size(); i++) {
                BookListItem item = new BookListItem();
                item.setBookListId(savedBookList.getId());
                item.setNovelId(novelIds.get(i));
                item.setSortOrder(i);
                bookListItemRepository.save(item);
            }
            savedBookList.setBookCount(novelIds.size());
            bookListRepository.save(savedBookList);
        }
        
        return savedBookList;
    }

    public List<BookList> getUserBookLists(Long userId) {
        return bookListRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<BookList> getPublicBookLists() {
        // 暂时使用 Pageable 的默认值，后续可以根据需要添加分页参数
        Page<BookList> page = bookListRepository.findByListTypeOrderByCreatedAtDesc("PUBLIC", org.springframework.data.domain.PageRequest.of(0, 20));
        return page != null ? page.getContent() : List.of();
    }

    public Page<BookList> getPublicBookListsPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BookList> result = bookListRepository.findByListTypeOrderByCreatedAtDesc("PUBLIC", pageable);
        return result != null ? result : Page.empty(pageable);
    }

    public Page<BookList> getAllBookListsPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<BookList> result = bookListRepository.findAll(pageable);
        return result != null ? result : Page.empty(pageable);
    }

    public BookList getBookListById(Long id) {
        return bookListRepository.findById(id).orElse(null);
    }

    public Map<String, Object> getBookListDetail(Long id) {
        BookList bookList = bookListRepository.findById(id).orElse(null);
        if (bookList == null) {
            return null;
        }
        
        // 获取书单中的小说 ID 列表
        List<BookListItem> items = bookListItemRepository.findByBookListId(id);
        List<Long> novelIds = items.stream()
                .map(BookListItem::getNovelId)
                .collect(Collectors.toList());
        
        // 根据小说 ID 获取小说详情
        List<Novel> novels = novelRepository.findAllById(novelIds);
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("id", bookList.getId());
        result.put("userId", bookList.getUserId());
        result.put("title", bookList.getTitle());
        result.put("description", bookList.getDescription());
        result.put("listType", bookList.getListType());
        result.put("bookCount", bookList.getBookCount());
        result.put("viewCount", bookList.getViewCount());
        result.put("collectCount", bookList.getCollectCount());
        result.put("likeCount", bookList.getLikeCount());
        result.put("createdAt", bookList.getCreatedAt());
        result.put("updatedAt", bookList.getUpdatedAt());
        
        // 添加用户信息
        User user = userRepository.findById(bookList.getUserId()).orElse(null);
        if (user != null) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            result.put("user", userInfo);
        }
        
        result.put("novels", novels);
        
        return result;
    }

    @Transactional
    public void deleteBookList(Long id) {
        // 先删除书单中的小说
        bookListItemRepository.deleteByBookListId(id);
        // 再删除书单
        bookListRepository.deleteById(id);
    }

    @Transactional
    public int makeAllBookListsPublic() {
        List<BookList> allBookLists = bookListRepository.findAll();
        int count = 0;
        for (BookList bookList : allBookLists) {
            if (!"PUBLIC".equals(bookList.getListType())) {
                bookList.setListType("PUBLIC");
                bookListRepository.save(bookList);
                count++;
            }
        }
        return count;
    }
}
