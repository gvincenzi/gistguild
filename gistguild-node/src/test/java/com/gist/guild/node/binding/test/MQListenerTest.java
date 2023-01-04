package com.gist.guild.node.binding.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gist.guild.node.binding.MQListener;
import com.gist.guild.node.core.repository.*;
import com.gist.guild.node.core.service.NodeBusinessService;
import com.mongodb.MongoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.MessageChannel;

public class MQListenerTest {
    @Autowired
    MQListener mqListener;

    @MockBean
    MongoClient mongo;

    @MockBean
    @Qualifier("responseChannel")
    MessageChannel responseChannel;

    @MockBean
    NodeBusinessService nodeBusinessService;

    @Value("${spring.application.name}")
    String instanceName;

    @Value("${gistguild.difficult.level}")
    Integer difficultLevel;

    static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockBean
    ProductRepository productRepository;

    @MockBean
    OrderRepository orderRepository;

    @MockBean
    ParticipantRepository participantRepository;

    @MockBean
    RechargeCreditRepository rechargeCreditRepository;

    @MockBean
    PaymentRepository paymentRepository;
}
