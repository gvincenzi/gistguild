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
    Future<Participant> findParticipantByMail(String participant_mail);
    Future<Participant> addOrUpdateParticipant(Participant participant);
    Action getActionInProgress(Long telegramUserId);
    void saveAction(Action action);
    void deleteActionInProgress(Action action);
    Future<List<Product>> getProducts(Boolean all);
    Future<Product> getProduct(Long productExternalShortId);
    Future<Product> updateProduct(Product product);
    Future<List<Order>> getOrders(Long telegramUserId);
    Order addOrUpdateOrder(Order order) throws GistGuildGenericException;
    Order getOrderProcessed(Long orderExternalId);
    void payOrder(Long orderExternalId, String customerMail, Long customerTelegramUserId) throws GistGuildGenericException;
    Future<RechargeCredit> getCredit(Long telegramUserId);
    Future<RechargeCredit> addCredit(RechargeCredit rechargeCredit);
}
