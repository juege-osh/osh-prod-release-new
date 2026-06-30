package com.juege.oshrelease.repo;

import com.juege.oshrelease.model.ReleaseChangeItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseChangeItemRepository extends JpaRepository<ReleaseChangeItem, Long> {
    List<ReleaseChangeItem> findByChangeIdOrderByItemOrderAsc(Long changeId);
}
