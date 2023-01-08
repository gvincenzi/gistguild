package com.gist.guild.node.spike.controller.test;

import com.gist.guild.node.core.document.Participant;
import com.gist.guild.node.core.repository.*;
import com.gist.guild.node.core.service.NodeService;
import com.gist.guild.node.spike.client.SpikeClient;
import com.gist.guild.node.spike.controller.NodeController;
import lombok.extern.java.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Log
@RunWith(SpringRunner.class)
@WebMvcTest(NodeController.class)
@ActiveProfiles("test")
public class NodeControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    ParticipantRepository participantRepository;

    @MockBean
    PaymentRepository paymentRepository;

    @MockBean
    OrderRepository orderRepository;

    @MockBean
    ProductRepository productRepository;

    @MockBean
    RechargeCreditRepository rechargeCreditRepository;

    @MockBean
    SpikeClient spikeClient;

    @MockBean
    NodeService<com.gist.guild.commons.message.entity.Participant, Participant> nodeService;

    @Test
    public void welcome() throws Exception {
        Mockito.when(participantRepository.findAll()).thenReturn(new ArrayList<>());
        mvc.perform(get("/"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
    }
}
