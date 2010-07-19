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

Logging
=======================

The logging configuration is customizable.  You have the option of using
[logback logger](http://logback.qos.ch/) or the less expressive [Configgy
Logger](http://www.lag.net/configgy/).

Unfortunately, the option is specified in
`project/build/NotifierProject.scala`, as the appropriate lines would need to
be (de)commented out.  Changing the options, require running

    sbt update compile

Logback is a more capable and expressive logging system, but Configgy logger
is easier to setup and configure.

Packaging
=======================

To bundle the project, you can run the shell script `./package.sh`.  The
script would generate a deployable binary folder in `./target/dist`.  You can
launch a daemon of the project by running the command

    ./target/dist/bin/notifier [config-file]

Please note that `notifier` requires the [daemon](http://libslack.org/daemon/).  The `pid` file
would live in `/tmp/iphonenotifier.pid` (when running as user), or in
`/var/run/iphonenotifier.pid` (when running as root).
