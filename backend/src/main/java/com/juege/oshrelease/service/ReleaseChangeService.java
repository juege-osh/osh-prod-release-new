package com.juege.oshrelease.service;

import com.juege.oshrelease.dto.ReleaseChangeCreateRequest;
import com.juege.oshrelease.dto.ReleaseChangeDetailDTO;
import com.juege.oshrelease.dto.ReleaseChangeItemCreateRequest;
import com.juege.oshrelease.dto.ReleaseChangeItemDTO;
import com.juege.oshrelease.dto.ReleaseChangeItemUpdateRequest;
import com.juege.oshrelease.dto.ReleaseChangeListItemDTO;
import com.juege.oshrelease.dto.ReleaseChangeOperationRequest;
import com.juege.oshrelease.dto.ReleaseChangeQueryRequest;
import com.juege.oshrelease.dto.ReleaseNodeDTO;
import com.juege.oshrelease.dto.ReleaseOperationRecordDTO;
import com.juege.oshrelease.dto.ReviewerTestEvidenceDTO;
import com.juege.oshrelease.dto.ReviewerTestEvidenceRequest;
import com.juege.oshrelease.dto.ReviewRecordDTO;
import com.juege.oshrelease.dto.TestReportDTO;
import java.util.List;
import java.util.Map;

public interface ReleaseChangeService {
    List<ReleaseChangeListItemDTO> list(ReleaseChangeQueryRequest request);
    ReleaseChangeDetailDTO detail(Long id);
    ReleaseChangeDetailDTO create(ReleaseChangeCreateRequest request);
    ReleaseChangeDetailDTO submit(Long id);
    ReleaseChangeDetailDTO approve(Long id, ReleaseChangeOperationRequest request);
    ReleaseChangeDetailDTO demo(Long id, ReleaseChangeOperationRequest request);
    ReleaseChangeDetailDTO createItem(Long changeId, ReleaseChangeItemCreateRequest request);
    ReleaseChangeDetailDTO updateItem(Long changeId, Long itemId, ReleaseChangeItemUpdateRequest request);
    ReleaseChangeDetailDTO reviewerTest(Long id, ReviewerTestEvidenceRequest request);
    ReleaseChangeDetailDTO validateSpecs(Long id);
    ReleaseChangeDetailDTO envDiff(Long id);
    ReleaseChangeDetailDTO announceCheck(Long id);
    ReleaseChangeDetailDTO functionTest(Long id);
    ReleaseChangeDetailDTO dataTest(Long id);
    ReleaseChangeDetailDTO switchGreen(Long id);
    ReleaseChangeDetailDTO switchBlue(Long id);
    ReleaseChangeDetailDTO manualVerify(Long id, ReleaseChangeOperationRequest request);
    ReleaseChangeDetailDTO syncGreenToBlue(Long id, ReleaseChangeOperationRequest request);
    ReleaseChangeDetailDTO rollback(Long id);
    Map<String, Object> reports(Long id);
    List<ReleaseNodeDTO> nodes(Long id);
    List<ReleaseChangeItemDTO> items(Long id);
    List<ReviewRecordDTO> reviews(Long id);
    List<ReviewerTestEvidenceDTO> evidences(Long id);
    List<TestReportDTO> testReports(Long id);
    List<ReleaseOperationRecordDTO> operations(Long id);
}
