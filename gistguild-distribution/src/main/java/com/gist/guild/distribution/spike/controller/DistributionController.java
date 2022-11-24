package com.gist.guild.distribution.spike.controller;

import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.DocumentRepositoryMethodParameter;
import com.gist.guild.commons.message.entity.DocumentProposition;
import com.gist.guild.distribution.domain.service.valence.DeliveryValenceService;
import com.gist.guild.distribution.exception.DistributionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
public class DistributionController {
    @Autowired
    DeliveryValenceService deliveryValenceService;

    @PostMapping("/document")
    public ResponseEntity<DistributionMessage<DocumentProposition>> itemProposition(@RequestBody DocumentProposition proposition) {
        log.info(String.format("[DISTRIBUTION SPIKE] Item proposition received"));
        DistributionMessage<DocumentProposition> distributionMessage = null;
        try {
            distributionMessage = deliveryValenceService.propose(proposition);
        } catch (DistributionException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(distributionMessage, HttpStatus.GATEWAY_TIMEOUT);
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage());
        }
        return distributionMessage!=null && distributionMessage.getCorrelationID() != null ?
            new ResponseEntity<>(distributionMessage, HttpStatus.OK) :
            new ResponseEntity<>(distributionMessage, HttpStatus.NOT_ACCEPTABLE);
    }

    @PostMapping("/document/{documentClass}/{documentRepositoryMethod}")
    public ResponseEntity<DistributionMessage<Void>> documentByClass(@PathVariable String documentClass,
                                                                     @PathVariable String documentRepositoryMethod,
                                                                     @RequestBody List<DocumentRepositoryMethodParameter> params) {
        log.info(String.format("[DISTRIBUTION SPIKE] Get document %s request received",documentClass));
        DistributionMessage<Void> distributionMessage = null;
        try {
            distributionMessage = deliveryValenceService.sendDocumentClassRequest(documentClass, documentRepositoryMethod, params);
        } catch (DistributionException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(distributionMessage, HttpStatus.GATEWAY_TIMEOUT);
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(distributionMessage, HttpStatus.NOT_ACCEPTABLE);
        }
        return distributionMessage.getCorrelationID() != null ?
                new ResponseEntity<>(distributionMessage, HttpStatus.OK) :
                new ResponseEntity<>(distributionMessage, HttpStatus.NOT_ACCEPTABLE);
    }

    @PostMapping("/verify")
    public ResponseEntity<DistributionMessage<Void>> integrityVerification() {
        log.info(String.format("[DISTRIBUTION SPIKE] Integrity verification request received"));
        DistributionMessage<Void> distributionMessage = null;
        try {
            distributionMessage = deliveryValenceService.sendIntegrityVerificationRequest();
        } catch (DistributionException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(distributionMessage, HttpStatus.GATEWAY_TIMEOUT);
        }
        return distributionMessage.getCorrelationID() != null ?
                new ResponseEntity<>(distributionMessage, HttpStatus.OK) :
                new ResponseEntity<>(distributionMessage, HttpStatus.NOT_ACCEPTABLE);
    }
}
