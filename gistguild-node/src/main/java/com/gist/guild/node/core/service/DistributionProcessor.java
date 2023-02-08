package com.gist.guild.node.core.service;

import com.gist.guild.commons.message.DistributionMessage;

import java.util.List;

public interface DistributionProcessor {
    void add(DistributionMessage<List<?>> msg);
    Boolean isEmpty();
    void process() throws InterruptedException;
}
