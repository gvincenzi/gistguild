package com.gist.guild.gui.service;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public abstract class GuiConcurrenceService {
    private static volatile Set<UUID> correlationIDs = new HashSet<>();

    public static Set<UUID> getCorrelationIDs() {
        return correlationIDs;
    }
}
