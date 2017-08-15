# Datamos Keywords

This is the list of internal dataMos keywords. For internal use by the dataMos platform.

## Keyword definitions:

---
### _Message related:_

##### :datamos/logistic	    
Defines the logistical part of a message. Containing information about sender, recipient and message meta data.


##### :datamos/rdf-content    
Defines the rdf-content part of a message. Containing the message content plus prefixes in triple or quad format. 
[See W3C RDF Recommendation](https://www.w3.org/TR/rdf-schema/)


##### :datamos/prefix		    
Contains the prefix namespace mapping. Describing the prefixes used in either the rdf:triples or rdf:quads


##### :datamos/triples	    
Contains data in triple format (Subject, Predicate, Object)


##### :datamos/quads		    
Contians data in quad format (Named graph, Subject, Predicate, Object

---
### _Module References:_

##### :datamos/local-register
Key for a modules local register (= var of (atom {)). Used to store other relevant modules and their functions.

---
### _Exchange and Queue Settings:_

##### :datamos/direct
Exchange setting for direct messaging [See RabbitMQ.com](http://www.rabbitmq.com/)


##### :datamos/fanout
Exchange setting for fanout messaging [See RabbitMQ.com](http://www.rabbitmq.com/)

 
##### :datamos/topic
Exchange setting for topic messaging [See RabbitMQ.com](http://www.rabbitmq.com/)

 
##### :datamos/headers
Exchange setting for messaging by header (= default for datamos) [See RabbitMQ.com](http://www.rabbitmq.com/)


##### :datamos/exchange-name
The name of the exchange as used by dataMos.

     
##### :datamos/exchange-type
The type of the exchange (one of direct / fanout / topic / headers )

    
##### :datamos/exchange-settings
The map of settings for the exchange. [See RabbitMQ.com](http://www.rabbitmq.com/)


##### :datamos/binding
The binding configuration for the queue and the exchange.


##### :datamos/queue-name
The name of the queue where the module listens on.


##### :datamos/queue-settings
The settings for the module queue. [See RabbitMQ.com](http://www.rabbitmq.com/)


##### :datamos/remote-channel
Channel using RabbbitMQ to connect with exchange. [See RabbitMQ.com](http://www.rabbitmq.com/)

---
### _Listener & Dispatcher Settings_

##### :datamos/dispatch
Dispatcher which receives messages from RabbitMQ Queue.


##### :datamos/consumer-settings
Default settings for the listener which consumes messages from a RabbitMQ Queue. [See RabbitMQ.com](http://www.rabbitmq.com/)


##### :datamos/listener-tag
The tag by which RabbitMQ knows the listener. [See RabbitMQ.com](http://www.rabbitmq.com/)


##### :datamos/local-channel
Clojure core.async channel for internal processing of the messages received. [See Clojure core.async](https://github.com/clojure/core.async)