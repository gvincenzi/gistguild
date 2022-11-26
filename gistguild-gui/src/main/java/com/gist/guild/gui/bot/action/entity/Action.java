package com.gist.guild.gui.bot.action.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "telegram_action")
public class Action {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column
    private String userMailFilter;
    @Column
    private Long telegramUserIdToManage;
    @Column
    private String productIdToManage;
    @Column
    private ActionType actionType;
    @Column
    private Long telegramUserId;
    @Column
    private Long selectedProductId;
    @Column
    private Double quantity;
    @Column
    private Boolean inProgress = Boolean.TRUE;
}
