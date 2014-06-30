/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudera.sa

import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.StreamingContext._
import org.apache.spark.storage.StorageLevel
import org.apache.spark.{SparkContext, SparkConf}

// scalastyle:off
/**
 * Counts words in text encoded with UTF8 received from the network every second.
 *
 * Usage: NetworkWordCount <master> <hostname> <port>
 *   <master> is the Spark master URL. In local mode, <master> should be 'local[n]' with n > 1.
 *   <hostname> and <port> describe the TCP server that Spark Streaming would connect to receive data.
 *
 * To run this on your local machine, you need to first run a Netcat server
 *    `$ nc -lk 9999`
 * and then run the example
 *    `$ ./bin/run-example org.apache.spark.streaming.examples.NetworkWordCount local[2] localhost 9999`
 *
 * See https://spark.apache.org/docs/latest/streaming-programming-guide.html#a-quick-example for more guidance.
 */
// scalastyle:on
object NetworkWordCount {
  def main(args: Array[String]) {
    if (args.length < 3) {
      System.err.println("Usage: NetworkWordCount <master> <hostname> <port>\n" +
        "In local mode, <master> should be 'local[n]' with n > 1")
      System.exit(1)
    }

    // Process Args
    val conf = new SparkConf()
      .setMaster(args(0))
      .setAppName(this.getClass.getCanonicalName)

    // Create the context with a 1 second batch size
    val ssc = new StreamingContext(conf, Seconds(5))

    // Create a NetworkInputDStream on target ip:port and count the
    // words in input stream of \n delimited text (eg. generated by 'nc')
    val lines = ssc.socketTextStream(args(1), args(2).toInt, StorageLevel.MEMORY_ONLY_SER)
    val words = lines.flatMap(_.split(" "))
    val wordCounts = words.map(x => (x, 1)).reduceByKey(_ + _)
    wordCounts.print()
    ssc.start()
    ssc.awaitTermination()
  }
}