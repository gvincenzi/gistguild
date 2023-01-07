package com.gist.guild.gui.service.impl;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.DocumentPropositionType;
import com.gist.guild.commons.message.DocumentRepositoryMethodParameter;
import com.gist.guild.commons.message.entity.*;
import com.gist.guild.gui.binding.DocumentAsyncService;
import com.gist.guild.gui.bot.action.entity.Action;
import com.gist.guild.gui.bot.action.repository.ActionRepository;
import com.gist.guild.gui.client.DocumentClient;
import com.gist.guild.gui.service.GuiConcurrenceService;
import com.gist.guild.gui.service.ResourceManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Slf4j
@Service
public class ResourceManagerServiceImpl implements ResourceManagerService {
    @Autowired
    DocumentClient documentClient;

    @Autowired
    DocumentAsyncService documentAsyncService;

    @Autowired
    private ActionRepository actionRepository;

    @Override
    public Future<Participant> findParticipantByTelegramId(Long participant_id) {
        List<DocumentRepositoryMethodParameter<?>> params = new ArrayList<>(1);
        params.add(new DocumentRepositoryMethodParameter<Long>(Long.class, participant_id));
        ResponseEntity<DistributionMessage<Void>> distributionMessageResponseEntity = documentClient.documentByClass(Participant.class.getSimpleName(), "findByTelegramUserIdAndActiveTrue", params);
        return documentAsyncService.getUniqueResult(distributionMessageResponseEntity.getBody().getCorrelationID());
    }

    @Override
    public Future<Participant> addOrUpdateParticipant(Participant participant) {
        DocumentProposition documentProposition = new DocumentProposition();
        documentProposition.setDocumentPropositionType(DocumentPropositionType.USER_REGISTRATION);
        documentProposition.setDocumentClass(Participant.class.getSimpleName());
        documentProposition.setDocument(participant);
        ResponseEntity<DistributionMessage<DocumentProposition>> distributionMessageResponseEntity = documentClient.itemProposition(documentProposition);
        GuiConcurrenceService.getCorrelationIDs().add(distributionMessageResponseEntity.getBody().getCorrelationID());
        return documentAsyncService.getUniqueResult(distributionMessageResponseEntity.getBody().getCorrelationID());
    }

    @Override
    public Order addOrUpdateOrder(Order order) throws GistGuildGenericException {
        DocumentProposition documentProposition = new DocumentProposition();
        documentProposition.setDocumentPropositionType(DocumentPropositionType.ORDER_REGISTRATION);
        documentProposition.setDocumentClass(Order.class.getSimpleName());
        documentProposition.setDocument(order);
        ResponseEntity<DistributionMessage<DocumentProposition>> distributionMessageResponseEntity = documentClient.itemProposition(documentProposition);
        GuiConcurrenceService.getCorrelationIDs().add(distributionMessageResponseEntity.getBody().getCorrelationID());
        UUID correlationID = distributionMessageResponseEntity.getBody().getCorrelationID();
        try {
            order = (Order) documentAsyncService.getUniqueResult(correlationID).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage());
            if (correlationID != null) {
                List<? extends GistGuildGenericException> exceptionResult = documentAsyncService.getExceptionResult(correlationID);
                if (exceptionResult != null && !exceptionResult.isEmpty()) {
                    throw exceptionResult.iterator().next();
                }
            }
        }

