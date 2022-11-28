package com.gist.guild.node.core.repository;

import com.gist.guild.node.core.document.Order;
import com.gist.guild.node.core.document.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrderRepository extends DocumentRepository<Order>, MongoRepository<Order, String> {
    List<Order> findByCustomerTelegramUserId(Long telegramUserId);
}
