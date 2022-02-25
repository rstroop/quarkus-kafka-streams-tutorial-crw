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
 * A bean producing random Price objects every 5 seconds.
 *
 * The Price objects are serialised as JSON, and are written to a MicroProfile
 * Reactive Messaging Channel. The Channel is configured to send data it has
 * been passed to a Kafka topic.
 * 
 * The Channel configuration can be found in the application.properties file.
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

                // Convert into dollar notation
                String dollars = BigDecimal
                    .valueOf(Double.valueOf(cents.doubleValue() / 100))
                    .setScale(2)
                    .toPlainString();

                Price generatedPrice = new Price(dollars, "USD");

                Log.infov("Generated a USD price entry: {0}", generatedPrice);

                // Return a Kafka record with a UUID string as the key and
                // the Price object in stringified JSON format. This is
                // written to the "generated-prices" channel. This channel
                // is configured in the application.properties
                return KafkaRecord.of(
                    // Key is the unique ID of this generated USD price
                    generatedPrice.getUuid(),
                    // Value is the price object serialised as JSON
                    JsonbBuilder.create().toJson(generatedPrice)
                );
            });
    }

}
