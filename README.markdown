AMQP Notifier
========================

A performant iPhone notification (APNs) system, that has an AMQP interface.
The project is meant to be run as a standalone service, that maintains
persistent connections to Apple servers.  Clients of the service, simply need
to enqueue notifications requests in a rabbitMQ queue.

How to Compile and Run
=========================

The project is built using Scala (Scala 2.7.7), and uses sbt for building:

Building:

    sbt update compile test

Running:

    sbt run  [configuration-file]

Configuration Files
=======================

A configuration file is needed to set the apns notification and rabbit mq
details.  The tool uses [Configgy library](http://www.lag.net/configgy/) for
config files, and a sample file would look like

    amqp {
        exchange = "notifier"
        queue = "notifier.notifications"
        routing = "notifier.notifications"
    }

    apns {
        keystore.path = "/path/to/apns/certificate.p12"
        keystore.pass = "apns_passcode"
    }

    log {
        filename = "/var/log/iphonenotifier.log"
        roll = "daily"
    }

Expected Message Format
=========================

The AMPQ notifier expects JSON messages, that contain three properties:

    token   -   The Device token the message should go to
    payload -   The JSON payload, as specified by Apple
    expiry (optional)   -   the expiration time of the message, specified in
                seconds from epoch

Sample message:

    {
        "token": "1234567890abcdef...",
        "payload": { "aps": {"alert": "Hello World!"} }
    }

