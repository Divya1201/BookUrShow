package com.ticketbooking.booking.config;

import com.ticketbooking.common.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Topics owned by booking-service. 3 partitions lets up to 3 consumer instances work in parallel. */
@Configuration
public class KafkaTopicsConfig {

    @Bean
    NewTopic bookingCreated() {
        return TopicBuilder.name(Topics.BOOKING_CREATED).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic bookingConfirmed() {
        return TopicBuilder.name(Topics.BOOKING_CONFIRMED).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic bookingCancelled() {
        return TopicBuilder.name(Topics.BOOKING_CANCELLED).partitions(3).replicas(1).build();
    }
}
