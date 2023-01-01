package com.gist.guild.node.core.service.impl;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.exception.GistGuildInsufficientCreditException;
import com.gist.guild.commons.exception.GistGuildInsufficientQuantityException;
import com.gist.guild.commons.message.entity.Order;
import com.gist.guild.commons.message.entity.Payment;
import com.gist.guild.commons.message.entity.RechargeCredit;
import com.gist.guild.commons.message.entity.RechargeCreditType;
import com.gist.guild.node.core.configuration.MessageProperties;
import com.gist.guild.node.core.document.Participant;
import com.gist.guild.node.core.document.Product;
import com.gist.guild.node.core.repository.ParticipantRepository;
import com.gist.guild.node.core.repository.ProductRepository;
import com.gist.guild.node.core.repository.RechargeCreditRepository;
import com.gist.guild.node.core.service.NodeBusinessService;
import com.gist.guild.node.core.service.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NodeBusinessServiceImpl implements NodeBusinessService {
    @Autowired
    RechargeCreditRepository rechargeCreditRepository;

    @Autowired
    ParticipantRepository participantRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    NodeService<RechargeCredit, com.gist.guild.node.core.document.RechargeCredit> rechargeCreditNodeService;

    @Autowired
    protected MessageProperties messageProperties;

    @Override
    public void validatePayment(Payment payment) throws GistGuildGenericException {
        List<Participant> byTelegramUserId = participantRepository.findByTelegramUserId(payment.getCustomerTelegramUserId());
        if(byTelegramUserId.isEmpty()) throw new GistGuildGenericException(messageProperties.getError3());
        Participant participant = byTelegramUserId.iterator().next();

        List<com.gist.guild.node.core.document.RechargeCredit> topByCustomerTelegramUserIdOrderByTimestampDesc = rechargeCreditRepository.findTopByCustomerTelegramUserIdOrderByTimestampDesc(payment.getCustomerTelegramUserId());

        if(topByCustomerTelegramUserIdOrderByTimestampDesc.isEmpty()) throw new GistGuildInsufficientCreditException(participant, messageProperties.getError4());

        Long actualCredit = topByCustomerTelegramUserIdOrderByTimestampDesc.iterator().next().getNewCredit();
        if(actualCredit<payment.getAmount()) throw new GistGuildInsufficientCreditException(participant, messageProperties.getError4());

        RechargeCredit rechargeCredit = new RechargeCredit();
        rechargeCredit.setCustomerMail(payment.getCustomerMail());
        rechargeCredit.setCustomerTelegramUserId(payment.getCustomerTelegramUserId());
        rechargeCredit.setNewCredit(actualCredit-payment.getAmount());
        rechargeCredit.setOldCredit(actualCredit);
        rechargeCredit.setRechargeUserCreditType(RechargeCreditType.PAYMENT);

        rechargeCreditNodeService.add(rechargeCredit);
    }

    @Override
    public void validateOrder(Order order) throws GistGuildGenericException {
        List<Participant> byTelegramUserId = participantRepository.findByTelegramUserId(order.getCustomerTelegramUserId());
        if(byTelegramUserId.isEmpty()) throw new GistGuildGenericException(messageProperties.getError3());
        Participant participant = byTelegramUserId.iterator().next();

        Optional<Product> productOptional = productRepository.findById(order.getProductId());
        if(productOptional.isEmpty()) throw new GistGuildGenericException(messageProperties.getError5());

        Product product = productOptional.get();
        if(product.getAvailableQuantity()!=null && product.getAvailableQuantity()<order.getQuantity()){
            throw new GistGuildInsufficientQuantityException(participant, messageProperties.getError6());
        } else if(product.getAvailableQuantity()!=null && product.getAvailableQuantity()>=order.getQuantity()){
            product.setAvailableQuantity(product.getAvailableQuantity()-order.getQuantity());
        }

        productRepository.save(product);
    }

    @Override
    public void deleteOrder(Order order) throws GistGuildGenericException {
        Optional<Product> productOptional = productRepository.findById(order.getProductId());
        if(productOptional.isEmpty()) throw new GistGuildGenericException(messageProperties.getError5());

        Product product = productOptional.get();
        if(order.getQuantity() != null && product.getAvailableQuantity() != null){
            product.setAvailableQuantity(product.getAvailableQuantity()+order.getQuantity());
        }
        productRepository.save(product);
    }
}
