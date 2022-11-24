package com.gist.guild.node.spike.client;

import com.gist.guild.commons.message.DistributionMessage;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "spike", url = "${gistguild.spike.url}")
public interface SpikeClient {
    @PostMapping("/verify")
    DistributionMessage<Void> integrityVerification();
}
