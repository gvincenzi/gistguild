package com.gist.guild.node.core.repository;

import com.gist.guild.node.core.document.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PaymentRepository extends DocumentRepository<Payment>, MongoRepository<Payment, String> {
    List<Payment> findTopByOrderIdAndCustomerTelegramUserIdOrderByTimestampDesc(String orderId, Long customerTelegramId);
}
