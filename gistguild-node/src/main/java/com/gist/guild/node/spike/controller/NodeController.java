package com.gist.guild.node.spike.controller;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.node.core.configuration.StartupConfig;
import com.gist.guild.node.core.document.Participant;
import com.gist.guild.node.core.repository.ParticipantRepository;
import com.gist.guild.node.core.service.NodeService;
import com.gist.guild.node.spike.client.SpikeClient;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Log
@Controller
public class NodeController {
    @Value("${spring.application.name}")
    private String instanceName;

    @Value("${gistguild.software.name}")
    private String softwareName;

    @Autowired
    ParticipantRepository participantRepository;

    @Autowired
    NodeService<com.gist.guild.commons.message.entity.Participant, Participant> nodeService;

    @Autowired
    SpikeClient spikeClient;

    @GetMapping("/")
    public String welcome(Model model) throws GistGuildGenericException {
        model.addAttribute("instanceName", instanceName);
        model.addAttribute("softwareName", softwareName);
        model.addAttribute("startup", StartupConfig.getStartupProcessed());
        model.addAttribute("validation", nodeService.validate(participantRepository.findAllByOrderByTimestampAsc()));
        return "welcome"; //view
    }
}
