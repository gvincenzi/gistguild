package com.gist.guild.node.spike.controller;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.node.core.configuration.StartupConfig;
import com.gist.guild.node.core.document.Participant;
import com.gist.guild.node.core.repository.ParticipantRepository;
import com.gist.guild.node.core.service.NodeService;
import com.gist.guild.node.core.service.impl.ParticipantServiceImpl;
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
public class NodeController {
    @Value("${spring.application.name}")
    private String instanceName;

    @Autowired
    ParticipantRepository participantRepository;

    @Autowired
    NodeService<com.gist.guild.commons.message.entity.Participant,Participant> nodeService;

    @Autowired
    SpikeClient spikeClient;

    @GetMapping("/")
    public String welcome(Model model) throws GistGuildGenericException {
        List<Participant> items = participantRepository.findAll();
        model.addAttribute("instanceName", instanceName);
        model.addAttribute("validation", nodeService.validate(items));
        model.addAttribute("startup", StartupConfig.startupProcessed);
        model.addAttribute("items", items);

        return "welcome"; //view
    }

    @GetMapping("/init")
    public String init(Model model) {
        spikeClient.integrityVerification();
        model.addAttribute("instanceName", instanceName);
        return "afterInit"; //view
    }
}
