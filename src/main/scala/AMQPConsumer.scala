package com.notnoop.smartpush.notifier

import org.slf4j.LoggerFactory

import com.rabbitmq.client.{ConnectionFactory,ConnectionParameters,Channel}
import com.rabbitmq.client.QueueingConsumer

trait MQHandler {
  def handleRequest(msg: Array[Byte]): Boolean
}

object MQChannel {
  def apply(exchangeName: String, queueName: String, routingKey: String) = {
    val durable = true

    val params = new ConnectionParameters();

    val conFactory = new ConnectionFactory(params);

    val conn = conFactory.newConnection("localhost");
    val channel = conn.createChannel();

    channel.exchangeDeclare(exchangeName, "direct", durable)
    channel.queueDeclare(queueName, durable)
    channel.queueBind(queueName, exchangeName, routingKey)

    channel
  }
}

class NotificationListener(channel: Channel, handler :MQHandler,
  queueName: String) {
  val logger = LoggerFactory.getLogger(getClass)

  def run() = {
    val consumer = new QueueingConsumer(channel)
    channel.basicConsume(queueName, false, consumer)

    while (true) {
      try {
        val delivery = consumer.nextDelivery();
        logger.debug("Received new request")
        val handled = handler.handleRequest(delivery.getBody)
        if (handled) {
          channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }
      } catch {
        case e => logger.error("Error while handling new message", e)
      }
    }
  }
}

