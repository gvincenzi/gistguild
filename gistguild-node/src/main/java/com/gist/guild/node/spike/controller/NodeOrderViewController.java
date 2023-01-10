package com.gist.guild.node.spike.controller;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.DocumentPropositionType;
import com.gist.guild.commons.message.entity.DocumentProposition;
import com.gist.guild.node.binding.CorrelationIdCache;
import com.gist.guild.node.core.configuration.StartupConfig;
import com.gist.guild.node.core.document.Order;
import com.gist.guild.node.core.document.Payment;
import com.gist.guild.node.core.document.Product;
import com.gist.guild.node.core.repository.OrderRepository;
import com.gist.guild.node.core.repository.PaymentRepository;
import com.gist.guild.node.core.repository.ProductRepository;
import com.gist.guild.node.core.service.NodeService;
import com.gist.guild.node.spike.client.SpikeClient;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Log
@Controller
public class NodeOrderViewController {
    @Value("${spring.application.name}")
    private String instanceName;

    @Autowired
    OrderRepository repository;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    NodeService<com.gist.guild.commons.message.entity.Order, Order> nodeService;

    @Autowired
    SpikeClient spikeClient;

    @Autowired
    CorrelationIdCache correlationIdCache;

    @GetMapping("/order")
    public String welcome(Principal principal, Model model) throws GistGuildGenericException {
        List<Order> items = repository.findByProductOwnerTelegramUserIdOrderByTimestampDesc(Long.parseLong(principal.getName()));
        Iterator<Order> orderIterator = items.iterator();
        while(orderIterator.hasNext()){
            Order next = orderIterator.next();
            List<Payment> payments = paymentRepository.findTopByOrderIdAndCustomerTelegramUserIdOrderByTimestampDesc(next.getId(), next.getCustomerTelegramUserId());
            if (payments.size() > 0) {
                next.setPaid(Boolean.TRUE);
            } else {
                next.setPaid(Boolean.FALSE);
            }
        }

        model.addAttribute("instanceName", instanceName);
        model.addAttribute("validation", nodeService.validate(items));
        model.addAttribute("startup", StartupConfig.getStartupProcessed());
        Collections.sort(items);
        Collections.reverse(items);
        model.addAttribute("items", items);
        model.addAttribute("inProgress", Boolean.FALSE);

        model.addAttribute("newOrder", new com.gist.guild.commons.message.entity.Order());

        return "order"; //view
    }

    @GetMapping("/orderInProgress")
    public String orderInProgress(Principal principal, Model model) throws GistGuildGenericException {
        List<Order> items = repository.findByProductOwnerTelegramUserIdAndDeletedIsFalseAndDeliveredIsFalseOrderByTimestampDesc(Long.parseLong(principal.getName()));
        Iterator<Order> orderIterator = items.iterator();
        while(orderIterator.hasNext()){
            Order next = orderIterator.next();
            List<Payment> payments = paymentRepository.findTopByOrderIdAndCustomerTelegramUserIdOrderByTimestampDesc(next.getId(), next.getCustomerTelegramUserId());
            if (payments.size() > 0) {
                next.setPaid(Boolean.TRUE);
            } else {
                next.setPaid(Boolean.FALSE);
            }
        }

        model.addAttribute("instanceName", instanceName);
        model.addAttribute("validation", nodeService.validate(items));
        model.addAttribute("startup", StartupConfig.getStartupProcessed());
        Collections.sort(items);
        Collections.reverse(items);
        model.addAttribute("items", items);
        model.addAttribute("inProgress", Boolean.TRUE);

        model.addAttribute("newOrder", new com.gist.guild.commons.message.entity.Order());

        return "order"; //view
    }

    @GetMapping("/order/{id}")
    public String prepareModifyProduct(Principal principal, Model model, @PathVariable String id) throws GistGuildGenericException {
        List<Order> items = repository.findByProductOwnerTelegramUserIdOrderByTimestampDesc(Long.parseLong(principal.getName()));
        com.gist.guild.commons.message.entity.Order toModify = new com.gist.guild.commons.message.entity.Order();
        model.addAttribute("instanceName", instanceName);
        Iterator<Order> orderIterator = items.iterator();

        while(orderIterator.hasNext()){
            Order next = orderIterator.next();
            if(next.getId().equals(id)){
                toModify = next;
                break;
            }
        }

        model.addAttribute("validation", nodeService.validate(items));
        model.addAttribute("startup", StartupConfig.getStartupProcessed());
        Collections.sort(items);
        Collections.reverse(items);
        model.addAttribute("items", items);

        model.addAttribute("inProgress", Boolean.TRUE);
        model.addAttribute("newOrder", toModify);
        return "order"; //view
    }

    @PostMapping("/order")
    public String newOrder(@ModelAttribute com.gist.guild.commons.message.entity.Order newOrder, Model model) throws GistGuildGenericException, InterruptedException {
        DocumentProposition documentProposition = new DocumentProposition();
        documentProposition.setDocumentPropositionType(DocumentPropositionType.ORDER_REGISTRATION);
        documentProposition.setDocumentClass(com.gist.guild.commons.message.entity.Order.class.getSimpleName());
        documentProposition.setDocument(newOrder);
        ResponseEntity<DistributionMessage<DocumentProposition>> distributionMessageResponseEntity = spikeClient.itemProposition(documentProposition);

        try {
            correlationIdCache.getResult(distributionMessageResponseEntity.getBody().getCorrelationID()).get(10000, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            log.severe(e.getMessage());
        } catch (TimeoutException e) {
            log.severe(e.getMessage());
        }

        List<Order> items = repository.findByProductOwnerTelegramUserIdAndDeletedIsFalseAndDeliveredIsFalseOrderByTimestampDesc(newOrder.getProductOwnerTelegramUserId());
        Iterator<Order> orderIterator = items.iterator();
        while(orderIterator.hasNext()){
            Order next = orderIterator.next();
            List<Payment> payments = paymentRepository.findTopByOrderIdAndCustomerTelegramUserIdOrderByTimestampDesc(next.getId(), next.getCustomerTelegramUserId());
            if (payments.size() > 0) {
                next.setPaid(Boolean.TRUE);
            } else {
                next.setPaid(Boolean.FALSE);
            }
        }
        model.addAttribute("instanceName", instanceName);
        model.addAttribute("validation", nodeService.validate(items));
        model.addAttribute("startup", StartupConfig.getStartupProcessed());
        Collections.sort(items);
        Collections.reverse(items);
        model.addAttribute("items", items);
        model.addAttribute("inProgress", Boolean.TRUE);
        model.addAttribute("newOrder", new com.gist.guild.commons.message.entity.Order());

        return "order"; //view
    }
}
