# AMASPJavaLib

Amasp library for java.

This library implements the AMASP (ASCII Master/Slave Protocol) for JAva, a simple way to exchange messages between two computers using serial communication.

AMASP is free and uses four different packets:

MASTER -> SLAVE:

 MRP - Master Request Packet
 CEP - Communication Error Packet

SLAVE -> MASTER:

 SRP - Slave Response Packet 
 SIP - Slave Interruption Packet
 CEP - Communication Error Packet

The protocol is transparent to the user that only needs to use the AMASP Java Library functions to implement his own applications.

AMASPJavaLib is under test and improvements, if you have any problem using it, please send a mail to the author (Spanish, Portuguese or English) adelai@gmail.com.

Contributors will be welcome!

Wants to design an AMASP library to other platforms? Be my guest!

Documentation about AMASP available here:  https://doi.org/10.14209/jcis.2019.1

Author:

Andre L. Delai
adelai@gmail.com

Enjoy, it's free! :)