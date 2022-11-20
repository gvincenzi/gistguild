package com.gist.guild.commons.message.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class GistGuildItem implements Comparable<GistGuildItem>{
    String id;
    String previousId;
    Instant timestamp;
    Integer nonce;
    Document document;
    Participant owner;
    String nodeInstanceName;
    Boolean isCorruptionDetected = Boolean.FALSE;

    @Override
    public int compareTo(GistGuildItem arg0) {
        return getTimestamp().compareTo(arg0.getTimestamp());
    }

    public static GistGuildItem getItemCorruption() {
        GistGuildItem gistGuildItemCorruption = new GistGuildItem();
        gistGuildItemCorruption.setId("CORRUPTION_DETECTION");
        gistGuildItemCorruption.setNodeInstanceName("gistguild-distribution");
        Participant bot = new Participant();
        bot.setNickname("automatic");
        gistGuildItemCorruption.setOwner(bot);
        gistGuildItemCorruption.setIsCorruptionDetected(Boolean.TRUE);
        gistGuildItemCorruption.setTimestamp(Instant.now());
        Document document = new Document();
        document.setDescription("Corruption detected");
        gistGuildItemCorruption.setDocument(document);
        return gistGuildItemCorruption;
    }
}
