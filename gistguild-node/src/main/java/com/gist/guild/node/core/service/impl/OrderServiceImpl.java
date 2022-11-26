package com.gist.guild.node.core.service.impl;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.node.core.document.Order;
import com.gist.guild.node.core.document.Product;
import com.gist.guild.node.core.repository.OrderRepository;
import com.gist.guild.node.core.repository.ProductRepository;
import com.gist.guild.node.core.service.NodeService;
import com.gist.guild.node.core.service.NodeUtils;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Data
@Service
public class OrderServiceImpl extends NodeService<com.gist.guild.commons.message.entity.Order, Order> {
    @Autowired
    OrderRepository repository;

    @Override
    public Order add(com.gist.guild.commons.message.entity.Order document) throws GistGuildGenericException {
        if (document == null) {
            throw new GistGuildGenericException("Document is mandatory");
        }
        if (repository.findByIsCorruptionDetectedTrue().size() > 0) {
            throw new GistGuildGenericException("Gist Guild registry is corrupted");
        }

        if(document.getId() != null && getRepository().findById(document.getId()).isPresent()){
            Order order = getRepository().findById(document.getId()).get();
            order.setAddress(document.getAddress());
            return repository.save(order);
        } else {
            Order previous = repository.findTopByOrderByTimestampDesc();
            Order newItem = getNewItem(document, previous);
            return repository.save(newItem);
        }
    }

    protected Order getNewItem(com.gist.guild.commons.message.entity.Order document, Order previous) throws GistGuildGenericException {
        if (document == null) {
            throw new GistGuildGenericException("Document are mandatory");
        }
        Order order = new Order();
        order.setPreviousId(previous != null ? previous.getId() : GENESIS);
        order.setNodeInstanceName(instanceName);
        order.setAddress(document.getAddress());
        order.setAmount(document.getAmount());
        order.setCustomerMail(document.getCustomerMail());
        order.setCustomerTelegramUserId(document.getCustomerTelegramUserId());
        order.setProductName(document.getProductName());
        order.setQuantity(document.getQuantity());

        Random random = new Random(order.getTimestamp().toEpochMilli());
        int nonce = random.nextInt();
        order.setNonce(nonce);
        order.setId(calculateHash(order));
        while (!NodeUtils.isHashResolved(order, difficultLevel)) {
            nonce = random.nextInt();
            order.setNonce(nonce);
            order.setId(calculateHash(order));
        }

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
            order.setCustomerMail(document.getCustomerMail());
            order.setCustomerTelegramUserId(document.getCustomerTelegramUserId());
            order.setProductName(document.getProductName());
            order.setQuantity(document.getQuantity());
            order.setTimestamp(document.getTimestamp());
            order.setNonce(document.getNonce());
            repository.save(order);
            return validate(repository.findAllByOrderByTimestampAsc());
        } else if(repository.findByIsCorruptionDetectedTrue().size() == 0 && repository.existsById(document.getId())){
            Order order = new Order();
            order.setAddress(document.getAddress());
            // WE CANNOT MODIFY ONLY ADDRESS
            repository.save(order);
            return validate(repository.findAllByOrderByTimestampAsc());
        }

        return Boolean.TRUE;
    }

    @Override
    public String calculateHash(Order document) throws GistGuildGenericException {
        return NodeUtils.applySha256(
                document.getPreviousId() +
                        document.getTimestamp().toEpochMilli() +
                        document.getNonce() +
                        document.getNodeInstanceName() +
                        document.getCustomerMail() +
                        document.getCustomerTelegramUserId() +
                        document.getProductName() +
                        document.getQuantity() +
                        document.getAmount()
        );
    }
}
