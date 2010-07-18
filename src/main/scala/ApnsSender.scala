/*
 * Copyright 2010, Mahmood Ali.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *   * Neither the name of Mahmood Ali. nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.notnoop.notifier

import com.notnoop.apns.{APNS, ApnsService}
import com.notnoop.apns.ReconnectPolicy.Provided.EVERY_HALF_HOUR

import org.slf4j.LoggerFactory
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper

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
      .build()

    logger.debug("Notifying {} for message {}", token, message)
    service.push(token, payload)
  }
}

class MQApnsHandler(sender: ApnsSender) extends MQHandler {
  val logger = LoggerFactory.getLogger(getClass)
  val mapper = new ObjectMapper

  def handleRequest(msg: Array[Byte]) = {
    try {
      val rootNode = mapper.readValue(msg, 0, msg.length, classOf[JsonNode])

      val token = rootNode.get("token").getTextValue
      val message = rootNode.get("message").getTextValue
      val threadid = rootNode.get("threadid").getTextValue
      val badgeNode = rootNode.get("badge")
      val badge = if (badgeNode == null) 0 else badgeNode.getIntValue

      sender.sendMessage(token, message, threadid, badge)
      true
    } catch {
      case e =>
        logger.error("Unexpected error while handling message", e)
        false
    }
  }
}

