package com.gist.guild.commons.message.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class Document implements Comparable<Document>{
    String id;
    String previousId;
    Instant timestamp = Instant.now();
    Instant lastUpdateTimestamp = Instant.now();
    Integer nonce;
    String nodeInstanceName;
    Boolean isCorruptionDetected = Boolean.FALSE;
    Long externalShortId;

    @Override
    public int compareTo(Document arg0) {
        return getTimestamp().compareTo(arg0.getTimestamp());
    }

    public static Document getItemCorruption() {
        Document documentCorruption = new Document();
        documentCorruption.setId("CORRUPTION_DETECTION");
        documentCorruption.setNodeInstanceName("gistguild-distribution");
        documentCorruption.setIsCorruptionDetected(Boolean.TRUE);
        documentCorruption.setTimestamp(Instant.now());
        documentCorruption.setLastUpdateTimestamp(Instant.now());
        return documentCorruption;
    }
}
