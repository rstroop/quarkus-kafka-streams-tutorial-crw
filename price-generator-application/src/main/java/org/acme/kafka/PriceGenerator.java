package org.acme.kafka;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Random;
import javax.json.bind.JsonbBuilder;
import javax.enterprise.context.ApplicationScoped;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;

import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;

/**
 * A bean producing random prices every 5 seconds.
 * The prices are written to a Kafka topic (prices). The Kafka configuration is specified in the application configuration.
 */
@ApplicationScoped
public class PriceGenerator {

    private Random random = new Random();
    private static final Logger Log = Logger.getLogger(PriceGenerator.class);

    @Outgoing("generated-prices")
    public Multi<KafkaRecord<String, String>> generate() {
        return Multi.createFrom().ticks().every(Duration.ofSeconds(5))
                .onOverflow().drop()
                .map(tick -> {
                    // Get a random price between 1000 and 100 cents
                    Integer cents = random.nextInt(1000 - 100) + 100;

                    // Convert into dollars
                    String dollars = BigDecimal
                        .valueOf(Double.valueOf(cents.doubleValue() / 100))
                        .setScale(2)
                        .toPlainString();

                    Price generatedPrice = new Price(dollars, "USD");

                    Log.infov("Generated a USD price entry: {0}", generatedPrice);

                    return KafkaRecord.of(
                        // Key is the unique ID of this generated USD price
                        generatedPrice.getUuid(),
                        // Value is the price object serialised as JSON
                        JsonbBuilder.create().toJson(generatedPrice)
                    );
                });
    }

}
