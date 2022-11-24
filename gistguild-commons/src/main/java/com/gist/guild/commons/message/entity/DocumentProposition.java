package com.gist.guild.commons.message.entity;

import com.gist.guild.commons.message.DocumentPropositionType;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Jacksonized
public class DocumentProposition<T> {
    DocumentPropositionType documentPropositionType;
    String documentClass;
    T document;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentProposition)) return false;

        DocumentProposition<?> documentProposition1 = (DocumentProposition<?>) o;

        if (getDocumentPropositionType() != null ? !getDocumentPropositionType().equals(documentProposition1.getDocumentPropositionType()) : documentProposition1.getDocumentPropositionType() != null)
            return false;
        return getDocument() != null ? getDocument().equals(documentProposition1.getDocument()) : documentProposition1.getDocument() == null;
    }

    @Override
    public int hashCode() {
        int result = (getDocumentPropositionType() != null ? getDocumentPropositionType().hashCode() : 0);
        result = 31 * result + (getDocument() != null ? getDocument().hashCode() : 0);
        return result;
    }
}
