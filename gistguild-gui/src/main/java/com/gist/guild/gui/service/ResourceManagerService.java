package com.gist.guild.gui.service;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.message.entity.Order;
import com.gist.guild.commons.message.entity.Participant;
import com.gist.guild.commons.message.entity.Product;
import com.gist.guild.commons.message.entity.RechargeCredit;
import com.gist.guild.gui.bot.action.entity.Action;

import java.util.List;
import java.util.concurrent.Future;

public interface ResourceManagerService {
    Future<Participant> findParticipantByTelegramId(Long participant_id);
    Future<Participant> addOrUpdateParticipant(Participant participant);
    Action getActionInProgress(Long telegramUserId);
    void deleteAllActionInProgress(Long telegramUserId);
    void saveAction(Action action);
    void deleteActionInProgress(Action action);
    Future<List<Product>> getProducts();
    Future<List<Product>> getProductsByTags(String tags);
    Future<Product> getProduct(Long productExternalShortId);
    Future<Product> updateProduct(Product product);
    Future<List<Order>> getOrders(Long telegramUserId);
    Future<List<Order>> getPaidOrders(Long telegramUserId);
    Future<Order> getOrder(Long orderExternalId);
    Order addOrUpdateOrder(Order order) throws GistGuildGenericException;
    void payOrder(Long orderExternalId, String customerNickname, Long customerTelegramUserId) throws GistGuildGenericException;
    Future<RechargeCredit> getCredit(Long telegramUserId);
    Future<RechargeCredit> addCredit(RechargeCredit rechargeCredit);
}
