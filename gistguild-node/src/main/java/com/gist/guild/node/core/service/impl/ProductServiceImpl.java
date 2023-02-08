package com.gist.guild.node.core.service.impl;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.node.core.document.Product;
import com.gist.guild.node.core.repository.ProductRepository;
import com.gist.guild.node.core.service.NodeService;
import com.gist.guild.node.core.service.NodeUtils;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;

@Data
@Service
public class ProductServiceImpl extends NodeService<com.gist.guild.commons.message.entity.Product, Product> {
    @Autowired
    ProductRepository repository;

    @Override
    public Product add(com.gist.guild.commons.message.entity.Product document) throws GistGuildGenericException {
        if (document == null) {
            throw new GistGuildGenericException(messageProperties.getError1());
        }
        if (repository.findByIsCorruptionDetectedTrue().size() > 0) {
            throw new GistGuildGenericException(messageProperties.getError2());
        }

        if(document.getId() != null && getRepository().findById(document.getId()).isPresent()){
            Product product = getRepository().findById(document.getId()).get();
            product.setActive(document.getActive());
            product.setPrice(document.getPrice());
            product.setAvailableQuantity(document.getAvailableQuantity());
            product.setTags(document.getTags());
            product.setDelivery(document.getDelivery());
            product.setDeleted(document.getDeleted());
            product.setName(document.getName());
            product.setDescription(document.getDescription());
            product.setPassword(document.getPassword());
            product.setUrl(document.getUrl());
            product.setLastUpdateTimestamp(document.getLastUpdateTimestamp());
            return repository.save(product);
        } else {
            Product previous = repository.findTopByOrderByTimestampDesc();
            Product newItem = getNewItem(document, previous);
            return repository.save(newItem);
        }
    }

    protected Product getNewItem(com.gist.guild.commons.message.entity.Product document, Product previous) throws GistGuildGenericException {
        if (document == null) {
            throw new GistGuildGenericException(messageProperties.getError1());
        }
        Product product = new Product();
        product.setPreviousId(previous != null ? previous.getId() : GENESIS);
        product.setNodeInstanceName(instanceName);
        product.setActive(document.getActive());
        product.setPrice(document.getPrice());
        product.setAvailableQuantity(document.getAvailableQuantity());
        product.setTags(document.getTags());
        product.setDelivery(document.getDelivery());
        product.setDeleted(document.getDeleted());
        product.setDescription(document.getDescription());
        product.setName(document.getName());
        product.setPassword(document.getPassword());
        product.setUrl(document.getUrl());
        product.setOwnerTelegramUserId(document.getOwnerTelegramUserId());
        product.setLastUpdateTimestamp(document.getLastUpdateTimestamp());

        Random random = new Random(product.getTimestamp().toEpochMilli());
        int nonce = random.nextInt();
        product.setNonce(nonce);
        product.setId(calculateHash(product));
        while (!NodeUtils.isHashResolved(product, difficultLevel)) {
            nonce = random.nextInt();
            product.setNonce(nonce);
            product.setId(calculateHash(product));
        }

        product.setExternalShortId(repository.count());
        return product;
    }

    @Override
    public Boolean updateLocal(com.gist.guild.commons.message.entity.Product document) throws GistGuildGenericException {
        if (repository.findByIsCorruptionDetectedTrue().size() == 0 && !repository.existsById(document.getId())) {
            Product product = new Product();
            product.setId(document.getId());
            product.setPreviousId(document.getPreviousId());
            product.setNodeInstanceName(document.getNodeInstanceName());
            product.setActive(document.getActive());
            product.setPrice(document.getPrice());
            product.setAvailableQuantity(document.getAvailableQuantity());
            product.setTags(document.getTags());
            product.setDelivery(document.getDelivery());
            product.setDeleted(document.getDeleted());
            product.setDescription(document.getDescription());
            product.setName(document.getName());
            product.setPassword(document.getPassword());
            product.setUrl(document.getUrl());
            product.setOwnerTelegramUserId(document.getOwnerTelegramUserId());
            product.setTimestamp(document.getTimestamp());
            product.setNonce(document.getNonce());
            product.setExternalShortId(document.getExternalShortId());
            product.setLastUpdateTimestamp(document.getLastUpdateTimestamp());

            repository.save(product);
        } else if(repository.findByIsCorruptionDetectedTrue().size() == 0 && repository.existsById(document.getId())){
            Product product = repository.findById(document.getId()).get();
            product.setActive(document.getActive());
            product.setPrice(document.getPrice());
            product.setAvailableQuantity(document.getAvailableQuantity());
            product.setTags(document.getTags());
            product.setDelivery(document.getDelivery());
            product.setDeleted(document.getDeleted());
            product.setDescription(document.getDescription());
            product.setPassword(document.getPassword());
            product.setUrl(document.getUrl());
            product.setLastUpdateTimestamp(document.getLastUpdateTimestamp());
            // WE CANNOT MODIFY OWNER AND NAME
            repository.save(product);
        }

        return validate(repository.findAllByOrderByTimestampAsc());
    }

    @Override
    public String calculateHash(Product document) throws GistGuildGenericException {
        return NodeUtils.applySha256(
                document.getPreviousId() +
                        document.getTimestamp().toEpochMilli() +
                        document.getNonce() +
                        document.getNodeInstanceName() +
                        document.getName() +
                        document.getOwnerTelegramUserId()
        );
    }
}
