# Nuklei

An experiment in connecting services in various languages via composable standard protocols.

## License (See LICENSE file for full license)

Copyright 2014 Kaazing Corporation, All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

## Attributions

AtomicBuffer and AtomicBufferTest are taken from [SBE](https://github.com/real-logic/simple-binary-encoding).
Queuing mechanism hugely inspired by [Martin Thompson](https://github.com/mjpt777),
[Mike Barker](https://github.com/mikeb01),
[Gil Tene](https://github.com/giltene), [Nitsan Wakart](https://github.com/nitsanw), and discussions on the
[Mechanical Sympathy Google Group](https://groups.google.com/forum/#!forum/mechanical-sympathy)

## Build

You require the following to build Nuklei:

* Latest stable [Oracle JDK 8](http://www.oracle.com/technetwork/java/)
* 3.0.4 or later of [Maven](http://maven.apache.org/)
* Latest [Agrona](https://github.com/real-logic/Agrona) installed in local maven repository

To build and install to local maven repository.

    $ mvn clean install

## Components

- __Nukleus__: interface for service. Also the service itself.
- __MpscRingBuffer__: multiple-producer-single-consumer (MPSC) ring buffer between Nuklei (suitable for inter-process communications)
with Spying support.
- __MpscArrayBuffer__: multiple-producer-single-consumer (MPSC) queue between Nuklei (suitable for in-process communications) with
Spying support.
- __Spy__: means to attach a "sniffer" to a communication channel to spy on the data exchange. May be lossy. Similar to
`tcpdump`. _TODO_
- __Flyweight__: overlay of structured layout over an `AtomicBuffer`.
- __Nuklei__: scheduler interface for one or more Nukleus implementations. Some implementations are:
    - __DedicatedNuklei__: one thread per `Nuklei` scheduler
    - __FjpFreeStandingNuklei__: ForkJoinPool based `Nuklei` scheduler
- __Kompound__: container for `Mikro` based services
    - __Mikro__: interface for pure message based services
    - __Proxy__: interface for sending messages to other services

## TODOs

- Spy for RingBuffer and ArrayBuffer
- Standard Parsers/Flyweights
    - WebSocket (RFC 6455) via HTTP/1.1 Upgrade
    - CoAP (RFC 7252) over WebSocket
