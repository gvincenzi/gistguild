package com.gist.guild.node.core.service.impl;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.node.core.document.Payment;
import com.gist.guild.node.core.repository.PaymentRepository;
import com.gist.guild.node.core.service.NodeBusinessService;
import com.gist.guild.node.core.service.NodeService;
import com.gist.guild.node.core.service.NodeUtils;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Data
@Service
public class PaymentServiceImpl extends NodeService<com.gist.guild.commons.message.entity.Payment, Payment> {
    @Autowired
    PaymentRepository repository;

    @Autowired
    NodeBusinessService nodeBusinessService;

    @Override
    public Payment add(com.gist.guild.commons.message.entity.Payment document) throws GistGuildGenericException {
        if (document == null) {
            throw new GistGuildGenericException(messageProperties.getError1());
        }
        if (repository.findByIsCorruptionDetectedTrue().size() > 0) {
            throw new GistGuildGenericException(messageProperties.getError2());
        }

        Payment previous = repository.findTopByOrderByTimestampDesc();
        Payment newItem = getNewItem(document, previous);

        //Validation payment : check if Participant has got credit enough to purchase
        nodeBusinessService.validatePayment(document);
        return repository.save(newItem);
    }

    protected Payment getNewItem(com.gist.guild.commons.message.entity.Payment document, Payment previous) throws GistGuildGenericException {
        if (document == null) {
            throw new GistGuildGenericException(messageProperties.getError1());
        }
        Payment payment = new Payment();
        payment.setPreviousId(previous != null ? previous.getId() : GENESIS);
        payment.setNodeInstanceName(instanceName);
        payment.setAmount(document.getAmount());
        payment.setCustomerMail(document.getCustomerMail());
        payment.setCustomerTelegramUserId(document.getCustomerTelegramUserId());
        payment.setOrderId(document.getOrderId());

        Random random = new Random(payment.getTimestamp().toEpochMilli());
        int nonce = random.nextInt();
        payment.setNonce(nonce);
        payment.setId(calculateHash(payment));
        while (!NodeUtils.isHashResolved(payment, difficultLevel)) {
            nonce = random.nextInt();
            payment.setNonce(nonce);
            payment.setId(calculateHash(payment));
        }

        payment.setExternalShortId(repository.count());
        return payment;
    }

    @Override
    public Boolean updateLocal(com.gist.guild.commons.message.entity.Payment document) throws GistGuildGenericException {
        if (repository.findByIsCorruptionDetectedTrue().size() == 0 && !repository.existsById(document.getId())) {
            Payment payment = new Payment();
            payment.setId(document.getId());
            payment.setPreviousId(document.getPreviousId());
            payment.setNodeInstanceName(document.getNodeInstanceName());
            payment.setAmount(document.getAmount());
            payment.setCustomerMail(document.getCustomerMail());
            payment.setCustomerTelegramUserId(document.getCustomerTelegramUserId());
            payment.setOrderId(document.getOrderId());
            payment.setTimestamp(document.getTimestamp());
            payment.setNonce(document.getNonce());
            payment.setExternalShortId(document.getExternalShortId());
            repository.save(payment);
        }

        return validate(repository.findAllByOrderByTimestampAsc());
    }

    @Override
    public String calculateHash(Payment document) throws GistGuildGenericException {
        return NodeUtils.applySha256(
                document.getPreviousId() +
                        document.getTimestamp().toEpochMilli() +
                        document.getNonce() +
                        document.getNodeInstanceName() +
                        document.getCustomerMail() +
                        document.getCustomerTelegramUserId() +
                        document.getOrderId() +
                        document.getAmount()
        );
    }
}
