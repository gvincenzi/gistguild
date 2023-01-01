package com.gist.guild.gui.bot.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "gistguild.bot.message")
public class MessageProperties {
    String welcome;
    String message1;
    String message2;
    String message3;
    String message4;
    String message5;
    String message6;
    String message7;
    String message8;
    String message9;
    String message10;
    String message11;
    String message12;
    String message13;
    String message14;
    String message15;
    String message16;
    String message17;
    String message18;
    String message19;
    String message20;
    String message21;
    String message22;
    String message23;
    String message24;
    String message25;
    String message26;

    String menuItem1;
    String menuItem2;
    String menuItem3;
    String menuItem4;
    String menuItem5;
    String menuItem6;
    String menuItem7;
    String menuItem8;
    String menuItem9;
    String menuItem10;
    String menuItem11;
    String menuItem12;
    String menuItem13;
    String menuItem14;
    String menuItem15;
    String menuItem16;
    String menuItem17;
    String menuItem18;

    String error1;
    String error2;

    String invoiceTitle;
    String invoiceDescription;
    String invoiceCurrency;
    String invoiceStartParameter;
}
