package com.notnoop.smartpush.notifier

import com.notnoop.apns.{APNS, ApnsService}
import com.notnoop.apns.ReconnectPolicy.Provided.EVERY_HALF_HOUR

import org.slf4j.LoggerFactory

import java.math.BigInteger

import scala.util.parsing.json._

class ApnsSender(service: ApnsService) {
  val logger = LoggerFactory.getLogger(getClass)

  def this(keyStore: String, keyPass: String) = {
    this(APNS.newService().withCert(keyStore, keyPass)
                     .withReconnectPolicy(EVERY_HALF_HOUR)
                     .withProductionDestination()
                     .build())
  }

  def toHex(id: String) = new BigInteger(id).toString(16).toLowerCase()
  def urlOf(threadidHex: String) = "https://mail.google.com/mail/s/#cv/Inbox/" + threadidHex

  def sendMessage(token: String, message: String, threadid: String, badge: Int) {
    val threadidHex = toHex(threadid)
    val url = urlOf(threadidHex)

    val payload = APNS.newPayload().sound("default")
      .alertBody(message)
      .customField("threadid", threadidHex)
      .customField("msg.url", url)
      .badge(badge).shrinkBody()
      .build();

    logger.debug("Notifying {} for message {}", token, message)
    service.push(token, payload)
  }
}

class MQApnsHandler(sender: ApnsSender) extends MQHandler {
  val logger = LoggerFactory.getLogger(getClass)

  def handleRequest(msg: Array[Byte]) = {
    try {
      val json = JSON.parseFull(new String(msg, "UTF-8")).get.asInstanceOf[Map[String, Any]]

      val token = json("token").asInstanceOf[String]
      val message = json("message").asInstanceOf[String]
      val threadid = json("threadid").asInstanceOf[String]
      val badge = json.get("badge").getOrElse(0).asInstanceOf[Number].intValue

      sender.sendMessage(token, message, threadid, badge)
      true
    } catch {
      case e =>
        logger.error("Unexpected error while handling message", e)
        false
    }
  }
}

