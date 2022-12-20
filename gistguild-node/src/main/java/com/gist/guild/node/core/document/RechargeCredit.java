package com.gist.guild.node.core.document;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@Document("recharge_credit")
public class RechargeCredit extends com.gist.guild.commons.message.entity.RechargeCredit {
    @Id
    private String id;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RechargeCredit)) return false;

        RechargeCredit that = (RechargeCredit) o;

        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
