package com.gist.guild.node.core.repository;

import com.gist.guild.node.core.document.Participant;

import java.util.List;

public interface DocumentRepository<T> {
    List<T> findByIsCorruptionDetectedTrue();
    List<T> findAllByOrderByTimestampAsc();
    T findTopByOrderByTimestampDesc();
}
