package com.gist.guild.commons.message.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Objects;

@Data
public class Product extends Document {
    private String name;
    private String description;
    private String url;
    private String password;
    private BigDecimal price;
    private Integer availableQuantity;
    private Boolean active = Boolean.TRUE;
    private Boolean delivery = Boolean.FALSE;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return id.equals(product.id) &&
                name.equals(product.name) &&
                description.equals(product.description) &&
                url.equals(product.url) &&
                ((delivery==null && product.delivery==null) || (delivery!=null && delivery.equals(product.delivery))) &&
                ((price==null && product.price==null) || (price!=null && price.equals(product.price))) &&
                ((availableQuantity==null && product.availableQuantity==null) || (availableQuantity!=null && availableQuantity.equals(product.availableQuantity))) &&
                active.equals(product.active);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, active, delivery);
    }
}
