package com.gist.guild.node.binding.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.message.DistributionEventType;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.entity.Document;
import com.gist.guild.commons.message.entity.DocumentProposition;
import com.gist.guild.node.core.document.*;
import com.gist.guild.node.core.service.NodeService;
import com.gist.guild.node.core.service.NodeUtils;
import lombok.extern.java.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Log
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class MQListenerIntegrityRequestTest extends MQListenerTest{
    @MockBean
    NodeService<com.gist.guild.commons.message.entity.Participant, Participant> participantNodeService;

    @MockBean
    NodeService<com.gist.guild.commons.message.entity.Product, Product> productNodeService;

    @MockBean
    NodeService<com.gist.guild.commons.message.entity.RechargeCredit, RechargeCredit> rechargeCreditNodeService;

    @MockBean
    NodeService<com.gist.guild.commons.message.entity.Payment, Payment> paymentNodeService;

    @MockBean
    NodeService<com.gist.guild.commons.message.entity.Order, Order> orderNodeService;

    @Test
    public void processIntegrityRequestTest1() throws JsonProcessingException, GistGuildGenericException {
        Mockito.when(participantNodeService.validate(ArgumentMatchers.any(List.class))).thenReturn(Boolean.TRUE);
        Mockito.when(productNodeService.validate(ArgumentMatchers.any(List.class))).thenReturn(Boolean.TRUE);
        Mockito.when(orderNodeService.validate(ArgumentMatchers.any(List.class))).thenReturn(Boolean.TRUE);
        Mockito.when(rechargeCreditNodeService.validate(ArgumentMatchers.any(List.class))).thenReturn(Boolean.TRUE);
        Mockito.when(paymentNodeService.validate(ArgumentMatchers.any(List.class))).thenReturn(Boolean.TRUE);
        Mockito.when(responseChannel.send(ArgumentMatchers.any(Message.class))).thenReturn(Boolean.TRUE);

        DistributionMessage<DocumentProposition<?>> msg = new DistributionMessage<>();
        msg.setType(DistributionEventType.INTEGRITY_VERIFICATION);
        msg.setCorrelationID(UUID.randomUUID());

        mqListener.processDocumentProposition(msg);
    }

    @Test
    public void processIntegrityRequestTest2() throws JsonProcessingException, GistGuildGenericException {
        Mockito.when(participantNodeService.validate(ArgumentMatchers.any(List.class))).thenReturn(Boolean.FALSE);

        Mockito.when(productNodeService.validate(ArgumentMatchers.any(List.class))).thenReturn(Boolean.TRUE);
        Mockito.when(orderNodeService.validate(ArgumentMatchers.any(List.class))).thenReturn(Boolean.TRUE);
        Mockito.when(rechargeCreditNodeService.validate(ArgumentMatchers.any(List.class))).thenReturn(Boolean.TRUE);
        Mockito.when(paymentNodeService.validate(ArgumentMatchers.any(List.class))).thenReturn(Boolean.TRUE);
        Mockito.when(responseChannel.send(ArgumentMatchers.any(Message.class))).thenReturn(Boolean.TRUE);

        DistributionMessage<DocumentProposition<?>> msg = new DistributionMessage<>();
        msg.setType(DistributionEventType.INTEGRITY_VERIFICATION);
        msg.setCorrelationID(UUID.randomUUID());

        mqListener.processDocumentProposition(msg);
    }


}
