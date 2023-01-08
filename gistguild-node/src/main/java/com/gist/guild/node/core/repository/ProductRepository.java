package com.gist.guild.node.core.repository;

import com.gist.guild.node.core.document.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends DocumentRepository<Product>, MongoRepository<Product, String> {
    @Query("{'active':true, 'deleted': false, $or: [ { 'availableQuantity': { $gt: :#{#quantity} } }, { 'availableQuantity': null } ]}")
    List<Product> findCatalog(@Param("quantity") Long quantity);
    List<Product> findByDeletedFalseAndOwnerTelegramUserIdOrderByTimestampAsc(Long ownerTelegramUserId);
}
