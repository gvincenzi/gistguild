package com.gist.guild.node.binding;

import com.gist.guild.commons.exception.GistGuildGenericException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class CorrelationIdCache {
    private Set<UUID> cache = new HashSet<>();

    private ExecutorService executor
            = Executors.newSingleThreadExecutor();

    public Future<Boolean> getResult(UUID correlationID) {
        return executor.submit(() -> {
            int timeout = 10000, waitingTime = 100, waitingTimeTotal = 0;
            while(!cache.contains(correlationID) && waitingTimeTotal<=timeout){
                Thread.sleep(waitingTime);
                waitingTimeTotal+=1000;
            }
            return waitingTimeTotal < timeout ? cache.remove(correlationID) : null;
        });
    }

    public void putInCache(UUID correlationID, List<GistGuildGenericException> exceptions){
        if(!cache.contains(correlationID)){
            cache.add(correlationID);
        }

        //TODO Process exceptions
    }
}
