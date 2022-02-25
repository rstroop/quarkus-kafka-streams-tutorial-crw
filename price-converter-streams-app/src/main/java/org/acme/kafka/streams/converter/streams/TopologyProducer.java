package org.acme.kafka.streams.converter.streams;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import org.acme.kafka.streams.converter.model.Price;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.jboss.logging.Logger;

import io.quarkus.kafka.client.serialization.ObjectMapperSerde;

@ApplicationScoped
public class TopologyProducer {
    private static final Logger Log = Logger.getLogger(TopologyProducer.class);

    static final String USD_PRICES_TOPIC = "usd-prices";
    static final String EUR_PRICES_TOPIC = "eur-prices";
    static final BigDecimal EXCHANGE_RATE = BigDecimal.valueOf(0.88);

    @Produces
    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();

        // Create a ObjectMapperSerde (Serialiser/Deserialiser) that 
        // can serialize/deserialize Price objects to/from JSON
        ObjectMapperSerde<Price> PriceSerde = new ObjectMapperSerde<>(Price.class);

        builder.stream(
            USD_PRICES_TOPIC,
            // The record key is a String, and value is a Price object
            // that's encoded in JSON format
            Consumed.with(Serdes.String(), PriceSerde)
        )   
            // A mapValues operation allows us to perform a transformation on
            // the value for each record in the USD_PRICES_TOPIC and write that
            // record, while retaining the existing key, to another topic
            .mapValues((v) -> {
                Log.infov("converting: {0}:", v);
                
                // Don't get too caught up with the rounding math here. It's
                // only an example anyways. Enjoy this Office Space clip:
                // https://www.youtube.com/watch?v=yZjCQ3T5yXo
                BigDecimal bd = BigDecimal
                    .valueOf(Double.valueOf(v.getPrice()))
                    .setScale(2, RoundingMode.HALF_UP);

                String eurPrice = bd
                    .multiply(EXCHANGE_RATE)
                    .setScale(2, RoundingMode.HALF_UP)
                    .toPlainString();
                
                // Modify the Price record with the new EUR value
                v.setCurrency("EUR");
                v.setPrice(eurPrice);

                // Return the modified Price object as the new value
                return v;
            })
            .to(
                // Write the updated Price record to the "eur-prices" Topic
                // The original UUID key will be re-used, but the value now
                // contains the EUR price and currency 
                EUR_PRICES_TOPIC,
                Produced.with(Serdes.String(), PriceSerde)
            );

        return builder.build();
    }
}
