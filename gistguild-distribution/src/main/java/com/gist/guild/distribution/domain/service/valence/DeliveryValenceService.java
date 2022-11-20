package com.gist.guild.distribution.domain.service.valence;

import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.entity.Document;
import com.gist.guild.distribution.exception.DistributionException;

public interface DeliveryValenceService {
    DistributionMessage<Document> propose(Document proposition) throws DistributionException;
    DistributionMessage<Void> sendIntegrityVerificationRequest() throws DistributionException;
}
