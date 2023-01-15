package com.gist.guild.node.core.service;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.message.entity.Order;
import com.gist.guild.commons.message.entity.Payment;

public interface NodeBusinessService {
    void validatePayment(Payment payment) throws GistGuildGenericException;
    void validateOrder(Order order) throws GistGuildGenericException;
    void deleteOrder(Order order) throws GistGuildGenericException;
    void payOrder(com.gist.guild.node.core.document.Payment payment);
}
