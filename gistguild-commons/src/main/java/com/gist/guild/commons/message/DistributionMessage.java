package com.gist.guild.commons.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.gist.guild.commons.exception.GistGuildGenericException;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@JsonInclude(Include.NON_NULL)
public class DistributionMessage<T> {
    private UUID correlationID;
    private String instanceName;
    private DistributionEventType type;
    private String documentRepositoryMethod;
    private List<DocumentRepositoryMethodParameter> params;
    private Class documentClass;
    private T content;
    private Boolean valid;
    private List<GistGuildGenericException> exceptions;
}
