package com.gist.guild.commons.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;

import java.util.UUID;

@Data
@JsonInclude(Include.NON_NULL)
public class DistributionMessage<T> {
    private UUID correlationID;
    private String instanceName;
    private DistributionEventType type;
    private Class documentClass;
    private T content;
    private Boolean valid;
}
