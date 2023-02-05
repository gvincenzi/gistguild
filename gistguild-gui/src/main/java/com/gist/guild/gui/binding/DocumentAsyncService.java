package com.gist.guild.gui.binding;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.message.entity.Document;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class DocumentAsyncService<T extends Document> {
    private Map<UUID, List<T>> cacheMap = new HashMap<>();
    private Map<UUID, List<GistGuildGenericException>> cacheExceptionMap = new HashMap<>();

    private ExecutorService executor
            = Executors.newSingleThreadExecutor();

    public Future<T> getUniqueResult(UUID correlationID) {
        return executor.submit(() -> {
            int timeout = 15000, waitingTime = 1000, waitingTimeTotal = 0;
            while(!cacheMap.containsKey(correlationID) && waitingTimeTotal<=timeout){
                Thread.sleep(waitingTime);
                waitingTimeTotal+=1000;
            }
            return waitingTimeTotal < timeout ? cacheMap.remove(correlationID).iterator().next() : null;
        });
    }

    public Future<List<T>> getResult(UUID correlationID) {
        return executor.submit(() -> {
            int timeout = 15000, waitingTime = 100, waitingTimeTotal = 0;
            while(!cacheMap.containsKey(correlationID) && waitingTimeTotal<=timeout){
                Thread.sleep(waitingTime);
                waitingTimeTotal+=1000;
            }
            return waitingTimeTotal < timeout ? cacheMap.remove(correlationID) : null;
        });
    }

    public void putInCache(UUID correlationID, T document, List<GistGuildGenericException> exceptions){
        if(!cacheMap.containsKey(correlationID)){
            cacheMap.put(correlationID, new ArrayList<>());
            cacheExceptionMap.put(correlationID, new ArrayList<>());
        }
        if(document != null){
            cacheMap.get(correlationID).add(document);
        }
        if(exceptions != null && !exceptions.isEmpty()){
            cacheExceptionMap.get(correlationID).addAll(exceptions);
        }
    }

    public List<GistGuildGenericException> getExceptionResult(UUID correlationID){
        return cacheExceptionMap.remove(correlationID);
    }
}
