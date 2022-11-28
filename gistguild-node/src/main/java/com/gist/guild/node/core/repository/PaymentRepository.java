package com.gist.guild.node.core.repository;

import com.gist.guild.node.core.document.Payment;
import com.gist.guild.node.core.document.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends DocumentRepository<Payment>, MongoRepository<Payment, String> {
    List<Payment> findByOrderId(String orderId);
}
