package com.gist.guild.commons.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gist.guild.commons.exception.GistGuildGenericException;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Data
@JsonInclude(Include.NON_NULL)
public class DistributionMessage<T> implements Delayed {
    private UUID correlationID;
    private String instanceName;
    private DistributionEventType type;
    private String documentRepositoryMethod;
    private List<DocumentRepositoryMethodParameter> params;
    private Class documentClass;
    private T content;
    private Boolean valid;
    private List<GistGuildGenericException> exceptions;
    private Instant timestamp = Instant.now();
    private Instant startBlockingProcessingTime = timestamp.plusMillis(500);

    @Override
    public long getDelay(TimeUnit timeUnit) {
        Duration res = Duration.between(startBlockingProcessingTime, timestamp);
        return res.toMillis();
    }

    @Override
    public int compareTo(Delayed that)
    {
        long result = this.getDelay(TimeUnit.NANOSECONDS)
                - that.getDelay(TimeUnit.NANOSECONDS);
        if (result < 0) {
            return -1;
        } else if (result > 0) {
            return 1;
        }
        return 0;
    }
}
