package com.gist.guild.node.core.service;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.message.entity.Payment;

public interface NodeBusinessService {
    void validatePayment(Payment payment) throws GistGuildGenericException;
}
