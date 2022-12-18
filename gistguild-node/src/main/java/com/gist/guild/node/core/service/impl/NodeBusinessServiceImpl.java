package com.gist.guild.node.core.service.impl;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.exception.GistGuildInsufficientCreditException;
import com.gist.guild.commons.message.entity.Payment;
import com.gist.guild.commons.message.entity.RechargeCredit;
import com.gist.guild.commons.message.entity.RechargeCreditType;
import com.gist.guild.node.core.document.Participant;
import com.gist.guild.node.core.repository.ParticipantRepository;
import com.gist.guild.node.core.repository.RechargeCreditRepository;
import com.gist.guild.node.core.service.NodeBusinessService;
import com.gist.guild.node.core.service.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NodeBusinessServiceImpl implements NodeBusinessService {
    @Autowired
    RechargeCreditRepository rechargeCreditRepository;

    @Autowired
    ParticipantRepository participantRepository;

    @Autowired
    NodeService<RechargeCredit, com.gist.guild.node.core.document.RechargeCredit> rechargeCreditNodeService;

    @Override
    public void validatePayment(Payment payment) throws GistGuildGenericException {
        List<Participant> byTelegramUserId = participantRepository.findByTelegramUserId(payment.getCustomerTelegramUserId());
        if(byTelegramUserId.isEmpty()) throw new GistGuildGenericException("Participant does not exist");
        Participant participant = byTelegramUserId.iterator().next();

        List<com.gist.guild.node.core.document.RechargeCredit> topByCustomerTelegramUserIdOrderByTimestampDesc = rechargeCreditRepository.findTopByCustomerTelegramUserIdOrderByTimestampDesc(payment.getCustomerTelegramUserId());

        if(topByCustomerTelegramUserIdOrderByTimestampDesc.isEmpty()) throw new GistGuildInsufficientCreditException(participant, "Participant has insufficient credit to finalize this payment");

        Long actualCredit = topByCustomerTelegramUserIdOrderByTimestampDesc.iterator().next().getNewCredit();
        if(actualCredit<payment.getAmount()) throw new GistGuildInsufficientCreditException(participant, "Participant has insufficient credit to finalize this payment");

        RechargeCredit rechargeCredit = new RechargeCredit();
        rechargeCredit.setCustomerMail(payment.getCustomerMail());
        rechargeCredit.setCustomerTelegramUserId(payment.getCustomerTelegramUserId());
        rechargeCredit.setNewCredit(actualCredit-payment.getAmount());
        rechargeCredit.setOldCredit(actualCredit);
        rechargeCredit.setRechargeUserCreditType(RechargeCreditType.PAYMENT);

        rechargeCreditNodeService.add(rechargeCredit);
    }
}
