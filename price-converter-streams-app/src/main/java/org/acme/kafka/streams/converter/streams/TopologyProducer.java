package org.acme.kafka.streams.converter.streams;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import org.acme.kafka.streams.converter.model.Price;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
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
            Consumed.with(Serdes.String(), PriceSerde)
        )   
            // A map operation allows us to perform a transformation on each
            // record in the USD_PRICES_TOPIC and write that record elsewhere
            .map((k, v) -> {
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
                
                // Modify the in-memory Price record with the new EUR value
                v.setCurrency("EUR");
                v.setPrice(eurPrice);

                // Return a Key-Value pair using the incoming UUID as the
                // key and the modified Price as the value
                return KeyValue.pair(k, v);
            })
            .to(
                // Write the new Price record to the "eur-prices" Topic
                EUR_PRICES_TOPIC,
                Produced.with(Serdes.String(), PriceSerde)
            );

        return builder.build();
    }
}
