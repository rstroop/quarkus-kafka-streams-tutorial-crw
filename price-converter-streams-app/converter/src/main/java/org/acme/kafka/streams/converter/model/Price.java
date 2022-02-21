package org.acme.kafka.streams.converter.model;

import java.util.UUID;

public class Price {
    public String price;
    public String currency;
    public String uuid;

    Price () {
        this.uuid = UUID.randomUUID().toString();
    }

    Price (String uuid, String price, String currency) {
        this.price = price;
        this.currency = currency;
        this.uuid = uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setPrice (String price) {
        this.price = price;
    }

    public String getPrice () {
        return price;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCurrency() {
        return currency;
    }

    @Override
    public String toString () {
        return "[currency=" + currency + ", price=" + price + ", uuid=" + uuid + "]";
    }
}
