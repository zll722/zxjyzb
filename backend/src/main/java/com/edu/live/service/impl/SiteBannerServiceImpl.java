package com.edu.live.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.live.common.BusinessException;
import com.edu.live.dto.SiteBannerRequest;
import com.edu.live.entity.SiteBanner;
import com.edu.live.entity.User;
import com.edu.live.mapper.SiteBannerMapper;
import com.edu.live.mapper.UserMapper;
import com.edu.live.service.SiteBannerService;
import com.edu.live.vo.SiteBannerVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SiteBannerServiceImpl implements SiteBannerService {
    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";

    private final SiteBannerMapper siteBannerMapper;
    private final UserMapper userMapper;

    @Override
    public List<SiteBannerVO> listPublic() {
        try {
            return siteBannerMapper.selectList(new LambdaQueryWrapper<SiteBanner>()
                    .eq(SiteBanner::getStatus, STATUS_ENABLED)
                    .orderByAsc(SiteBanner::getSort)
                    .orderByDesc(SiteBanner::getCreatedAt))
                    .stream()
                    .map(this::toVO)
                    .toList();
        } catch (DataAccessException e) {
            return List.of();
        }
    }

    @Override
    public List<SiteBannerVO> listAdmin(String status) {
        try {
            return siteBannerMapper.selectList(new LambdaQueryWrapper<SiteBanner>()
                    .eq(StringUtils.hasText(status), SiteBanner::getStatus, status)
                    .orderByAsc(SiteBanner::getSort)
                    .orderByDesc(SiteBanner::getCreatedAt))
                    .stream()
                    .map(this::toVO)
                    .toList();
        } catch (DataAccessException e) {
            return List.of();
        }
    }

    @Override
    public SiteBanner create(Long creatorId, SiteBannerRequest request) {
        validateRequest(request);
        SiteBanner banner = new SiteBanner();
        fillBanner(banner, request);
        banner.setCreatorId(creatorId);
        siteBannerMapper.insert(banner);
        return banner;
    }

    @Override
    public SiteBanner update(Long id, SiteBannerRequest request) {
        validateRequest(request);
        SiteBanner banner = requireBanner(id);
        fillBanner(banner, request);
        siteBannerMapper.updateById(banner);
        return banner;
    }

    @Override
    public void delete(Long id) {
        requireBanner(id);
        siteBannerMapper.deleteById(id);
    }

    private void validateRequest(SiteBannerRequest request) {
        if (!Set.of(STATUS_ENABLED, STATUS_DISABLED).contains(request.getStatus())) {
            throw new BusinessException(400, "轮播状态不合法");
        }
        if (request.getSort() < 0) {
            throw new BusinessException(400, "排序不能小于0");
        }
    }

    private void fillBanner(SiteBanner banner, SiteBannerRequest request) {
        banner.setTitle(request.getTitle());
        banner.setSubtitle(request.getSubtitle());
        banner.setImageUrl(request.getImageUrl());
        banner.setLinkUrl(request.getLinkUrl());
        banner.setSort(request.getSort());
        banner.setStatus(request.getStatus());
    }

    private SiteBanner requireBanner(Long id) {
        SiteBanner banner = siteBannerMapper.selectById(id);
        if (banner == null) {
            throw new BusinessException(404, "轮播不存在");
        }
        return banner;
    }

    private SiteBannerVO toVO(SiteBanner banner) {
        User creator = banner.getCreatorId() == null ? null : userMapper.selectById(banner.getCreatorId());
        SiteBannerVO vo = new SiteBannerVO();
        vo.setId(banner.getId());
        vo.setTitle(banner.getTitle());
        vo.setSubtitle(banner.getSubtitle());
        vo.setImageUrl(banner.getImageUrl());
        vo.setLinkUrl(banner.getLinkUrl());
        vo.setSort(banner.getSort());
        vo.setStatus(banner.getStatus());
        vo.setCreatorId(banner.getCreatorId());
        vo.setCreatorName(creator == null ? null : creator.getUsername());
        vo.setCreatedAt(banner.getCreatedAt());
        return vo;
    }
}
