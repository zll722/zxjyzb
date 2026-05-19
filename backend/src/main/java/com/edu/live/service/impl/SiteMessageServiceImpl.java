package com.edu.live.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.common.BusinessException;
import com.edu.live.dto.SiteMessageRequest;
import com.edu.live.entity.SiteMessage;
import com.edu.live.entity.User;
import com.edu.live.mapper.SiteMessageMapper;
import com.edu.live.mapper.UserMapper;
import com.edu.live.service.SiteMessageService;
import com.edu.live.vo.SiteMessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SiteMessageServiceImpl implements SiteMessageService {
    private final SiteMessageMapper siteMessageMapper;
    private final UserMapper userMapper;

    @Override
    public Page<SiteMessageVO> pageMyMessages(Long userId, long current, long size, Integer readStatus) {
        Page<SiteMessage> page = siteMessageMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<SiteMessage>()
                .eq(SiteMessage::getReceiverId, userId)
                .eq(readStatus != null, SiteMessage::getReadStatus, readStatus)
                .orderByAsc(SiteMessage::getReadStatus)
                .orderByDesc(SiteMessage::getCreatedAt));
        Page<SiteMessageVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toVOList(page.getRecords()));
        return result;
    }

    @Override
    public SiteMessageVO detail(Long userId, Long id) {
        SiteMessage message = requireReceiverMessage(userId, id);
        return toVO(message);
    }

    @Override
    public SiteMessage send(Long senderId, SiteMessageRequest request) {
        User receiver = userMapper.selectById(request.getReceiverId());
        if (receiver == null || !Objects.equals(receiver.getStatus(), 1)) {
            throw new BusinessException(404, "接收人不存在");
        }
        SiteMessage message = buildMessage(senderId, request.getReceiverId(), request.getTitle(), request.getContent());
        siteMessageMapper.insert(message);
        return message;
    }

    @Override
    @Transactional
    public void sendToAllEnabledUsers(Long senderId, String title, String content) {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getStatus, 1));
        for (User user : users) {
            siteMessageMapper.insert(buildMessage(senderId, user.getId(), title, content));
        }
    }

    @Override
    public long unreadCount(Long userId) {
        return siteMessageMapper.selectCount(new LambdaQueryWrapper<SiteMessage>()
                .eq(SiteMessage::getReceiverId, userId)
                .eq(SiteMessage::getReadStatus, 0));
    }

    @Override
    public void markRead(Long userId, Long id) {
        SiteMessage message = requireReceiverMessage(userId, id);
        if (!Objects.equals(message.getReadStatus(), 1)) {
            message.setReadStatus(1);
            message.setReadAt(LocalDateTime.now());
            siteMessageMapper.updateById(message);
        }
    }

    @Override
    public void markAllRead(Long userId) {
        siteMessageMapper.update(null, new LambdaUpdateWrapper<SiteMessage>()
                .eq(SiteMessage::getReceiverId, userId)
                .eq(SiteMessage::getReadStatus, 0)
                .set(SiteMessage::getReadStatus, 1)
                .set(SiteMessage::getReadAt, LocalDateTime.now()));
    }

    @Override
    public void delete(Long userId, Long id) {
        requireReceiverMessage(userId, id);
        siteMessageMapper.deleteById(id);
    }

    private SiteMessage buildMessage(Long senderId, Long receiverId, String title, String content) {
        SiteMessage message = new SiteMessage();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setTitle(title);
        message.setContent(content);
        message.setReadStatus(0);
        return message;
    }

    private SiteMessage requireReceiverMessage(Long userId, Long id) {
        SiteMessage message = siteMessageMapper.selectOne(new LambdaQueryWrapper<SiteMessage>()
                .eq(SiteMessage::getId, id)
                .eq(SiteMessage::getReceiverId, userId));
        if (message == null) {
            throw new BusinessException(404, "消息不存在");
        }
        return message;
    }

    private List<SiteMessageVO> toVOList(List<SiteMessage> messages) {
        if (messages.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = new HashSet<>();
        messages.forEach(message -> {
            userIds.add(message.getSenderId());
            userIds.add(message.getReceiverId());
        });
        Map<Long, User> users = userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return messages.stream().map(message -> fillVO(message, users.get(message.getSenderId()), users.get(message.getReceiverId()))).toList();
    }

    private SiteMessageVO toVO(SiteMessage message) {
        User sender = userMapper.selectById(message.getSenderId());
        User receiver = userMapper.selectById(message.getReceiverId());
        return fillVO(message, sender, receiver);
    }

    private SiteMessageVO fillVO(SiteMessage message, User sender, User receiver) {
        SiteMessageVO vo = new SiteMessageVO();
        vo.setId(message.getId());
        vo.setSenderId(message.getSenderId());
        vo.setSenderName(sender == null ? null : sender.getUsername());
        vo.setReceiverId(message.getReceiverId());
        vo.setReceiverName(receiver == null ? null : receiver.getUsername());
        vo.setTitle(message.getTitle());
        vo.setContent(message.getContent());
        vo.setReadStatus(message.getReadStatus());
        vo.setReadAt(message.getReadAt());
        vo.setCreatedAt(message.getCreatedAt());
        return vo;
    }
}
