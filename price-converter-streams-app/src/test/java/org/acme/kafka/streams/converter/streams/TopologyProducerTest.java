package org.acme.kafka.streams.converter.streams;

import static org.acme.kafka.streams.converter.streams.TopologyProducer.EUR_PRICES_TOPIC;
import static org.acme.kafka.streams.converter.streams.TopologyProducer.USD_PRICES_TOPIC;

import java.util.Properties;
import java.util.UUID;

import javax.inject.Inject;

import org.acme.kafka.streams.converter.model.Price;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.test.TestRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Testing of the Topology without a broker, using TopologyTestDriver
 */
@QuarkusTest
public class TopologyProducerTest {

    @Inject
    Topology topology;

    TopologyTestDriver testDriver;

    TestInputTopic<String, String> usdPrices;
    TestOutputTopic<String, Price> eurPrices;

    @BeforeEach
    public void setUp(){
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "testApplicationId");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        
        testDriver = new TopologyTestDriver(topology, config);

        usdPrices = testDriver.createInputTopic(USD_PRICES_TOPIC, new StringSerializer(), new StringSerializer());
        eurPrices = testDriver.createOutputTopic(EUR_PRICES_TOPIC, new StringDeserializer(), new ObjectMapperDeserializer<>(Price.class));
    }

    @AfterEach
    public void tearDown(){
        testDriver.close();
    }

    @Test
    public void test() {
        String uuid = UUID.randomUUID().toString();
        usdPrices.pipeInput(
            uuid,
            "{\"uuid\":\"" + uuid + "\",\"price\":\"10.00\",\"currency\":\"USD\"}"
        );

        TestRecord<String, Price> result = eurPrices.readRecord();

        Assertions.assertEquals(result.getKey(), uuid);
        Assertions.assertEquals(result.getValue().currency, "EUR");
        Assertions.assertEquals(result.getValue().price, "8.80");
        Assertions.assertEquals(result.getValue().uuid, uuid);
    }
}
