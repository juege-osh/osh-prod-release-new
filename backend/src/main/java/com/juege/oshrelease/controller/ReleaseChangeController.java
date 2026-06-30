package com.juege.oshrelease.controller;

import com.juege.oshrelease.common.ApiResponse;
import com.juege.oshrelease.dto.ReleaseChangeCreateRequest;
import com.juege.oshrelease.dto.ReleaseChangeDetailDTO;
import com.juege.oshrelease.dto.ReleaseChangeItemUpdateRequest;
import com.juege.oshrelease.dto.ReleaseChangeListItemDTO;
import com.juege.oshrelease.dto.ReleaseChangeOperationRequest;
import com.juege.oshrelease.dto.ReleaseChangeQueryRequest;
import com.juege.oshrelease.dto.ReviewerTestEvidenceRequest;
import com.juege.oshrelease.service.ReleaseChangeService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/changes")
public class ReleaseChangeController {

    private final ReleaseChangeService releaseChangeService;

    public ReleaseChangeController(ReleaseChangeService releaseChangeService) {
        this.releaseChangeService = releaseChangeService;
    }

    @GetMapping
    public ApiResponse<List<ReleaseChangeListItemDTO>> list(@RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) String status,
                                                            @RequestParam(required = false) String targetEnvCode) {
        ReleaseChangeQueryRequest request = new ReleaseChangeQueryRequest();
        request.keyword = keyword;
        request.status = status;
        request.targetEnvCode = targetEnvCode;
        return ApiResponse.ok(releaseChangeService.list(request));
    }

    @PostMapping
    public ApiResponse<ReleaseChangeDetailDTO> create(@RequestBody ReleaseChangeCreateRequest request) {
        return ApiResponse.ok(releaseChangeService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReleaseChangeDetailDTO> detail(@PathVariable Long id) {
        return ApiResponse.ok(releaseChangeService.detail(id));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<ReleaseChangeDetailDTO> submit(@PathVariable Long id) {
        return ApiResponse.ok(releaseChangeService.submit(id));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<ReleaseChangeDetailDTO> approve(@PathVariable Long id, @RequestBody ReleaseChangeOperationRequest request) {
        return ApiResponse.ok(releaseChangeService.approve(id, request));
    }

    @PostMapping("/{id}/demo")
    public ApiResponse<ReleaseChangeDetailDTO> demo(@PathVariable Long id, @RequestBody ReleaseChangeOperationRequest request) {
        return ApiResponse.ok(releaseChangeService.demo(id, request));
    }

    @PostMapping("/{id}/items/{itemId}")
    public ApiResponse<ReleaseChangeDetailDTO> updateItem(@PathVariable Long id,
                                                          @PathVariable Long itemId,
                                                          @RequestBody ReleaseChangeItemUpdateRequest request) {
        return ApiResponse.ok(releaseChangeService.updateItem(id, itemId, request));
    }

    @PostMapping("/{id}/reviewer-test")
    public ApiResponse<ReleaseChangeDetailDTO> reviewerTest(@PathVariable Long id, @RequestBody ReviewerTestEvidenceRequest request) {
        return ApiResponse.ok(releaseChangeService.reviewerTest(id, request));
    }

    @PostMapping("/{id}/validate-specs")
    public ApiResponse<ReleaseChangeDetailDTO> validateSpecs(@PathVariable Long id) {
        return ApiResponse.ok(releaseChangeService.validateSpecs(id));
    }

    @PostMapping("/{id}/test/function")
    public ApiResponse<ReleaseChangeDetailDTO> functionTest(@PathVariable Long id) {
        return ApiResponse.ok(releaseChangeService.functionTest(id));
    }

    @PostMapping("/{id}/test/data")
    public ApiResponse<ReleaseChangeDetailDTO> dataTest(@PathVariable Long id) {
        return ApiResponse.ok(releaseChangeService.dataTest(id));
    }

    @PostMapping("/{id}/test/env-diff")
    public ApiResponse<ReleaseChangeDetailDTO> envDiff(@PathVariable Long id) {
        return ApiResponse.ok(releaseChangeService.envDiff(id));
    }

    @PostMapping("/{id}/test/announce")
    public ApiResponse<ReleaseChangeDetailDTO> announceCheck(@PathVariable Long id) {
        return ApiResponse.ok(releaseChangeService.announceCheck(id));
    }

    @PostMapping("/{id}/switch/green")
    public ApiResponse<ReleaseChangeDetailDTO> switchGreen(@PathVariable Long id) {
        return ApiResponse.ok(releaseChangeService.switchGreen(id));
    }

    @PostMapping("/{id}/switch/blue")
    public ApiResponse<ReleaseChangeDetailDTO> switchBlue(@PathVariable Long id) {
        return ApiResponse.ok(releaseChangeService.switchBlue(id));
    }

    @PostMapping("/{id}/verify/manual")
    public ApiResponse<ReleaseChangeDetailDTO> manualVerify(@PathVariable Long id, @RequestBody ReleaseChangeOperationRequest request) {
        return ApiResponse.ok(releaseChangeService.manualVerify(id, request));
    }

    @PostMapping("/{id}/sync/blue")
    public ApiResponse<ReleaseChangeDetailDTO> syncGreenToBlue(@PathVariable Long id, @RequestBody ReleaseChangeOperationRequest request) {
        return ApiResponse.ok(releaseChangeService.syncGreenToBlue(id, request));
    }

    @PostMapping("/{id}/rollback")
    public ApiResponse<ReleaseChangeDetailDTO> rollback(@PathVariable Long id) {
        return ApiResponse.ok(releaseChangeService.rollback(id));
    }

    @GetMapping("/{id}/reports")
    public ApiResponse<Map<String, Object>> reports(@PathVariable Long id) {
        return ApiResponse.ok(releaseChangeService.reports(id));
    }
}
