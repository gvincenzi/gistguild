package com.gist.guild.gui.client;

import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.DocumentRepositoryMethodParameter;
import com.gist.guild.commons.message.entity.DocumentProposition;
import com.gist.guild.gui.configuration.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "document", url = "${gistguild.spike.url}/document", configuration = FeignClientConfiguration.class)
public interface DocumentClient {
    @PostMapping("/")
    ResponseEntity<DistributionMessage<DocumentProposition>> itemProposition(@RequestBody DocumentProposition proposition);

    @PostMapping("/{documentClass}/{documentRepositoryMethod}")
    ResponseEntity<DistributionMessage<Void>> documentByClass(@PathVariable String documentClass,
                                                              @PathVariable String documentRepositoryMethod,
                                                              @RequestBody List<DocumentRepositoryMethodParameter<?>> params);
}
