package com.gist.guild.node.spike.controller;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.DocumentPropositionType;
import com.gist.guild.commons.message.entity.DocumentProposition;
import com.gist.guild.commons.message.entity.Participant;
import com.gist.guild.node.core.configuration.StartupConfig;
import com.gist.guild.node.core.document.Product;
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
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Log
@Controller
public class NodeProductViewController {
    @Value("${spring.application.name}")
    private String instanceName;

    @Autowired
    ProductRepository repository;

    @Autowired
    NodeService<com.gist.guild.commons.message.entity.Product,Product> nodeService;

    @Autowired
    SpikeClient spikeClient;

    @GetMapping("/product")
    public String welcome(Model model) throws GistGuildGenericException {
        List<Product> items = repository.findAll();
        model.addAttribute("instanceName", instanceName);
        model.addAttribute("validation", nodeService.validate(items));
        model.addAttribute("startup", StartupConfig.getStartupProcessed());
        model.addAttribute("items", items);

        model.addAttribute("newProduct", new com.gist.guild.commons.message.entity.Product());

        return "product"; //view
    }

    @PostMapping("/product")
    public String newProduct(@ModelAttribute com.gist.guild.commons.message.entity.Product newProduct, Model model) throws GistGuildGenericException, InterruptedException {
        long countBefore = repository.count();

        DocumentProposition documentProposition = new DocumentProposition();
        documentProposition.setDocumentPropositionType(DocumentPropositionType.PRODUCT_REGISTRATION);
        documentProposition.setDocumentClass(com.gist.guild.commons.message.entity.Product.class.getSimpleName());
        documentProposition.setDocument(newProduct);
        spikeClient.itemProposition(documentProposition);

        while(repository.count() == countBefore){
            Thread.sleep(1000);
        }

        List<Product> items = repository.findAll();
        model.addAttribute("instanceName", instanceName);
        model.addAttribute("validation", nodeService.validate(items));
        model.addAttribute("startup", StartupConfig.getStartupProcessed());
        model.addAttribute("items", items);

        model.addAttribute("newProduct", new com.gist.guild.commons.message.entity.Product());

        return "product"; //view
    }

}
