package org.puregxl.distribution.common.constant;

/**
 * 消息队列常量
 */
public class RocketMQConstant {
    /**
     * 消费者队列 （分发逻辑）立即发送
     */
    public static final String COUPON_ACTUAL_TOPIC = "one-coupon_distribution-service_coupon-task-execute_topic";


    /**
     * 消费者组 （分发逻辑）立即发送
     */
    public static final String COUPON_ACTUAL_TOPIC_GROUP = "one-coupon_distribution-service_coupon-task-execute_topic_group";

    /**
     * 分发逻辑 - topic
     */
    public static final String COUPON_DISTRIBUTION_TOPIC = "one-coupon_distribution-service_coupon-execute-distribution_topic";

    /**
     * 分发逻辑 - group
     */
    public static final String COUPON_DISTRIBUTION_TOPIC_GROUP = "one-coupon_distribution-service_coupon-execute-distribution_topic_group";


}
