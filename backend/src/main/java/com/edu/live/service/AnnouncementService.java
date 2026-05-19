package com.edu.live.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.dto.AnnouncementRequest;
import com.edu.live.entity.Announcement;
import com.edu.live.vo.AnnouncementVO;

public interface AnnouncementService {
    Page<AnnouncementVO> pagePublic(long current, long size, String keyword, String type);

    AnnouncementVO publicDetail(Long id);

    Page<AnnouncementVO> pageAdmin(long current, long size, String status, String keyword, String type);

    Announcement create(Long creatorId, AnnouncementRequest request);

    Announcement update(Long id, AnnouncementRequest request);

    Announcement publish(Long id, Boolean syncMessage);

    Announcement offline(Long id);

    void delete(Long id);
}
