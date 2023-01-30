package com.gist.guild.node.core.repository;

import com.gist.guild.node.core.document.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends DocumentRepository<Product>, MongoRepository<Product, String> {
    @Query("{'active':true, 'deleted': false, $or: [ { 'availableQuantity': { $gt: :#{#quantity} } }, { 'availableQuantity': null } ]}")
    List<Product> findCatalog(@Param("quantity") Long quantity);
    @Query("{'active':true, 'deleted': false, $or: [ { 'availableQuantity': { $gt: :#{#quantity} } }, { 'availableQuantity': null } ], $or: [{'tags' : { $regex: :#{#tags}, $options: 'i' }},{'name' : { $regex: :#{#tags}, $options: 'i' }}]}")
    List<Product> findCatalogByTag(@Param("quantity") Long quantity, @Param("tags") String tags);
    List<Product> findByDeletedFalseAndOwnerTelegramUserIdOrderByTimestampAsc(Long ownerTelegramUserId);
}
