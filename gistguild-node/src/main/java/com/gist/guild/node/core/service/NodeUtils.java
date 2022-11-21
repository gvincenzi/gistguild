package com.gist.guild.node.core.service;

import com.gist.guild.commons.exception.GistGuildGenericException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class NodeUtils {
    private static final char ZERO = '0';
    private static final String ALGORITHM = "SHA-256";

    public static String applySha256(String input) throws GistGuildGenericException {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1)
                    hexString.append(ZERO);
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new GistGuildGenericException(e.getMessage());
        }
    }
}
