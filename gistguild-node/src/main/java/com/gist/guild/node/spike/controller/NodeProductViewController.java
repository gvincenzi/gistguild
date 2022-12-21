package com.gist.guild.node.spike.controller;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.message.DocumentPropositionType;
import com.gist.guild.commons.message.entity.DocumentProposition;
import com.gist.guild.node.core.configuration.StartupConfig;
import com.gist.guild.node.core.document.Product;
import com.gist.guild.node.core.repository.ProductRepository;
import com.gist.guild.node.core.service.NodeService;
import com.gist.guild.node.spike.client.SpikeClient;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Iterator;
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

    @GetMapping("/product/{id}")
    public String prepareModifyProduct(Model model, @PathVariable String id) throws GistGuildGenericException {
        List<Product> items = repository.findAll();
        com.gist.guild.commons.message.entity.Product toModify = new com.gist.guild.commons.message.entity.Product();
        model.addAttribute("instanceName", instanceName);
        Iterator<Product> productIterator = items.iterator();

        while(productIterator.hasNext()){
            Product next = productIterator.next();
            if(next.getId().equals(id)){
                toModify = next;
                break;
            }
        }

        model.addAttribute("validation", nodeService.validate(items));
        model.addAttribute("startup", StartupConfig.getStartupProcessed());
        model.addAttribute("items", items);
        model.addAttribute("newProduct", toModify);
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

        Thread.sleep(5000);

        List<Product> items = repository.findAll();
        model.addAttribute("instanceName", instanceName);
        model.addAttribute("validation", nodeService.validate(items));
        model.addAttribute("startup", StartupConfig.getStartupProcessed());
        model.addAttribute("items", items);

        model.addAttribute("newProduct", new com.gist.guild.commons.message.entity.Product());

        return "product"; //view
    }

}
