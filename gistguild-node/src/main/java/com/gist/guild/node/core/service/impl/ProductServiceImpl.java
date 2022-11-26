package com.gist.guild.node.core.service.impl;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.node.core.document.Product;
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
public class ProductServiceImpl extends NodeService<com.gist.guild.commons.message.entity.Product, Product> {
    @Autowired
    ProductRepository repository;

    @Override
    public Product add(com.gist.guild.commons.message.entity.Product document) throws GistGuildGenericException {
        if (document == null) {
            throw new GistGuildGenericException("Document is mandatory");
        }
        if (repository.findByIsCorruptionDetectedTrue().size() > 0) {
            throw new GistGuildGenericException("Gist Guild registry is corrupted");
        }

        List<Product> products = getRepository().findByName(document.getName());
        if(products.size() > 0){
            Product product = products.iterator().next();
            product.setActive(document.getActive());
            product.setPrice(document.getPrice());
            product.setAvailableQuantity(document.getAvailableQuantity());
            product.setDelivery(document.getDelivery());
            product.setName(document.getName());
            product.setDescription(document.getDescription());
            product.setPassword(document.getPassword());
            product.setUrl(document.getUrl());
            return repository.save(product);
        } else {
            Product previous = repository.findTopByOrderByTimestampDesc();
            Product newItem = getNewItem(document, previous);
            return repository.save(newItem);
        }
    }

    protected Product getNewItem(com.gist.guild.commons.message.entity.Product document, Product previous) throws GistGuildGenericException {
        if (document == null) {
            throw new GistGuildGenericException("Document are mandatory");
        }
        Product product = new Product();
        product.setPreviousId(previous != null ? previous.getId() : GENESIS);
        product.setNodeInstanceName(instanceName);
        product.setActive(document.getActive());
        product.setPrice(document.getPrice());
        product.setAvailableQuantity(document.getAvailableQuantity());
        product.setDelivery(document.getDelivery());
        product.setDescription(document.getDescription());
        product.setName(document.getName());
        product.setPassword(document.getPassword());
        product.setUrl(document.getUrl());
        product.setOwnerMail(document.getOwnerMail());
        product.setOwnerTelegramUserId(document.getOwnerTelegramUserId());

        Random random = new Random(product.getTimestamp().toEpochMilli());
        int nonce = random.nextInt();
        product.setNonce(nonce);
        product.setId(calculateHash(product));
        while (!NodeUtils.isHashResolved(product, difficultLevel)) {
            nonce = random.nextInt();
            product.setNonce(nonce);
            product.setId(calculateHash(product));
        }

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
            product.setDelivery(document.getDelivery());
            product.setDescription(document.getDescription());
            product.setName(document.getName());
            product.setPassword(document.getPassword());
            product.setUrl(document.getUrl());
            product.setOwnerTelegramUserId(document.getOwnerTelegramUserId());
            product.setOwnerMail(document.getOwnerMail());
            product.setTimestamp(document.getTimestamp());
            product.setNonce(document.getNonce());
            repository.save(product);
            return validate(repository.findAllByOrderByTimestampAsc());
        } else if(repository.findByIsCorruptionDetectedTrue().size() == 0 && repository.existsById(document.getId())){
            Product product = repository.findById(document.getId()).get();
            product.setActive(document.getActive());
            product.setPrice(document.getPrice());
            product.setAvailableQuantity(document.getAvailableQuantity());
            product.setDelivery(document.getDelivery());
            product.setDescription(document.getDescription());
            product.setPassword(document.getPassword());
            product.setUrl(document.getUrl());
            // WE CANNOT MODIFY OWNER AND NAME
            repository.save(product);
            return validate(repository.findAllByOrderByTimestampAsc());
        }

        return Boolean.TRUE;
    }

    @Override
    public String calculateHash(Product document) throws GistGuildGenericException {
        return NodeUtils.applySha256(
                document.getPreviousId() +
                        document.getTimestamp().toEpochMilli() +
                        document.getNonce() +
                        document.getNodeInstanceName() +
                        document.getName() +
                        document.getOwnerMail()+
                        document.getOwnerTelegramUserId()
        );
    }
}
