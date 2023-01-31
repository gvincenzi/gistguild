package com.gist.guild.node.spike.controller;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.DocumentPropositionType;
import com.gist.guild.commons.message.entity.DocumentProposition;
import com.gist.guild.node.binding.CorrelationIdCache;
import com.gist.guild.node.core.configuration.StartupConfig;
import com.gist.guild.node.core.document.Order;
import com.gist.guild.node.core.repository.OrderRepository;
import com.gist.guild.node.core.repository.PaymentRepository;
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
    SpikeClient spikeClient;

    @Autowired
    CorrelationIdCache correlationIdCache;

    @GetMapping("/statistics")
    public String statistics(Principal principal, Model model) throws GistGuildGenericException {
        model.addAttribute("instanceName", instanceName);
        model.addAttribute("startup", StartupConfig.getStartupProcessed());

        return "statistics"; //view
    }

}