        return order;
    }

    @Override
    public Action getActionInProgress(Long telegramUserId) {
        Optional<Action> actionOptional = actionRepository.findByTelegramUserIdAndInProgressTrue(telegramUserId);
        if (actionOptional.isPresent()) {
            return actionOptional.get();
        } else {
            return null;
        }
    }

    @Override
    public Future<List<Product>> getProducts(Boolean all, Long ownerTelegramUserId) {
        List<DocumentRepositoryMethodParameter<?>> params = new ArrayList<>(1);
        params.add(new DocumentRepositoryMethodParameter<Long>(Long.class, 0L));
        List<DocumentRepositoryMethodParameter<?>> paramsAll = new ArrayList<>(1);
        paramsAll.add(new DocumentRepositoryMethodParameter<Long>(Long.class, ownerTelegramUserId));
        ResponseEntity<DistributionMessage<Void>> distributionMessageResponseEntity = documentClient.documentByClass(Product.class.getSimpleName(), all ? "findByDeletedFalseAndOwnerTelegramUserIdOrderByTimestampAsc" : "findByActiveTrueAndDeletedFalseAndAvailableQuantityIsNullOrAvailableQuantityGreaterThan", all ? paramsAll : params);
        return documentAsyncService.getResult(distributionMessageResponseEntity.getBody().getCorrelationID());
    }

    @Override
    public Future<Product> getProduct(Long productExternalShortId) {
        List<DocumentRepositoryMethodParameter<?>> params = new ArrayList<>(1);
        params.add(new DocumentRepositoryMethodParameter<Long>(Long.class, productExternalShortId));
        ResponseEntity<DistributionMessage<Void>> distributionMessageResponseEntity = documentClient.documentByClass(Product.class.getSimpleName(), "findByExternalShortId", params);
        return documentAsyncService.getUniqueResult(distributionMessageResponseEntity.getBody().getCorrelationID());
    }

    @Override
    public Future<Product> updateProduct(Product product) {
        DocumentProposition documentProposition = new DocumentProposition();
        documentProposition.setDocumentPropositionType(DocumentPropositionType.PRODUCT_REGISTRATION);
        documentProposition.setDocumentClass(Product.class.getSimpleName());
        documentProposition.setDocument(product);
        ResponseEntity<DistributionMessage<DocumentProposition>> distributionMessageResponseEntity = documentClient.itemProposition(documentProposition);
        GuiConcurrenceService.getCorrelationIDs().add(distributionMessageResponseEntity.getBody().getCorrelationID());
        return documentAsyncService.getUniqueResult(distributionMessageResponseEntity.getBody().getCorrelationID());
    }

    @Override
    public Future<List<Order>> getOrders(Long telegramUserId) {
        List<DocumentRepositoryMethodParameter<?>> params = new ArrayList<>(1);
        params.add(new DocumentRepositoryMethodParameter<Long>(Long.class, telegramUserId));
        ResponseEntity<DistributionMessage<Void>> distributionMessageResponseEntity = documentClient.documentByClass(Order.class.getSimpleName(), "findByCustomerTelegramUserIdAndDeletedIsFalseAndDeliveredIsFalseOrderByTimestampAsc", params);
        return documentAsyncService.getResult(distributionMessageResponseEntity.getBody().getCorrelationID());
    }

    private Future<Order> getOrder(Long orderExternalId) {
        List<DocumentRepositoryMethodParameter<?>> params = new ArrayList<>(1);
        params.add(new DocumentRepositoryMethodParameter<Long>(Long.class, orderExternalId));
        ResponseEntity<DistributionMessage<Void>> distributionMessageResponseEntity = documentClient.documentByClass(Order.class.getSimpleName(), "findByExternalShortId", params);
        return documentAsyncService.getUniqueResult(distributionMessageResponseEntity.getBody().getCorrelationID());
    }

    @Override
    public Order getOrderProcessed(Long orderExternalId) {
        List<DocumentRepositoryMethodParameter<?>> params = new ArrayList<>(1);
        params.add(new DocumentRepositoryMethodParameter<Long>(Long.class, orderExternalId));
        ResponseEntity<DistributionMessage<Void>> distributionMessageResponseEntity = documentClient.documentByClass(Order.class.getSimpleName(), "findByExternalShortId", params);
        Order order = null;
        try {
            order = (Order) documentAsyncService.getUniqueResult(distributionMessageResponseEntity.getBody().getCorrelationID()).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage());
        }

        params.clear();
        params.add(new DocumentRepositoryMethodParameter<String>(String.class, order.getId()));
        params.add(new DocumentRepositoryMethodParameter<Long>(Long.class, order.getCustomerTelegramUserId()));
        distributionMessageResponseEntity = documentClient.documentByClass(Payment.class.getSimpleName(), "findTopByOrderIdAndCustomerTelegramUserIdOrderByTimestampDesc", params);
        try {
            Payment payment = (Payment) documentAsyncService.getUniqueResult(distributionMessageResponseEntity.getBody().getCorrelationID()).get();
            if (payment != null) {
                log.info(String.format("Order ID [%s] has been payed by Payment ID [%s]", order.getId(), payment.getId()));
                order.setPaid(Boolean.TRUE);
            } else {
                order.setPaid(Boolean.FALSE);
            }

        } catch (ExecutionException e) {
            if (NoSuchElementException.class == e.getCause().getClass()) {
                order.setPaid(Boolean.FALSE);
            } else {
                log.error(e.getMessage());
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
        return order;
    }

    @Override
    public void payOrder(Long orderExternalId, String customerNickname, Long customerTelegramUserId) throws GistGuildGenericException {
        UUID correlationID = null;
        try {
            Order order = getOrder(orderExternalId).get();
            Payment payment = new Payment();
            payment.setOrderId(order.getId());
            payment.setAmount(order.getAmount());
            payment.setCustomerNickname(customerNickname);
            payment.setCustomerTelegramUserId(customerTelegramUserId);
            DocumentProposition documentProposition = new DocumentProposition();
            documentProposition.setDocumentPropositionType(DocumentPropositionType.ORDER_PAYMENT_CONFIRMATION);
            documentProposition.setDocumentClass(Payment.class.getSimpleName());
            documentProposition.setDocument(payment);
            ResponseEntity<DistributionMessage<DocumentProposition>> distributionMessageResponseEntity = documentClient.itemProposition(documentProposition);
            correlationID = distributionMessageResponseEntity.getBody().getCorrelationID();
            GuiConcurrenceService.getCorrelationIDs().add(correlationID);
            documentAsyncService.getUniqueResult(correlationID).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage());
            if (correlationID != null) {
                List<? extends GistGuildGenericException> exceptionResult = documentAsyncService.getExceptionResult(correlationID);
                if (exceptionResult != null && !exceptionResult.isEmpty()) {
                    throw exceptionResult.iterator().next();
                }
            }
        }
    }

    @Override
    public Future<RechargeCredit> getCredit(Long telegramUserId) {
        List<DocumentRepositoryMethodParameter<?>> params = new ArrayList<>(1);
        params.add(new DocumentRepositoryMethodParameter<Long>(Long.class, telegramUserId));
        ResponseEntity<DistributionMessage<Void>> distributionMessageResponseEntity = documentClient.documentByClass(RechargeCredit.class.getSimpleName(), "findTopByCustomerTelegramUserIdOrderByTimestampDesc", params);
        return documentAsyncService.getUniqueResult(distributionMessageResponseEntity.getBody().getCorrelationID());
    }

    @Override
    public Future<RechargeCredit> addCredit(RechargeCredit rechargeCredit) {
        DocumentProposition documentProposition = new DocumentProposition();
        documentProposition.setDocumentPropositionType(DocumentPropositionType.RECHARGE_USER_CREDIT);
        documentProposition.setDocumentClass(RechargeCredit.class.getSimpleName());
        documentProposition.setDocument(rechargeCredit);
        ResponseEntity<DistributionMessage<DocumentProposition>> distributionMessageResponseEntity = documentClient.itemProposition(documentProposition);
        GuiConcurrenceService.getCorrelationIDs().add(distributionMessageResponseEntity.getBody().getCorrelationID());
        return documentAsyncService.getUniqueResult(distributionMessageResponseEntity.getBody().getCorrelationID());
    }

    @Override
    public void saveAction(Action action) {
        actionRepository.save(action);
    }

    @Override
    public void deleteActionInProgress(Action action) {
        actionRepository.delete(action);
    }
}
