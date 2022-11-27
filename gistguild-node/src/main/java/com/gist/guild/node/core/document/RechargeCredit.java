package com.gist.guild.node.core.document;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@Document("recharge_credit")
public class RechargeCredit extends com.gist.guild.commons.message.entity.RechargeCredit {
}
