package com.edu.live.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.dto.LiveReplayRequest;
import com.edu.live.entity.LiveReplay;
import com.edu.live.vo.LiveReplayVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface LiveReplayService {
    Page<LiveReplayVO> pagePublished(long current, long size, Long teacherId);

    Page<LiveReplayVO> pageTeacher(Long teacherId, long current, long size);

    LiveReplayVO detail(Long id, Long userId);

    LiveReplay create(Long teacherId, LiveReplayRequest request);

    LiveReplay update(Long teacherId, Long id, LiveReplayRequest request);

    void delete(Long teacherId, Long id);

    LiveReplayVO uploadVideo(Long teacherId, Long id, MultipartFile file);

    void ensureReplayForEndedRoom(Long roomId);

    ResponseEntity<Resource> stream(Long id, Long userId, HttpServletRequest request);
}
