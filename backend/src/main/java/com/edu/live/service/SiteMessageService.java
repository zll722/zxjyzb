package com.edu.live.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.dto.SiteMessageRequest;
import com.edu.live.entity.SiteMessage;
import com.edu.live.vo.SiteMessageVO;

public interface SiteMessageService {
    Page<SiteMessageVO> pageMyMessages(Long userId, long current, long size, Integer readStatus);

    SiteMessageVO detail(Long userId, Long id);

    SiteMessage send(Long senderId, SiteMessageRequest request);

    void sendToAllEnabledUsers(Long senderId, String title, String content);

    long unreadCount(Long userId);

    void markRead(Long userId, Long id);

    void markAllRead(Long userId);

    void delete(Long userId, Long id);
}
