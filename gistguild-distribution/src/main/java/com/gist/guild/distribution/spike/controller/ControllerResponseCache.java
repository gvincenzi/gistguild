package com.gist.guild.distribution.spike.controller;

import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.distribution.exception.DistributionException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ControllerResponseCache {
    private static int limitCache = 100;
    private static volatile Map<UUID, DistributionMessage> cache = new HashMap<>(limitCache);

    public static void putInCache(DistributionMessage distributionMessage) throws DistributionException {
        if(cache.size()<limitCache){
            cache.put(distributionMessage.getCorrelationID(),distributionMessage);
        } else {
            throw new DistributionException(String.format("Max cache limit [%d] exceeded",limitCache));
        }
    }

    public static DistributionMessage getFromCache(UUID correlationId) {
        return cache.get(correlationId);
    }

    public static DistributionMessage removeFromCache(UUID correlationId) {
        return cache.remove(correlationId);
    }
}
