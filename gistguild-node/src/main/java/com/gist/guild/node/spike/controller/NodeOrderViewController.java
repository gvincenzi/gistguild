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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Log
@Controller
public class NodeOrderViewController {
    private static final String ALL_PRODUCTS = "-";
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
        model.addAttribute("instanceName", instanceName);
        model.addAttribute("startup", StartupConfig.getStartupProcessed());
        Collections.sort(items);
        Collections.reverse(items);

        Set<String> products = new HashSet<>();
        for(Order item : items){
            products.add(item.getProductName());
        }
        products.add(ALL_PRODUCTS);
        model.addAttribute("products", products);

        model.addAttribute("items", items);
        model.addAttribute("inProgress", Boolean.FALSE);

        model.addAttribute("newOrder", new com.gist.guild.commons.message.entity.Order());

        return "order"; //view
    }

    @GetMapping("/orderInProgress")
    public String orderInProgress(Principal principal, Model model) throws GistGuildGenericException {
        List<Order> items = repository.findByProductOwnerTelegramUserIdAndDeletedIsFalseAndDeliveredIsFalseOrderByTimestampDesc(Long.parseLong(principal.getName()));
        model.addAttribute("instanceName", instanceName);
        model.addAttribute("startup", StartupConfig.getStartupProcessed());
        Collections.sort(items);
        Collections.reverse(items);

        Set<String> products = new HashSet<>();
        for(Order item : items){
            products.add(item.getProductName());
        }
        products.add(ALL_PRODUCTS);
        model.addAttribute("products", products);

        model.addAttribute("items", items);
        model.addAttribute("inProgress", Boolean.TRUE);

        model.addAttribute("newOrder", new com.gist.guild.commons.message.entity.Order());

        return "order"; //view
    }

    @GetMapping("/order/{id}")
    public String prepareModifyProduct(Principal principal, Model model, @PathVariable String id) throws GistGuildGenericException {
        List<Order> items = repository.findByProductOwnerTelegramUserIdAndDeletedIsFalseAndDeliveredIsFalseOrderByTimestampDesc(Long.parseLong(principal.getName()));
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
        model.addAttribute("instanceName", instanceName);
        model.addAttribute("startup", StartupConfig.getStartupProcessed());
        Collections.sort(items);
        Collections.reverse(items);
        model.addAttribute("items", items);
        model.addAttribute("inProgress", Boolean.TRUE);
        model.addAttribute("newOrder", new com.gist.guild.commons.message.entity.Order());

        return "order"; //view
    }

    @GetMapping("/order/by/{product}")
    public String orderByProduct(Principal principal, Model model, @PathVariable String product) throws GistGuildGenericException {
        if(ALL_PRODUCTS.equalsIgnoreCase((product))) return welcome(principal,model);
        List<Order> items = repository.findByProductOwnerTelegramUserIdOrderByTimestampDesc(Long.parseLong(principal.getName()));
        model.addAttribute("instanceName", instanceName);
        model.addAttribute("startup", StartupConfig.getStartupProcessed());
        Collections.sort(items);
        Collections.reverse(items);

        Set<String> products = new HashSet<>();
        for(Order item : items){
            products.add(item.getProductName());
        }
        products.add(ALL_PRODUCTS);
        model.addAttribute("products", products);

        //Filter by product
        Set<Order> filteredItems = new HashSet<>();
        for(Order item : items){
            if(item.getProductName().equalsIgnoreCase(product)){
                filteredItems.add(item);
            }
        }

        model.addAttribute("items", filteredItems);
        model.addAttribute("inProgress", Boolean.FALSE);

        model.addAttribute("newOrder", new com.gist.guild.commons.message.entity.Order());

        return "order"; //view
    }

    @GetMapping("/orderInProgress/by/{product}")
    public String orderInProgressByProduct(Principal principal, Model model, @PathVariable String product) throws GistGuildGenericException {
        if(ALL_PRODUCTS.equalsIgnoreCase((product))) return orderInProgress(principal,model);
        List<Order> items = repository.findByProductOwnerTelegramUserIdAndDeletedIsFalseAndDeliveredIsFalseOrderByTimestampDesc(Long.parseLong(principal.getName()));
        model.addAttribute("instanceName", instanceName);
        model.addAttribute("startup", StartupConfig.getStartupProcessed());
        Collections.sort(items);
        Collections.reverse(items);

        Set<String> products = new HashSet<>();
        for(Order item : items){
            products.add(item.getProductName());
        }
        products.add(ALL_PRODUCTS);
        model.addAttribute("products", products);

        //Filter by product
        Set<Order> filteredItems = new HashSet<>();
        for(Order item : items){
            if(item.getProductName().equalsIgnoreCase(product)){
                filteredItems.add(item);
            }
        }

        model.addAttribute("items", filteredItems);
        model.addAttribute("inProgress", Boolean.TRUE);

        model.addAttribute("newOrder", new com.gist.guild.commons.message.entity.Order());

        return "order"; //view
    }

    @GetMapping("/order/export/csv")
    public void exportCSV(Principal principal, HttpServletResponse response) throws IOException {
        List<Order> items = repository.findByProductOwnerTelegramUserIdOrderByTimestampDesc(Long.parseLong(principal.getName()));
        Collections.sort(items);
        Collections.reverse(items);


        response.setContentType("text/csv");
        response.addHeader("Content-Disposition", "attachment; filename=\"orders.csv\"");

        CSVPrinter printer = new CSVPrinter(response.getWriter(), CSVFormat.EXCEL.withDelimiter(';'));
        printer.printRecord("Product name", "Customer nickname", "Quantity", "Amount", "Address", "Insertion date/time", "Status");
        for (Order order : items) {
            String status = "IN PROGRESS";
            if(order.getDeleted()){
                status = "DELETED";
            }
            if(order.getPaymentId() != null) {
                status = "PAID";
            }
            if(order.getDelivered()){
                status = "DELIVERED";
            }
            printer.printRecord(order.getProductName(), order.getCustomerNickname(), order.getQuantity(), order.getAmount(), order.getAddress(), order.getTimestamp(), status);
        }
    }
}
