package com.relax.content;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.relax.common.api.BusinessException;

@Service
public class BannerService {

    private final BannerMapper bannerMapper;

    BannerService(BannerMapper bannerMapper) {
        this.bannerMapper = bannerMapper;
    }

    public List<BannerMapper.BannerView> listActive() {
        return bannerMapper.findActive();
    }

    public List<BannerMapper.BannerView> listAll() {
        return bannerMapper.findAll();
    }

    @Transactional
    public BannerMapper.BannerView create(BannerRequest request) {
        long id = IdWorker.getId();
        bannerMapper.insert(id, request.title(), request.imageFileId(), request.linkType(), request.linkValue(), request.sort());
        return bannerMapper.findAll().stream().filter(b -> b.id() == id).findFirst().orElseThrow();
    }

    @Transactional
    public BannerMapper.BannerView update(long id, BannerRequest request) {
        if (bannerMapper.update(id, request.title(), request.imageFileId(), request.linkType(), request.linkValue(), request.sort()) == 0) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "BANNER_NOT_FOUND", "轮播图不存在");
        }
        return bannerMapper.findAll().stream().filter(b -> b.id() == id).findFirst().orElseThrow();
    }

    @Transactional
    public void updateStatus(long id, String status) {
        if (bannerMapper.updateStatus(id, status) == 0) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "BANNER_NOT_FOUND", "轮播图不存在");
        }
    }

    public record BannerRequest(String title, Long imageFileId, String linkType, String linkValue, int sort) {}
}
