package com.gist.guild.distribution.domain.service.valence;

import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.DocumentRepositoryMethodParameter;
import com.gist.guild.commons.message.entity.DocumentProposition;
import com.gist.guild.distribution.exception.DistributionException;

import java.util.List;

public interface DeliveryValenceService {
    DistributionMessage<DocumentProposition> propose(DocumentProposition proposition) throws DistributionException, ClassNotFoundException;
    DistributionMessage<Void> sendIntegrityVerificationRequest() throws DistributionException;
    DistributionMessage<Void> sendDocumentClassRequest(String documentClass, String documentRepositoryMethod, List<DocumentRepositoryMethodParameter> params) throws DistributionException, ClassNotFoundException;
}
