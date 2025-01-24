package com.socialchat.config;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RocketMQConfig {

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Value("${rocketmq.producer.like-topic.group}")
    private String likeTopicProducerGroup;

    @Value("${rocketmq.producer.collect-topic.group}")
    private String collectTopicProducerGroup;

    @Bean(name = "likeRocketMQTemplate")
    public RocketMQTemplate likeRocketMQTemplate() {
        DefaultMQProducer producer = new DefaultMQProducer(likeTopicProducerGroup);
        producer.setNamesrvAddr(nameServer);
        RocketMQTemplate template = new RocketMQTemplate();
        template.setProducer(producer);
        return template;
    }

    @Bean(name = "collectRocketMQTemplate")
    public RocketMQTemplate collectRocketMQTemplate() {
        DefaultMQProducer producer = new DefaultMQProducer(collectTopicProducerGroup);
        producer.setNamesrvAddr(nameServer);
        RocketMQTemplate template = new RocketMQTemplate();
        template.setProducer(producer);
        return template;
    }
}
