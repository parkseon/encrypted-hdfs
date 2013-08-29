Encrypted-HDFS
==============

Seonyoung Park(https://sites.google.com/a/networks.cnu.ac.kr/dnlab/members/seonyoung-park) and Youngseok Lee(https://sites.google.com/a/networks.cnu.ac.kr/dnlab/members/yslee)


## About 
Encrypted-HDFS is implemented in Java using the CompressionCodec interface and designed to encrypt data in Hadoop Filesystem(HDFS) for further protection against theft or unauthorized access. 
It can work with any Hadoop version with the compression codec interface and decrypt HDFS blocks by a large number of clients concurrently when using the SplittableCompressionCodec interface.

Basic code structure is implemented by reference to BZip2 class in the Hadoop source code. 

## Encryption
The encryption is performed on a client side due to lack of multiple HDFS writing with calling the CompressionCodec interface.
A client splits a file into smaller blocks(chunks), encrypting these blocks, and saves them to HDFS. 

## Decryption
In contrast to the encryption, the decryption in MapReduce can be performed by multiple HDFS tasktrackers in parallel. 
That is, every block is processed by a map task at the HDFS tasktrackers. 
In general, multiple map tasks are executed by a tasktracker up to the number of available map task slots which is usually constrained by the number of CPU cores. 

## Test Result
The evaluation results show a performance degradation of 25% during storing files to AES encrypted HDFS because it is processed by only a client and a single thread. 
In contrast, in the decryption in MapReduce, the overhead is only 5% on average since it is possible to decrypt encrypted files on multiple nodes.

## Building from source

Or you can download the latest JAR

### You require the following to build Netty:

* Latest stable Oracle JDK 6 or later
* Latest stable Apache Maven 

### check out sources

`git clone https://github.com/delipark/encrypted-hdfs.git`


### compile and build encrypted-hdfs.jar

`mvn package`
