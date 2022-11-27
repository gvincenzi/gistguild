package com.gist.guild.node.core.repository;

import java.util.List;

public interface DocumentRepository<T> {
    List<T> findByIsCorruptionDetectedTrue();
    List<T> findAllByOrderByTimestampAsc();
    T findTopByOrderByTimestampDesc();
    List<T> findByExternalShortId(Long externalShortId);
}
