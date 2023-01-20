package com.gist.guild.node.core.service.impl;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.node.core.document.Order;
import com.gist.guild.node.core.document.RechargeCredit;
import com.gist.guild.node.core.repository.RechargeCreditRepository;
import com.gist.guild.node.core.service.NodeService;
import com.gist.guild.node.core.service.NodeUtils;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Data
@Service
public class RechargeCreditServiceImpl extends NodeService<com.gist.guild.commons.message.entity.RechargeCredit, RechargeCredit> {
    @Autowired
    RechargeCreditRepository repository;

    @Override
    public RechargeCredit add(com.gist.guild.commons.message.entity.RechargeCredit document) throws GistGuildGenericException {
        if (document == null) {
            throw new GistGuildGenericException(messageProperties.getError1());
        }
        if (repository.findByIsCorruptionDetectedTrue().size() > 0) {
            throw new GistGuildGenericException(messageProperties.getError2());
        }

        if(document.getId() == null || !getRepository().findById(document.getId()).isPresent()){
            RechargeCredit previous = repository.findTopByOrderByTimestampDesc();
            RechargeCredit newItem = getNewItem(document, previous);
            return repository.save(newItem);
        } else {
            return repository.findById(document.getId()).get();
        }
    }

    protected RechargeCredit getNewItem(com.gist.guild.commons.message.entity.RechargeCredit document, RechargeCredit previous) throws GistGuildGenericException {
        if (document == null) {
            throw new GistGuildGenericException(messageProperties.getError1());
        }
        RechargeCredit rechargeCredit = new RechargeCredit();
        rechargeCredit.setPreviousId(previous != null ? previous.getId() : GENESIS);
        rechargeCredit.setNodeInstanceName(instanceName);
        rechargeCredit.setCustomerNickname(document.getCustomerNickname());
        rechargeCredit.setCustomerTelegramUserId(document.getCustomerTelegramUserId());
        rechargeCredit.setNewCredit(document.getNewCredit());
        rechargeCredit.setOldCredit(document.getOldCredit());
        rechargeCredit.setRechargeUserCreditType(document.getRechargeUserCreditType());

        Random random = new Random(rechargeCredit.getTimestamp().toEpochMilli());
        int nonce = random.nextInt();
        rechargeCredit.setNonce(nonce);
        rechargeCredit.setId(calculateHash(rechargeCredit));
        while (!NodeUtils.isHashResolved(rechargeCredit, difficultLevel)) {
            nonce = random.nextInt();
            rechargeCredit.setNonce(nonce);
            rechargeCredit.setId(calculateHash(rechargeCredit));
        }

        rechargeCredit.setExternalShortId(repository.count());
        return rechargeCredit;
    }

    @Override
    public Boolean updateLocal(com.gist.guild.commons.message.entity.RechargeCredit document) throws GistGuildGenericException {
        if (repository.findByIsCorruptionDetectedTrue().size() == 0 && !repository.existsById(document.getId())) {
            RechargeCredit rechargeCredit = new RechargeCredit();
            rechargeCredit.setId(document.getId());
            rechargeCredit.setTimestamp(document.getTimestamp());
            rechargeCredit.setPreviousId(document.getPreviousId());
            rechargeCredit.setNodeInstanceName(document.getNodeInstanceName());
            rechargeCredit.setCustomerNickname(document.getCustomerNickname());
            rechargeCredit.setCustomerTelegramUserId(document.getCustomerTelegramUserId());
            rechargeCredit.setNewCredit(document.getNewCredit());
            rechargeCredit.setOldCredit(document.getOldCredit());
            rechargeCredit.setRechargeUserCreditType(document.getRechargeUserCreditType());
            rechargeCredit.setNonce(document.getNonce());
            rechargeCredit.setExternalShortId(document.getExternalShortId());
            rechargeCredit.setLastUpdateTimestamp(document.getLastUpdateTimestamp());
            repository.save(rechargeCredit);
        }

        return validate(repository.findAllByOrderByTimestampAsc());
    }

    @Override
    public String calculateHash(RechargeCredit document) throws GistGuildGenericException {
        return NodeUtils.applySha256(
                document.getPreviousId() +
                        document.getTimestamp().toEpochMilli() +
                        document.getNonce() +
                        document.getNodeInstanceName() +
                        document.getCustomerNickname() +
                        document.getCustomerTelegramUserId() +
                        document.getOldCredit() +
                        document.getNewCredit()
        );
    }
}
