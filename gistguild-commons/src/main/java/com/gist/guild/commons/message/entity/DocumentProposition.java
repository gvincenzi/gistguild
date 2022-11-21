package com.gist.guild.commons.message.entity;

import com.gist.guild.commons.message.DocumentPropositionType;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Jacksonized
public class DocumentProposition<T> {
    String description;
    DocumentPropositionType documentPropositionType;
    String documentClass;
    T document;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentProposition)) return false;

        DocumentProposition<?> documentProposition1 = (DocumentProposition<?>) o;

        if (getDescription() != null ? !getDescription().equals(documentProposition1.getDescription()) : documentProposition1.getDescription() != null)
            return false;
        if (getDocumentPropositionType() != null ? !getDocumentPropositionType().equals(documentProposition1.getDocumentPropositionType()) : documentProposition1.getDocumentPropositionType() != null)
            return false;
        return getDocument() != null ? getDocument().equals(documentProposition1.getDocument()) : documentProposition1.getDocument() == null;
    }

    @Override
    public int hashCode() {
        int result = getDescription() != null ? getDescription().hashCode() : 0;
        result = 31 * result + (getDocumentPropositionType() != null ? getDocumentPropositionType().hashCode() : 0);
        result = 31 * result + (getDocument() != null ? getDocument().hashCode() : 0);
        return result;
    }
}
