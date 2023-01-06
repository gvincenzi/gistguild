package com.gist.guild.node.core.repository;

import com.gist.guild.node.core.document.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductRepository extends DocumentRepository<Product>, MongoRepository<Product, String> {
    List<Product> findByActiveTrueAndDeletedFalseAndAvailableQuantityIsNullOrAvailableQuantityGreaterThan(Long quantity);
    List<Product> findByDeletedFalseAndOwnerTelegramUserIdOrderByTimestampAsc(Long ownerTelegramUserId);
}
