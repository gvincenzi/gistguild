package com.gist.guild.node.spike.controller;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.node.core.configuration.StartupConfig;
import com.gist.guild.node.core.document.Order;
import com.gist.guild.node.core.document.Product;
import com.gist.guild.node.core.repository.OrderRepository;
import com.gist.guild.node.core.repository.ProductRepository;
import com.gist.guild.node.core.service.NodeService;
import com.gist.guild.node.spike.client.SpikeClient;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Log
@Controller
public class NodeOrderViewController {
    @Value("${spring.application.name}")
    private String instanceName;

    @Autowired
    OrderRepository repository;

    @Autowired
    NodeService<com.gist.guild.commons.message.entity.Order, Order> nodeService;

    @Autowired
    SpikeClient spikeClient;

    @GetMapping("/order")
    public String welcome(Model model) throws GistGuildGenericException {
        List<Order> items = repository.findAll();
        model.addAttribute("instanceName", instanceName);
        model.addAttribute("validation", nodeService.validate(items));
        model.addAttribute("startup", StartupConfig.getStartupProcessed());
        model.addAttribute("items", items);

        return "order"; //view
    }
}
