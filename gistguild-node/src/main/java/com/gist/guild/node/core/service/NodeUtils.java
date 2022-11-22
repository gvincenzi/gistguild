package com.gist.guild.node.core.service;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.node.core.document.Participant;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class NodeUtils {
    private static final char ZERO = '0';
    private static final String ALGORITHM = "SHA-256";
    private static final String REGEX_DIGIT = "[0-9].*";

    public static boolean isHashResolved(Participant document, Integer difficultLevel) {
        List<Integer> digits = new ArrayList<>(difficultLevel);

        Integer index = 0;
        String hash = document.getId();
        while (index < hash.length() && digits.size() < difficultLevel) {
            String s = hash.substring(index, ++index);
            if (s.matches(REGEX_DIGIT)) {
                digits.add(Integer.parseInt(s));
            }
        }

        Integer sum = digits.parallelStream().reduce(0, Integer::sum);
        return sum % difficultLevel == 0;
    }

    public static String calculateHash(Participant document) throws GistGuildGenericException {
        return NodeUtils.applySha256(
                document.getPreviousId() +
                        document.getTimestamp().toEpochMilli() +
                        document.getNonce() +
                        document.getNodeInstanceName() +
                        document.getMail()
        );
    }

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
