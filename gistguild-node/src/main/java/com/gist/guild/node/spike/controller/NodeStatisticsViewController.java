package com.gist.guild.node.spike.controller;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.DocumentPropositionType;
import com.gist.guild.commons.message.entity.DocumentProposition;
import com.gist.guild.node.binding.CorrelationIdCache;
import com.gist.guild.node.core.configuration.StartupConfig;
import com.gist.guild.node.core.document.Order;
import com.gist.guild.node.core.document.Product;
import com.gist.guild.node.core.document.RechargeCredit;
import com.gist.guild.node.core.repository.*;
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
public class NodeStatisticsViewController {
    @Value("${spring.application.name}")
    private String instanceName;

    @Autowired
    ParticipantRepository participantRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    RechargeCreditRepository rechargeCreditRepository;


    @GetMapping("/statistics")
    public String statistics(Principal principal, Model model) throws GistGuildGenericException {
        model.addAttribute("instanceName", instanceName);
        model.addAttribute("startup", StartupConfig.getStartupProcessed());

        long numberOfParticipants = participantRepository.count();
        long numberOfProducts = productRepository.count();
        long numberOfOrders = orderRepository.count();

        model.addAttribute("numberOfParticipants", numberOfParticipants);
        model.addAttribute("numberOfProducts", numberOfProducts);
        model.addAttribute("numberOfOrders", numberOfOrders);


        Iterator<RechargeCredit> rechargeCreditIterator = rechargeCreditRepository.findAll().iterator();
        long numberOfFreeCredit = 0;
        long numberOfUsedCredit = 0;
        long numberOfRechargedCredit = 0;
        while(rechargeCreditIterator.hasNext()){
            RechargeCredit rechargeCredit = rechargeCreditIterator.next();
            switch (rechargeCredit.getRechargeUserCreditType()){
                case FREE: numberOfFreeCredit+=(rechargeCredit.getNewCredit()-rechargeCredit.getOldCredit()); break;
                case PAYMENT: numberOfUsedCredit+=(rechargeCredit.getOldCredit()-rechargeCredit.getNewCredit()); break;
                default : numberOfRechargedCredit +=(rechargeCredit.getNewCredit()-rechargeCredit.getOldCredit()); break;
            }

        }

        model.addAttribute("numberOfFreeCredit", numberOfFreeCredit);
        model.addAttribute("numberOfUsedCredit", numberOfUsedCredit);
        model.addAttribute("numberOfRechargedCredit", numberOfRechargedCredit);

        return "statistics"; //view
    }

}
