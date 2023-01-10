package com.gist.guild.gui.bot.configuration;

import com.gist.guild.commons.message.entity.Order;
import com.gist.guild.commons.message.entity.Product;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "gistguild.bot.message")
public class MessageProperties {
    protected static final String EMPTY_STRING = "";

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
    String message27;
    String message28;

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
    String menuItem19;

    String error1;
    String error2;

    String invoiceTitle;
    String invoiceDescription;
    String invoiceCurrency;
    String invoiceStartParameter;

    String orderDetails1;
    String orderDetails2;
    String orderDetails3;
    String orderDetails4;
    String orderDetails5;
    String orderDetails6;
    String orderDetails7;

    String productDetails1;
    String productDetails2;
    String productDetails3;
    String productDetails4;

    String loadingMessage;

    public String toString(Order order) {
        return String.format(getOrderDetails1(),order.getExternalShortId()) +
                (order.getQuantity()!=null ? String.format(getOrderDetails2(), order.getQuantity()) : EMPTY_STRING) +
                (order.getAddress()!=null ? String.format(getOrderDetails3(),order.getAddress()) : EMPTY_STRING) +
                (order.getAmount()!=null ? String.format(getOrderDetails4(),order.getAmount()) : EMPTY_STRING ) +
                String.format(getOrderDetails5(), order.getProductName()) +
                (order.getPaid() && order.getProductPassword() != null && order.getProductPassword() != EMPTY_STRING ? String.format(getOrderDetails6(), order.getProductPassword()) : EMPTY_STRING) +
                (order.getPaid() && order.getProductUrl() != null && order.getProductUrl() != EMPTY_STRING ? String.format(getOrderDetails7(), order.getProductUrl()) : EMPTY_STRING);

    }

    public String toString(Product product) {
        return  String.format(getProductDetails1(), product.getName()) +
                String.format(getProductDetails2(), product.getDescription()) +
                String.format(getProductDetails3(), product.getPrice()) +
                (product.getDelivery()!=null && product.getDelivery() ? getProductDetails4() : EMPTY_STRING);
    }
}
