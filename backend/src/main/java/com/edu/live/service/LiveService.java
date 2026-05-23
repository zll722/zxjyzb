package com.edu.live.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.dto.LiveMessageRequest;
import com.edu.live.dto.LivePollRequest;
import com.edu.live.dto.LiveRoomRequest;
import com.edu.live.dto.LiveVoteRequest;
import com.edu.live.entity.LiveBarrage;
import com.edu.live.entity.LiveChat;
import com.edu.live.entity.LiveMicRequest;
import com.edu.live.entity.LivePoll;
import com.edu.live.entity.LivePollVote;
import com.edu.live.entity.LiveRoom;
import com.edu.live.vo.LiveMicRequestVO;
import com.edu.live.vo.LivePollVO;
import com.edu.live.vo.LiveRoomVO;

import java.util.List;

public interface LiveService {
    Page<LiveRoomVO> pageRooms(long current, long size, String status, Long teacherId);

    LiveRoomVO detail(Long id);

    LiveRoom createRoom(Long teacherId, LiveRoomRequest request);

    LiveRoom updateRoom(Long teacherId, Long roomId, LiveRoomRequest request);

    LiveRoom startRoom(Long teacherId, Long roomId);

    LiveRoom endRoom(Long teacherId, Long roomId);

    void forceClose(Long roomId);

    LiveChat sendChat(Long userId, Long roomId, LiveMessageRequest request);

    LiveBarrage sendBarrage(Long userId, Long roomId, LiveMessageRequest request);

    LivePoll createPoll(Long teacherId, Long roomId, LivePollRequest request);

    void dismissPoll(Long teacherId, Long pollId);

    List<LivePollVO> listPolls(Long roomId, Long userId);

    LivePollVO pollDetail(Long pollId, Long userId);

    LivePollVO pollResults(Long pollId, Long userId);

    LivePollVote vote(Long userId, Long pollId, LiveVoteRequest request);

    LiveMicRequest requestMic(Long userId, Long roomId);

    List<LiveMicRequestVO> listMicRequests(Long teacherId, Long roomId);

    LiveMicRequest handleMic(Long teacherId, Long requestId, boolean approved);

    LiveMicRequest endMic(Long userId, Long requestId);

    void deleteRoom(Long teacherId, Long roomId);
}
