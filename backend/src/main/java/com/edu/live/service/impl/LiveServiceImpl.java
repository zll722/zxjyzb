package com.edu.live.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.live.util.AgoraTokenUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.common.BusinessException;
import com.edu.live.config.AgoraProperties;
import com.edu.live.dto.LiveMessageRequest;
import com.edu.live.dto.LivePollRequest;
import com.edu.live.dto.LiveRoomRequest;
import com.edu.live.dto.LiveVoteRequest;
import com.edu.live.entity.BarrageKeyword;
import com.edu.live.entity.LiveBarrage;
import com.edu.live.entity.LiveChat;
import com.edu.live.entity.LiveMicRequest;
import com.edu.live.entity.LivePoll;
import com.edu.live.entity.LivePollVote;
import com.edu.live.entity.LiveRoom;
import com.edu.live.entity.User;
import com.edu.live.enums.LiveRoomStatus;
import com.edu.live.mapper.BarrageKeywordMapper;
import com.edu.live.mapper.LiveBarrageMapper;
import com.edu.live.mapper.LiveChatMapper;
import com.edu.live.mapper.LiveMicRequestMapper;
import com.edu.live.mapper.LivePollMapper;
import com.edu.live.mapper.LivePollVoteMapper;
import com.edu.live.mapper.LiveRoomMapper;
import com.edu.live.mapper.UserMapper;
import com.edu.live.service.LiveReplayService;
import com.edu.live.service.LiveService;
import com.edu.live.vo.LiveMicRequestVO;
import com.edu.live.vo.LivePollVO;
import com.edu.live.vo.LiveRoomVO;
import com.edu.live.websocket.LiveWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LiveServiceImpl implements LiveService {
    private final LiveRoomMapper liveRoomMapper;
    private final LiveChatMapper liveChatMapper;
    private final LiveBarrageMapper liveBarrageMapper;
    private final BarrageKeywordMapper barrageKeywordMapper;
    private final LivePollMapper livePollMapper;
    private final LivePollVoteMapper livePollVoteMapper;
    private final LiveMicRequestMapper liveMicRequestMapper;
    private final UserMapper userMapper;
    private final LiveReplayService liveReplayService;
    private final AgoraProperties agoraProperties;
    private final LiveWebSocketHandler liveWebSocketHandler;

    @Override
    public Page<LiveRoomVO> pageRooms(long current, long size, String status, Long teacherId) {
        Page<LiveRoom> page = liveRoomMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<LiveRoom>()
                .eq(StringUtils.hasText(status), LiveRoom::getStatus, status)
                .eq(teacherId != null, LiveRoom::getTeacherId, teacherId)
                .orderByDesc(LiveRoom::getCreatedAt));
        Page<LiveRoomVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    @Override
    public LiveRoomVO detail(Long id) {
        LiveRoom room = liveRoomMapper.selectById(id);
        if (room == null) {
            throw new BusinessException(404, "直播间不存在");
        }
        return toVO(room);
    }

    @Override
    public LiveRoom createRoom(Long teacherId, LiveRoomRequest request) {
        LiveRoom room = new LiveRoom();
        room.setTitle(request.getTitle());
        room.setCover(request.getCover());
        room.setIntro(request.getIntro());
        room.setTeacherId(teacherId);
        room.setStatus(LiveRoomStatus.SCHEDULED.name());
        room.setScheduledTime(request.getScheduledTime());
        room.setOnlineCount(0);
        liveRoomMapper.insert(room);
        return room;
    }

    @Override
    public LiveRoom startRoom(Long teacherId, Long roomId) {
        LiveRoom room = requireTeacherRoom(teacherId, roomId);
        room.setStatus(LiveRoomStatus.LIVE.name());
        room.setStartTime(LocalDateTime.now());
        liveRoomMapper.updateById(room);
        return room;
    }

    @Override
    public LiveRoom endRoom(Long teacherId, Long roomId) {
        LiveRoom room = requireTeacherRoom(teacherId, roomId);
        room.setStatus(LiveRoomStatus.ENDED.name());
        room.setEndTime(LocalDateTime.now());
        room.setOnlineCount(0);
        liveRoomMapper.updateById(room);
        liveReplayService.ensureReplayForEndedRoom(room.getId());
        liveWebSocketHandler.broadcast(room.getId(), Map.of("type", "room_ended", "roomId", room.getId()));
        return room;
    }

    @Override
    public void forceClose(Long roomId) {
        LiveRoom room = liveRoomMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException(404, "直播间不存在");
        }
        room.setStatus(LiveRoomStatus.ENDED.name());
        room.setEndTime(LocalDateTime.now());
        room.setOnlineCount(0);
        liveRoomMapper.updateById(room);
        liveReplayService.ensureReplayForEndedRoom(room.getId());
        liveWebSocketHandler.broadcast(roomId, Map.of("type", "room_ended", "roomId", roomId));
    }

    @Override
    public LiveChat sendChat(Long userId, Long roomId, LiveMessageRequest request) {
        requireRoom(roomId);
        rejectBlockedContent(request.getContent());
        String content = escapeHtml(request.getContent());
        User user = userMapper.selectById(userId);
        LiveChat chat = new LiveChat();
        chat.setRoomId(roomId);
        chat.setUserId(userId);
        chat.setMessage(content);
        liveChatMapper.insert(chat);
        // 广播聊天消息给房间内所有 WebSocket 连接
        liveWebSocketHandler.broadcast(roomId, java.util.Map.of(
                "type", "chat",
                "roomId", roomId,
                "userId", userId,
                "username", user == null ? "匿名" : user.getUsername(),
                "content", content
        ));
        return chat;
    }

    @Override
    public LiveBarrage sendBarrage(Long userId, Long roomId, LiveMessageRequest request) {
        requireRoom(roomId);
        rejectBlockedContent(request.getContent());
        String content = escapeHtml(request.getContent());
        User user = userMapper.selectById(userId);
        LiveBarrage barrage = new LiveBarrage();
        barrage.setRoomId(roomId);
        barrage.setUserId(userId);
        barrage.setContent(content);
        liveBarrageMapper.insert(barrage);
        liveWebSocketHandler.broadcast(roomId, java.util.Map.of("type", "barrage", "roomId", roomId, "userId", userId, "username", user == null ? "匿名" : user.getUsername(), "content", content));
        return barrage;
    }

    @Override
    public LivePoll createPoll(Long teacherId, Long roomId, LivePollRequest request) {
        requireTeacherRoom(teacherId, roomId);
        LivePoll poll = new LivePoll();
        poll.setRoomId(roomId);
        poll.setQuestion(request.getQuestion());
        poll.setOptions(JSONUtil.toJsonStr(request.getOptions()));
        livePollMapper.insert(poll);
        LivePollVO pollVO = toPollVO(poll, null);
        liveWebSocketHandler.broadcast(roomId, Map.of("type", "poll", "roomId", roomId, "poll", pollVO));
        return poll;
    }

    @Override
    public void dismissPoll(Long teacherId, Long pollId) {
        LivePoll poll = requirePoll(pollId);
        requireTeacherRoom(teacherId, poll.getRoomId());
        liveWebSocketHandler.broadcast(poll.getRoomId(),
                Map.of("type", "poll_dismiss", "roomId", poll.getRoomId(), "pollId", pollId));
    }

    @Override
    public List<LivePollVO> listPolls(Long roomId, Long userId) {
        requireRoom(roomId);
        return livePollMapper.selectList(new LambdaQueryWrapper<LivePoll>()
                .eq(LivePoll::getRoomId, roomId)
                .orderByDesc(LivePoll::getCreatedAt))
                .stream()
                .map(poll -> toPollVO(poll, userId))
                .toList();
    }

    @Override
    public LivePollVO pollDetail(Long pollId, Long userId) {
        LivePoll poll = requirePoll(pollId);
        return toPollVO(poll, userId);
    }

    @Override
    public LivePollVO pollResults(Long pollId, Long userId) {
        LivePoll poll = requirePoll(pollId);
        return toPollVO(poll, userId);
    }

    @Override
    public LivePollVote vote(Long userId, Long pollId, LiveVoteRequest request) {
        LivePoll poll = requirePoll(pollId);
        List<String> options = parseOptions(poll.getOptions());
        if (request.getOptionIndex() < 0 || request.getOptionIndex() >= options.size()) {
            throw new BusinessException(400, "投票选项不存在");
        }
        LivePollVote vote = livePollVoteMapper.selectOne(new LambdaQueryWrapper<LivePollVote>()
                .eq(LivePollVote::getPollId, pollId)
                .eq(LivePollVote::getUserId, userId));
        if (vote == null) {
            vote = new LivePollVote();
            vote.setPollId(pollId);
            vote.setUserId(userId);
        }
        vote.setOptionIndex(request.getOptionIndex());
        if (vote.getId() == null) {
            livePollVoteMapper.insert(vote);
        } else {
            livePollVoteMapper.updateById(vote);
        }
        // 广播时不携带 selectedOptionIndex（避免其他学生误判为自己已投票）
        LivePollVO pollVO = toPollVO(poll, null);
        liveWebSocketHandler.broadcast(poll.getRoomId(), Map.of("type", "poll_result", "roomId", poll.getRoomId(), "poll", pollVO));
        return vote;
    }

    @Override
    public LiveMicRequest requestMic(Long userId, Long roomId) {
        requireRoom(roomId);
        LiveMicRequest micRequest = new LiveMicRequest();
        micRequest.setRoomId(roomId);
        micRequest.setUserId(userId);
        micRequest.setStatus("PENDING");
        liveMicRequestMapper.insert(micRequest);
        liveWebSocketHandler.broadcast(roomId, Map.of("type", "mic_request", "roomId", roomId, "request", toMicRequestVO(micRequest)));
        return micRequest;
    }

    @Override
    public List<LiveMicRequestVO> listMicRequests(Long teacherId, Long roomId) {
        requireTeacherRoom(teacherId, roomId);
        return liveMicRequestMapper.selectList(new LambdaQueryWrapper<LiveMicRequest>()
                .eq(LiveMicRequest::getRoomId, roomId)
                .orderByDesc(LiveMicRequest::getCreatedAt))
                .stream()
                .map(this::toMicRequestVO)
                .toList();
    }

    @Override
    public LiveMicRequest handleMic(Long teacherId, Long requestId, boolean approved) {
        LiveMicRequest request = liveMicRequestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException(404, "连麦申请不存在");
        }
        requireTeacherRoom(teacherId, request.getRoomId());
        request.setStatus(approved ? "APPROVED" : "REJECTED");
        liveMicRequestMapper.updateById(request);
        liveWebSocketHandler.broadcast(request.getRoomId(), Map.of("type", "mic_response", "roomId", request.getRoomId(), "request", toMicRequestVO(request), "requestId", requestId, "userId", request.getUserId(), "approved", approved));
        return request;
    }

    @Override
    public LiveMicRequest endMic(Long userId, Long requestId) {
        LiveMicRequest request = liveMicRequestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException(404, "连麦申请不存在");
        }
        LiveRoom room = requireRoom(request.getRoomId());
        if (!room.getTeacherId().equals(userId) && !request.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权结束该连麦");
        }
        request.setStatus("ENDED");
        liveMicRequestMapper.updateById(request);
        liveWebSocketHandler.broadcast(request.getRoomId(), Map.of("type", "mic_end", "roomId", request.getRoomId(), "request", toMicRequestVO(request), "requestId", requestId, "userId", request.getUserId()));
        return request;
    }

    private void rejectBlockedContent(String content) {
        if (!StringUtils.hasText(content)) {
            return;
        }
        String text = content.toLowerCase();
        List<BarrageKeyword> keywords = barrageKeywordMapper.selectList(new LambdaQueryWrapper<BarrageKeyword>());
        keywords.stream()
                .map(BarrageKeyword::getKeyword)
                .filter(StringUtils::hasText)
                .filter(keyword -> text.contains(keyword.toLowerCase()))
                .findFirst()
                .ifPresent(keyword -> {
                    throw new BusinessException(400, "内容包含屏蔽词，发送失败");
                });
    }

    private String escapeHtml(String content) {
        if (content == null) {
            return "";
        }
        return content.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private LiveRoom requireRoom(Long roomId) {
        LiveRoom room = liveRoomMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException(404, "直播间不存在");
        }
        return room;
    }

    private LivePoll requirePoll(Long pollId) {
        LivePoll poll = livePollMapper.selectById(pollId);
        if (poll == null) {
            throw new BusinessException(404, "投票不存在");
        }
        return poll;
    }

    private LiveRoom requireTeacherRoom(Long teacherId, Long roomId) {
        LiveRoom room = requireRoom(roomId);
        if (!Objects.equals(room.getTeacherId(), teacherId)) {
            throw new BusinessException(403, "只能操作自己的直播间");
        }
        return room;
    }

    private LivePollVO toPollVO(LivePoll poll, Long userId) {
        List<String> options = parseOptions(poll.getOptions());
        Map<Integer, Long> voteCounts = livePollVoteMapper.selectList(new LambdaQueryWrapper<LivePollVote>()
                .eq(LivePollVote::getPollId, poll.getId()))
                .stream()
                .collect(Collectors.groupingBy(LivePollVote::getOptionIndex, Collectors.counting()));
        LivePollVO vo = new LivePollVO();
        vo.setId(poll.getId());
        vo.setRoomId(poll.getRoomId());
        vo.setQuestion(poll.getQuestion());
        vo.setOptions(options);
        vo.setCreatedAt(poll.getCreatedAt());
        vo.setTotalVotes(voteCounts.values().stream().mapToInt(Long::intValue).sum());
        vo.setResults(java.util.stream.IntStream.range(0, options.size()).mapToObj(index -> {
            LivePollVO.PollOptionResultVO result = new LivePollVO.PollOptionResultVO();
            result.setOptionIndex(index);
            result.setOptionText(options.get(index));
            result.setCount(voteCounts.getOrDefault(index, 0L));
            return result;
        }).toList());
        if (userId != null) {
            LivePollVote vote = livePollVoteMapper.selectOne(new LambdaQueryWrapper<LivePollVote>()
                    .eq(LivePollVote::getPollId, poll.getId())
                    .eq(LivePollVote::getUserId, userId));
            vo.setSelectedOptionIndex(vote == null ? null : vote.getOptionIndex());
        }
        return vo;
    }

    private List<String> parseOptions(String options) {
        if (!StringUtils.hasText(options)) {
            return List.of();
        }
        return JSONUtil.toList(options, String.class);
    }

    private LiveMicRequestVO toMicRequestVO(LiveMicRequest micRequest) {
        User user = userMapper.selectById(micRequest.getUserId());
        LiveMicRequestVO vo = new LiveMicRequestVO();
        vo.setId(micRequest.getId());
        vo.setRoomId(micRequest.getRoomId());
        vo.setUserId(micRequest.getUserId());
        vo.setUsername(user == null ? null : user.getUsername());
        vo.setStatus(micRequest.getStatus());
        vo.setCreatedAt(micRequest.getCreatedAt());
        vo.setUpdatedAt(micRequest.getUpdatedAt());
        return vo;
    }

    private LiveRoomVO toVO(LiveRoom room) {
        User teacher = userMapper.selectById(room.getTeacherId());
        LiveRoomVO vo = new LiveRoomVO();
        vo.setId(room.getId());
        vo.setTitle(room.getTitle());
        vo.setCover(room.getCover());
        vo.setIntro(room.getIntro());
        vo.setTeacherId(room.getTeacherId());
        vo.setTeacherName(teacher == null ? null : teacher.getUsername());
        vo.setStatus(room.getStatus());
        vo.setScheduledTime(room.getScheduledTime());
        vo.setStartTime(room.getStartTime());
        vo.setEndTime(room.getEndTime());
        vo.setOnlineCount(room.getOnlineCount());
        String channel = "live_room_" + room.getId();
        String agoraToken = buildAgoraToken(channel, 0);
        vo.setAgoraAppId(agoraProperties.getAppId());
        vo.setAgoraChannel(channel);
        vo.setAgoraToken(agoraToken);
        return vo;
    }

    private String buildAgoraToken(String channel, int uid) {
        String appId   = agoraProperties.getAppId();
        String appCert = agoraProperties.getAppCertificate();
        if (!org.springframework.util.StringUtils.hasText(appId)
                || !org.springframework.util.StringUtils.hasText(appCert)) {
            return "";
        }
        int expire = agoraProperties.getTokenExpirationSeconds() != null
                ? agoraProperties.getTokenExpirationSeconds()
                : 86400;
        return AgoraTokenUtil.buildRtcToken(appId, appCert, channel, uid,
                AgoraTokenUtil.ROLE_PUBLISHER, expire, expire);
    }
}
