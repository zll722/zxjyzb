package com.edu.live.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.common.Result;
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
import com.edu.live.entity.User;
import com.edu.live.service.LiveService;
import com.edu.live.vo.LiveMicRequestVO;
import com.edu.live.vo.LivePollVO;
import com.edu.live.vo.LiveRoomVO;
import jakarta.validation.Valid;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/live")
@RequiredArgsConstructor
public class LiveController {
    private final LiveService liveService;

    @GetMapping("/rooms")
    public Result<Page<LiveRoomVO>> pageRooms(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) Long teacherId) {
        return Result.success(liveService.pageRooms(current, size, status, teacherId));
    }

    @GetMapping("/rooms/{id}")
    public Result<LiveRoomVO> detail(@PathVariable Long id) {
        return Result.success(liveService.detail(id));
    }

    @PostMapping("/rooms")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<LiveRoom> createRoom(@AuthenticationPrincipal User user, @Valid @RequestBody LiveRoomRequest request) {
        return Result.success(liveService.createRoom(user.getId(), request));
    }

    @PostMapping("/rooms/{id}/start")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<LiveRoom> startRoom(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return Result.success(liveService.startRoom(user.getId(), id));
    }

    @PostMapping("/rooms/{id}/end")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<LiveRoom> endRoom(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return Result.success(liveService.endRoom(user.getId(), id));
    }

    @DeleteMapping("/rooms/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Void> deleteRoom(@AuthenticationPrincipal User user, @PathVariable Long id) {
        liveService.deleteRoom(user.getId(), id);
        return Result.success();
    }

    @PostMapping("/rooms/{id}/force-close")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> forceClose(@PathVariable Long id) {
        liveService.forceClose(id);
        return Result.success();
    }

    @PostMapping("/rooms/{id}/chat")
    public Result<LiveChat> sendChat(@AuthenticationPrincipal User user, @PathVariable Long id, @Valid @RequestBody LiveMessageRequest request) {
        return Result.success(liveService.sendChat(user.getId(), id, request));
    }

    @PostMapping("/rooms/{id}/barrage")
    public Result<LiveBarrage> sendBarrage(@AuthenticationPrincipal User user, @PathVariable Long id, @Valid @RequestBody LiveMessageRequest request) {
        return Result.success(liveService.sendBarrage(user.getId(), id, request));
    }

    @PostMapping("/rooms/{id}/polls")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<LivePoll> createPoll(@AuthenticationPrincipal User user, @PathVariable Long id, @Valid @RequestBody LivePollRequest request) {
        return Result.success(liveService.createPoll(user.getId(), id, request));
    }

    @GetMapping("/rooms/{id}/polls")
    public Result<List<LivePollVO>> listPolls(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return Result.success(liveService.listPolls(id, user == null ? null : user.getId()));
    }

    @GetMapping("/polls/{id}")
    public Result<LivePollVO> pollDetail(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return Result.success(liveService.pollDetail(id, user == null ? null : user.getId()));
    }

    @GetMapping("/polls/{id}/results")
    public Result<LivePollVO> pollResults(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return Result.success(liveService.pollResults(id, user == null ? null : user.getId()));
    }

    @PostMapping("/polls/{id}/dismiss")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<Void> dismissPoll(@AuthenticationPrincipal User user, @PathVariable Long id) {
        liveService.dismissPoll(user.getId(), id);
        return Result.success();
    }

    @PostMapping("/polls/{id}/vote")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<LivePollVote> vote(@AuthenticationPrincipal User user, @PathVariable Long id, @Valid @RequestBody LiveVoteRequest request) {
        return Result.success(liveService.vote(user.getId(), id, request));
    }

    @PostMapping("/rooms/{id}/mic-requests")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<LiveMicRequest> requestMic(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return Result.success(liveService.requestMic(user.getId(), id));
    }

    @GetMapping("/rooms/{id}/mic-requests")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<List<LiveMicRequestVO>> listMicRequests(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return Result.success(liveService.listMicRequests(user.getId(), id));
    }

    @PostMapping("/mic-requests/{id}/handle")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<LiveMicRequest> handleMic(@AuthenticationPrincipal User user, @PathVariable Long id, @RequestParam boolean approved) {
        return Result.success(liveService.handleMic(user.getId(), id, approved));
    }

    @PostMapping("/mic-requests/{id}/end")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    public Result<LiveMicRequest> endMic(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return Result.success(liveService.endMic(user.getId(), id));
    }
}
