package com.edu.live.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.common.BusinessException;
import com.edu.live.dto.LiveReplayRequest;
import com.edu.live.entity.LiveReplay;
import com.edu.live.entity.LiveRoom;
import com.edu.live.entity.User;
import com.edu.live.enums.LiveRoomStatus;
import com.edu.live.mapper.LiveReplayMapper;
import com.edu.live.mapper.LiveRoomMapper;
import com.edu.live.mapper.UserMapper;
import com.edu.live.service.LiveReplayService;
import com.edu.live.vo.LiveReplayVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LiveReplayServiceImpl implements LiveReplayService {
    private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/webm", "video/ogg", "video/quicktime", "video/x-msvideo");

    private final LiveReplayMapper liveReplayMapper;
    private final LiveRoomMapper liveRoomMapper;
    private final UserMapper userMapper;

    @Value("${file.replay-path}")
    private String replayPath;

    @Override
    public Page<LiveReplayVO> pagePublished(long current, long size, Long teacherId) {
        Page<LiveReplay> page = liveReplayMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<LiveReplay>()
                .eq(LiveReplay::getStatus, "PUBLISHED")
                .eq(teacherId != null, LiveReplay::getTeacherId, teacherId)
                .orderByDesc(LiveReplay::getCreatedAt));
        return toPage(page);
    }

    @Override
    public Page<LiveReplayVO> pageTeacher(Long teacherId, long current, long size) {
        Page<LiveReplay> page = liveReplayMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<LiveReplay>()
                .eq(LiveReplay::getTeacherId, teacherId)
                .orderByDesc(LiveReplay::getCreatedAt));
        return toPage(page);
    }

    @Override
    public LiveReplayVO detail(Long id, Long userId) {
        LiveReplay replay = requireReplay(id);
        if (!"PUBLISHED".equals(replay.getStatus()) && !Objects.equals(replay.getTeacherId(), userId)) {
            throw new BusinessException(403, "无权访问该回放");
        }
        return toVO(replay);
    }

    @Override
    public LiveReplay create(Long teacherId, LiveReplayRequest request) {
        LiveReplay replay = new LiveReplay();
        replay.setRoomId(request.getRoomId());
        replay.setTeacherId(teacherId);
        replay.setTitle(request.getTitle());
        replay.setIntro(request.getIntro());
        replay.setDuration(0);
        replay.setFileSize(0L);
        replay.setStatus(normalizeStatus(request.getStatus()));
        if (request.getRoomId() != null) {
            requireTeacherRoom(teacherId, request.getRoomId());
        }
        liveReplayMapper.insert(replay);
        return replay;
    }

    @Override
    public LiveReplay update(Long teacherId, Long id, LiveReplayRequest request) {
        LiveReplay replay = requireTeacherReplay(teacherId, id);
        if (request.getRoomId() != null && !Objects.equals(request.getRoomId(), replay.getRoomId())) {
            requireTeacherRoom(teacherId, request.getRoomId());
            replay.setRoomId(request.getRoomId());
        }
        replay.setTitle(request.getTitle());
        replay.setIntro(request.getIntro());
        replay.setStatus(normalizeStatus(request.getStatus()));
        liveReplayMapper.updateById(replay);
        return replay;
    }

    @Override
    public void delete(Long teacherId, Long id) {
        LiveReplay replay = requireTeacherReplay(teacherId, id);
        liveReplayMapper.deleteById(replay.getId());
    }

    @Override
    public LiveReplayVO uploadVideo(Long teacherId, Long id, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "回放视频不能为空");
        }
        if (!VIDEO_TYPES.contains(file.getContentType())) {
            throw new BusinessException(400, "视频仅支持 MP4、WEBM、OGG、MOV、AVI 格式");
        }
        LiveReplay replay = requireTeacherReplay(teacherId, id);
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : ".mp4";
        String datePath = LocalDate.now().toString().replace("-", "/");
        String fileName = UUID.randomUUID() + ext;
        Path dir = Path.of(replayPath, datePath);
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(fileName));
        } catch (IOException e) {
            throw new BusinessException("回放视频上传失败");
        }
        replay.setVideoPath(datePath + "/" + fileName);
        replay.setFileSize(file.getSize());
        liveReplayMapper.updateById(replay);
        return toVO(replay);
    }

    @Override
    public void ensureReplayForEndedRoom(Long roomId) {
        LiveRoom room = liveRoomMapper.selectById(roomId);
        if (room == null || !LiveRoomStatus.ENDED.name().equals(room.getStatus())) {
            return;
        }
        boolean exists = liveReplayMapper.exists(new LambdaQueryWrapper<LiveReplay>().eq(LiveReplay::getRoomId, roomId));
        if (exists) {
            return;
        }
        LiveReplay replay = new LiveReplay();
        replay.setRoomId(room.getId());
        replay.setTeacherId(room.getTeacherId());
        replay.setTitle(room.getTitle() + " 回放");
        replay.setIntro(room.getIntro());
        replay.setFileSize(0L);
        replay.setDuration(0);
        replay.setStatus("DRAFT");
        liveReplayMapper.insert(replay);
    }

    @Override
    public ResponseEntity<Resource> stream(Long id, Long userId, HttpServletRequest request) {
        LiveReplay replay = requireReplay(id);
        boolean isOwner = Objects.equals(replay.getTeacherId(), userId);
        if (!"PUBLISHED".equals(replay.getStatus()) && !isOwner) {
            throw new BusinessException(403, "回放未发布");
        }
        if (!StringUtils.hasText(replay.getVideoPath())) {
            throw new BusinessException(404, "回放视频不存在");
        }
        Path file = Path.of(replayPath, replay.getVideoPath());
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new BusinessException(404, "回放视频不存在");
        }
        try {
            long fileSize = Files.size(file);
            String range = request.getHeader(HttpHeaders.RANGE);
            long start = 0;
            long end = fileSize - 1;
            HttpStatus status = HttpStatus.OK;
            if (StringUtils.hasText(range) && range.startsWith("bytes=")) {
                String[] ranges = range.substring(6).split("-", 2);
                start = Long.parseLong(ranges[0]);
                if (ranges.length > 1 && StringUtils.hasText(ranges[1])) {
                    end = Long.parseLong(ranges[1]);
                }
                end = Math.min(end, fileSize - 1);
                status = HttpStatus.PARTIAL_CONTENT;
            }
            if (start > end || start < 0) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                        .build();
            }
            Resource resource = status == HttpStatus.PARTIAL_CONTENT
                    ? new InputStreamResource(openRangeStream(file, start))
                    : new FileSystemResource(file);
            String fileName = URLEncoder.encode(file.getFileName().toString(), StandardCharsets.UTF_8);
            ResponseEntity.BodyBuilder builder = ResponseEntity.status(status)
                    .contentType(MediaType.parseMediaType(resolveContentType(file)))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + fileName)
                    .contentLength(end - start + 1);
            if (status == HttpStatus.PARTIAL_CONTENT) {
                builder.header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize);
            }
            return builder.body(resource);
        } catch (IOException | NumberFormatException e) {
            throw new BusinessException("回放视频读取失败");
        }
    }

    private LiveReplay requireReplay(Long id) {
        LiveReplay replay = liveReplayMapper.selectById(id);
        if (replay == null) {
            throw new BusinessException(404, "回放不存在");
        }
        return replay;
    }

    private LiveReplay requireTeacherReplay(Long teacherId, Long id) {
        LiveReplay replay = requireReplay(id);
        if (!Objects.equals(replay.getTeacherId(), teacherId)) {
            throw new BusinessException(403, "只能操作自己的回放");
        }
        return replay;
    }

    private LiveRoom requireTeacherRoom(Long teacherId, Long roomId) {
        LiveRoom room = liveRoomMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException(404, "直播间不存在");
        }
        if (!Objects.equals(room.getTeacherId(), teacherId)) {
            throw new BusinessException(403, "只能关联自己的直播间");
        }
        return room;
    }

    private String normalizeStatus(String status) {
        return "PUBLISHED".equals(status) ? "PUBLISHED" : "DRAFT";
    }

    private Page<LiveReplayVO> toPage(Page<LiveReplay> page) {
        Page<LiveReplayVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    private LiveReplayVO toVO(LiveReplay replay) {
        LiveRoom room = replay.getRoomId() == null ? null : liveRoomMapper.selectById(replay.getRoomId());
        User teacher = userMapper.selectById(replay.getTeacherId());
        LiveReplayVO vo = new LiveReplayVO();
        vo.setId(replay.getId());
        vo.setRoomId(replay.getRoomId());
        vo.setRoomTitle(room == null ? null : room.getTitle());
        vo.setTeacherId(replay.getTeacherId());
        vo.setTeacherName(teacher == null ? null : teacher.getUsername());
        vo.setTitle(replay.getTitle());
        vo.setIntro(replay.getIntro());
        vo.setVideoPath(replay.getVideoPath());
        vo.setPlayUrl(StringUtils.hasText(replay.getVideoPath()) ? "/api/replays/" + replay.getId() + "/stream" : null);
        vo.setFileSize(replay.getFileSize());
        vo.setDuration(replay.getDuration());
        vo.setStatus(replay.getStatus());
        vo.setCreatedAt(replay.getCreatedAt());
        vo.setUpdatedAt(replay.getUpdatedAt());
        return vo;
    }

    private InputStream openRangeStream(Path file, long start) throws IOException {
        InputStream inputStream = Files.newInputStream(file);
        long skipped = inputStream.skip(start);
        if (skipped != start) {
            inputStream.close();
            throw new IOException();
        }
        return inputStream;
    }

    private String resolveContentType(Path file) throws IOException {
        String contentType = Files.probeContentType(file);
        return contentType == null ? "video/mp4" : contentType;
    }
}
