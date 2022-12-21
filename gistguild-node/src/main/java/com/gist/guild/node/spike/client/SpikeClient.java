package com.gist.guild.node.spike.client;

import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.entity.DocumentProposition;
import com.gist.guild.node.spike.configuration.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "spike", url = "${gistguild.spike.url}", configuration = FeignClientConfiguration.class)
public interface SpikeClient {
    @PostMapping("/verify")
    DistributionMessage<Void> integrityVerification();

    @PostMapping("/document")
    ResponseEntity<DistributionMessage<DocumentProposition>> itemProposition(@RequestBody DocumentProposition proposition);
}
