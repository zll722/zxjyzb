package com.edu.live.service;

import com.edu.live.dto.SiteBannerRequest;
import com.edu.live.entity.SiteBanner;
import com.edu.live.vo.SiteBannerVO;

import java.util.List;

public interface SiteBannerService {
    List<SiteBannerVO> listPublic();

    List<SiteBannerVO> listAdmin(String status);

    SiteBanner create(Long creatorId, SiteBannerRequest request);

    SiteBanner update(Long id, SiteBannerRequest request);

    void delete(Long id);
}
