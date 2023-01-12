package com.gist.guild.node.core.service;

import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.entity.DocumentProposition;

public interface EntryPropositionProcessor {
    void add(DistributionMessage<DocumentProposition<?>> msg);
    Boolean isEmpty();
    void process() throws InterruptedException;
}
