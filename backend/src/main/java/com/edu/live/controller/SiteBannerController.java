package com.edu.live.controller;

import com.edu.live.common.Result;
import com.edu.live.dto.SiteBannerRequest;
import com.edu.live.entity.SiteBanner;
import com.edu.live.entity.User;
import com.edu.live.service.SiteBannerService;
import com.edu.live.vo.SiteBannerVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/banners")
@RequiredArgsConstructor
public class SiteBannerController {
    private final SiteBannerService siteBannerService;

    @GetMapping
    public Result<List<SiteBannerVO>> listPublic() {
        return Result.success(siteBannerService.listPublic());
    }

    @GetMapping("/sample-image")
    public Result<String> sampleImage() {
        return Result.success("https://coreva-normal.trae.ai/api/ide/v1/text_to_image?prompt=modern%20online%20education%20platform%20hero%20banner%2C%20teacher%20livestream%20class%2C%20students%20learning%20dashboards%2C%20dark%20blue%20glassmorphism%2C%20cinematic%20lighting&image_size=landscape_16_9");
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<SiteBannerVO>> listAdmin(@RequestParam(required = false) String status) {
        return Result.success(siteBannerService.listAdmin(status));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SiteBanner> create(@AuthenticationPrincipal User user, @Valid @RequestBody SiteBannerRequest request) {
        return Result.success(siteBannerService.create(user.getId(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SiteBanner> update(@PathVariable Long id, @Valid @RequestBody SiteBannerRequest request) {
        return Result.success(siteBannerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        siteBannerService.delete(id);
        return Result.success();
    }
}
