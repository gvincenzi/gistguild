package com.gist.guild.node.core.service.impl;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.node.core.document.Order;
import com.gist.guild.node.core.repository.OrderRepository;
import com.gist.guild.node.core.service.NodeBusinessService;
import com.gist.guild.node.core.service.NodeService;
import com.gist.guild.node.core.service.NodeUtils;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Data
@Service
public class OrderServiceImpl extends NodeService<com.gist.guild.commons.message.entity.Order, Order> {
    @Autowired
    OrderRepository repository;

    @Autowired
    NodeBusinessService nodeBusinessService;

    @Override
    public Order add(com.gist.guild.commons.message.entity.Order document) throws GistGuildGenericException {
        if (document == null) {
            throw new GistGuildGenericException(messageProperties.getError1());
        }
        if (repository.findByIsCorruptionDetectedTrue().size() > 0) {
            throw new GistGuildGenericException(messageProperties.getError2());
        }

        if(document.getId() != null && getRepository().findById(document.getId()).isPresent()){
            Order order = getRepository().findById(document.getId()).get();
            order.setAddress(document.getAddress());
            order.setDeleted(document.getDeleted());
            order.setDelivered(document.getDelivered());
            if(order.getDeleted()) nodeBusinessService.deleteOrder(document);
            return repository.save(order);
        } else {
            Order previous = repository.findTopByOrderByTimestampDesc();
            Order newItem = getNewItem(document, previous);

            //Validation order : check if Product has availability quantity
            nodeBusinessService.validateOrder(document);
            return repository.save(newItem);
        }
    }

    protected Order getNewItem(com.gist.guild.commons.message.entity.Order document, Order previous) throws GistGuildGenericException {
        if (document == null) {
            throw new GistGuildGenericException(messageProperties.getError1());
        }
        Order order = new Order();
        order.setPreviousId(previous != null ? previous.getId() : GENESIS);
        order.setNodeInstanceName(instanceName);
        order.setAddress(document.getAddress());
        order.setAmount(document.getAmount());
        order.setCustomerNickname(document.getCustomerNickname());
        order.setCustomerTelegramUserId(document.getCustomerTelegramUserId());
        order.setProductName(document.getProductName());
        order.setProductOwnerTelegramUserId(document.getProductOwnerTelegramUserId());
        order.setProductId(document.getProductId());
        order.setProductUrl(document.getProductUrl());
        order.setProductPassword(document.getProductPassword());
        order.setQuantity(document.getQuantity());
        order.setDeleted(document.getDeleted());
        order.setDelivered(document.getDelivered());

        Random random = new Random(order.getTimestamp().toEpochMilli());
        int nonce = random.nextInt();
        order.setNonce(nonce);
        order.setId(calculateHash(order));
        while (!NodeUtils.isHashResolved(order, difficultLevel)) {
            nonce = random.nextInt();
            order.setNonce(nonce);
            order.setId(calculateHash(order));
        }

        order.setExternalShortId(repository.count());
        return order;
    }

    @Override
    public Boolean updateLocal(com.gist.guild.commons.message.entity.Order document) throws GistGuildGenericException {
        if (repository.findByIsCorruptionDetectedTrue().size() == 0 && !repository.existsById(document.getId())) {
            Order order = new Order();
            order.setId(document.getId());
            order.setPreviousId(document.getPreviousId());
            order.setNodeInstanceName(document.getNodeInstanceName());
            order.setAddress(document.getAddress());
            order.setAmount(document.getAmount());
            order.setCustomerNickname(document.getCustomerNickname());
            order.setCustomerTelegramUserId(document.getCustomerTelegramUserId());
            order.setProductName(document.getProductName());
            order.setProductOwnerTelegramUserId(document.getProductOwnerTelegramUserId());
            order.setProductId(document.getProductId());
            order.setProductUrl(document.getProductUrl());
            order.setProductPassword(document.getProductPassword());
            order.setQuantity(document.getQuantity());
            order.setTimestamp(document.getTimestamp());
            order.setNonce(document.getNonce());
            order.setExternalShortId(document.getExternalShortId());
            order.setDeleted(document.getDeleted());
            order.setDelivered(document.getDelivered());
            repository.save(order);
        } else if(repository.findByIsCorruptionDetectedTrue().size() == 0 && repository.existsById(document.getId())){
            Order order = new Order();
            order.setAddress(document.getAddress());
            order.setDeleted(document.getDeleted());
            order.setDelivered(document.getDelivered());
            repository.save(order);
        }

        return validate(repository.findAllByOrderByTimestampAsc());
    }

    @Override
    public String calculateHash(Order document) throws GistGuildGenericException {
        return NodeUtils.applySha256(
                document.getPreviousId() +
                        document.getTimestamp().toEpochMilli() +
                        document.getNonce() +
                        document.getNodeInstanceName() +
                        document.getCustomerNickname() +
                        document.getCustomerTelegramUserId() +
                        document.getProductName() +
                        document.getQuantity() +
                        document.getAmount()
        );
    }
}
