package com.gist.guild.node.core.repository;

import com.gist.guild.node.core.document.RechargeCredit;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RechargeCreditRepository extends DocumentRepository<RechargeCredit>, MongoRepository<RechargeCredit, String> {
    List<RechargeCredit> findTopByCustomerTelegramUserIdOrderByTimestampDesc(Long telegramUserId);
}
