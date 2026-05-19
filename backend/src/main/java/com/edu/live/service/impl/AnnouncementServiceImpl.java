package com.edu.live.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.common.BusinessException;
import com.edu.live.dto.AnnouncementRequest;
import com.edu.live.entity.Announcement;
import com.edu.live.entity.User;
import com.edu.live.mapper.AnnouncementMapper;
import com.edu.live.mapper.UserMapper;
import com.edu.live.service.AnnouncementService;
import com.edu.live.service.SiteMessageService;
import com.edu.live.vo.AnnouncementVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_OFFLINE = "OFFLINE";

    private final AnnouncementMapper announcementMapper;
    private final UserMapper userMapper;
    private final SiteMessageService siteMessageService;

    @Override
    public Page<AnnouncementVO> pagePublic(long current, long size, String keyword, String type) {
        Page<Announcement> page = announcementMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, STATUS_PUBLISHED)
                .eq(StringUtils.hasText(type), Announcement::getType, type)
                .le(Announcement::getPublishTime, LocalDateTime.now())
                .like(StringUtils.hasText(keyword), Announcement::getTitle, keyword)
                .orderByDesc(Announcement::getPinned)
                .orderByDesc(Announcement::getPublishTime)
                .orderByDesc(Announcement::getCreatedAt));
        Page<AnnouncementVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toVOList(page.getRecords()));
        return result;
    }

    @Override
    public AnnouncementVO publicDetail(Long id) {
        Announcement announcement = announcementMapper.selectOne(new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getId, id)
                .eq(Announcement::getStatus, STATUS_PUBLISHED)
                .le(Announcement::getPublishTime, LocalDateTime.now()));
        if (announcement == null) {
            throw new BusinessException(404, "公告不存在");
        }
        return toVO(announcement);
    }

    @Override
    public Page<AnnouncementVO> pageAdmin(long current, long size, String status, String keyword, String type) {
        Page<Announcement> page = announcementMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<Announcement>()
                .eq(StringUtils.hasText(status), Announcement::getStatus, status)
                .eq(StringUtils.hasText(type), Announcement::getType, type)
                .like(StringUtils.hasText(keyword), Announcement::getTitle, keyword)
                .orderByDesc(Announcement::getPinned)
                .orderByDesc(Announcement::getCreatedAt));
        Page<AnnouncementVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toVOList(page.getRecords()));
        return result;
    }

    @Override
    @Transactional
    public Announcement create(Long creatorId, AnnouncementRequest request) {
        validateRequest(request);
        Announcement announcement = new Announcement();
        fillAnnouncement(announcement, request);
        announcement.setCreatorId(creatorId);
        announcement.setPublishTime(request.getPublishTime() == null ? LocalDateTime.now() : request.getPublishTime());
        announcementMapper.insert(announcement);
        if (STATUS_PUBLISHED.equals(announcement.getStatus()) && Boolean.TRUE.equals(request.getSyncMessage())) {
            siteMessageService.sendToAllEnabledUsers(creatorId, announcement.getTitle(), announcement.getContent());
        }
        return announcement;
    }

    @Override
    @Transactional
    public Announcement update(Long id, AnnouncementRequest request) {
        validateRequest(request);
        Announcement announcement = requireAnnouncement(id);
        boolean needSync = !STATUS_PUBLISHED.equals(announcement.getStatus()) && STATUS_PUBLISHED.equals(request.getStatus()) && Boolean.TRUE.equals(request.getSyncMessage());
        fillAnnouncement(announcement, request);
        announcement.setPublishTime(request.getPublishTime() == null ? announcement.getPublishTime() : request.getPublishTime());
        announcementMapper.updateById(announcement);
        if (needSync) {
            siteMessageService.sendToAllEnabledUsers(announcement.getCreatorId(), announcement.getTitle(), announcement.getContent());
        }
        return announcement;
    }

    @Override
    @Transactional
    public Announcement publish(Long id, Boolean syncMessage) {
        Announcement announcement = requireAnnouncement(id);
        announcement.setStatus(STATUS_PUBLISHED);
        announcement.setPublishTime(LocalDateTime.now());
        announcementMapper.updateById(announcement);
        if (Boolean.TRUE.equals(syncMessage)) {
            siteMessageService.sendToAllEnabledUsers(announcement.getCreatorId(), announcement.getTitle(), announcement.getContent());
        }
        return announcement;
    }

    @Override
    public Announcement offline(Long id) {
        Announcement announcement = requireAnnouncement(id);
        announcement.setStatus(STATUS_OFFLINE);
        announcementMapper.updateById(announcement);
        return announcement;
    }

    @Override
    public void delete(Long id) {
        requireAnnouncement(id);
        announcementMapper.deleteById(id);
    }

    private void validateRequest(AnnouncementRequest request) {
        if (!Set.of(STATUS_DRAFT, STATUS_PUBLISHED, STATUS_OFFLINE).contains(request.getStatus())) {
            throw new BusinessException(400, "公告状态不合法");
        }
        if (request.getPinned() < 0) {
            throw new BusinessException(400, "置顶排序不能小于0");
        }
    }

    private void fillAnnouncement(Announcement announcement, AnnouncementRequest request) {
        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        announcement.setType(request.getType());
        announcement.setStatus(request.getStatus());
        announcement.setPinned(request.getPinned());
    }

    private Announcement requireAnnouncement(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new BusinessException(404, "公告不存在");
        }
        return announcement;
    }

    private List<AnnouncementVO> toVOList(List<Announcement> announcements) {
        if (announcements.isEmpty()) {
            return List.of();
        }
        Set<Long> creatorIds = announcements.stream().map(Announcement::getCreatorId).collect(Collectors.toSet());
        Map<Long, User> creators = userMapper.selectBatchIds(creatorIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return announcements.stream().map(announcement -> fillVO(announcement, creators.get(announcement.getCreatorId()))).toList();
    }

    private AnnouncementVO toVO(Announcement announcement) {
        return fillVO(announcement, userMapper.selectById(announcement.getCreatorId()));
    }

    private AnnouncementVO fillVO(Announcement announcement, User creator) {
        AnnouncementVO vo = new AnnouncementVO();
        vo.setId(announcement.getId());
        vo.setTitle(announcement.getTitle());
        vo.setContent(announcement.getContent());
        vo.setType(announcement.getType());
        vo.setStatus(announcement.getStatus());
        vo.setPinned(announcement.getPinned());
        vo.setCreatorId(announcement.getCreatorId());
        vo.setCreatorName(creator == null ? null : creator.getUsername());
        vo.setPublishTime(announcement.getPublishTime());
        vo.setCreatedAt(announcement.getCreatedAt());
        return vo;
    }
}
