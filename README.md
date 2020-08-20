# Simple Dynamo | Distributed-Systems-CSE-586
This project is developed incrementally. </br>

Each project helps in understanding the concepts of Distributed Systems and also focuses on practical implementation. </br>

Project          | About the project
-----------------|------------------
Simple Messenger | This is the first part towards learning client-server interaction.</br> This project aims at enabling two Android devices to send messages to each other.</br>
Group Messenger1 | This is the second part of the project. A group messenger is developed with a Local Persistent Key-Value table i.e a Content Provider. Here an Android device can interact with multiple devices at the same time, thus implementing multicast. </br>
Group Messenger2 | This is the third part of the project. The multicast group messenger is further extended with Total and FIFO ordering guarantees. The app implements B-multicast. A failure is injected and this project aims implementing a decentralized algorithm.</br>
Simple DHT       | This is the fourth part of the project. This is a simplified version of Chord. This project implements ID space partitioning/re-partitioning, ring-based routing and node joins.</br>
Simple Dynamo    | This is the final part of creating an Amazon Simple Dynamo. This project aims at implementing the three main pieces for Dynamo: Partitioning, Replication and Failure Handling.</br> The main goal here is to implement availability and linearizability at the same time.</br>



