/**
  * Copyright [2015] [soledede]
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package search.common.queue.impl

import java.util.concurrent.LinkedBlockingQueue
import java.util.{Collections, Properties}

import kafka.consumer.Consumer
import kafka.consumer.ConsumerConfig
import kafka.producer.{KeyedMessage, ProducerConfig, Producer}
import search.common.config.{SolrConfiguration, Configuration}
import search.common.queue.MessageQueue
import search.common.util.Logging

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

/**
  * @author soledede
  */
class KafkaMessageQueue private() extends MessageQueue with Logging with SolrConfiguration {

  //product

  val props = new Properties()
  props.put("metadata.broker.list", brokers)
  props.put("serializer.class", serializerClass)
  //props.put("partitioner.class", conf.get("kafka.partitioner.class", "kafka.SimplePartitioner"))
  props.put("producer.type", productType)
  //props.put("request.required.acks", "1")

  val producterConfig = new ProducerConfig(props)

  val producer = new Producer[String, String](producterConfig)

  override def sendMsg(msg: String): Boolean = {
    try {
      val t = System.currentTimeMillis()
      val data = new KeyedMessage[String, String](productTopic, msg)
      producer.send(data)
      logInfo(s"kafka send message: ${msg} cost second: ${(System.currentTimeMillis() - t) / 1000}")
      //producer.close();
      true
    } catch {
      case e: Exception => logError(s"kafka send message: ${msg} failed!", e)
        false
    }
  }

  override def consumeMsg(): String = {
    val msg = Collections.synchronizedList(new ListBuffer[String]())
    KafkaMessageQueue.kafkaBlockQueue.take()
  }

  def consumeKafka() = {
    val config = createConsumerConfig(zk,groupId)
    val consumer = Consumer.create(config)

    val topicCountMap = Map(consumerTopic -> 1)
    val consumerMap = consumer.createMessageStreams(topicCountMap)
    val streams = consumerMap.get(consumerTopic).get

    for (stream <- streams) {

      val it = stream.iterator()

      while (it.hasNext() && it != null) {
        val c = it.next()
        val msg = new String(c.message())
        KafkaMessageQueue.kafkaBlockQueue.put(msg)
        // msg.append(new String(it.next().message()))
      }
      //consumer.shutdown()
    }
  }

  def createConsumerConfig(zookeeper: String, groupId: String): ConsumerConfig = {
    val props = new Properties()
    props.put("zookeeper.connect", zookeeper)
    props.put("group.id", groupId)
    props.put("auto.offset.reset", "largest")
    props.put("zookeeper.session.timeout.ms", "15000")
    //props.put("zk.connectiontimeout.ms", "15000")
    props.put("zookeeper.sync.time.ms", "500000")
    props.put("auto.commit.interval.ms", "1000")
    val config = new ConsumerConfig(props)
    config
  }

  private var thread = new Thread("kafka consumer ") {
    setDaemon(true)
    override def run() {
      consumeKafka()
    }
  }

  override def size(): Int = {
    KafkaMessageQueue.kafkaBlockQueue.size()
  }

  override def start() = {
    thread.start()
  }
}

object KafkaMessageQueue {
  var kafka: KafkaMessageQueue = null
  val kafkaBlockQueue = new LinkedBlockingQueue[String]

  def apply(): KafkaMessageQueue = {
    if (kafka == null)
      kafka = new KafkaMessageQueue()
    kafka
  }

  def main(args: Array[String]): Unit = {

  val kafka =  new KafkaMessageQueue()
    kafka.start()
    kafka .sendMsg("this is 测试")
    kafka .sendMsg("test2")
    //KafkaMessageQueue.create().sendMsg("dsf345435345435345")
    //new KafkaMessageQueue().sendMsg("dsfewrjkdsjfskdjfk111111111111111111111111")

    println( kafka.size()+"=哈哈    " + KafkaMessageQueue().consumeMsg)
    Thread.sleep(1000)
    kafka.sendMsg("change")
    println("size"+kafka.size()+"consume:"+KafkaMessageQueue().consumeMsg)
    println("consume:"+KafkaMessageQueue().consumeMsg)
  }
}