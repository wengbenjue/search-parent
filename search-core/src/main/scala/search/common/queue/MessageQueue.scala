
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
package search.common.queue

import search.common.queue.impl.KafkaMessageQueue

/**
 * You can implements by kafka,metaq...
 *
 * @author soledede
 */
trait MessageQueue {

  def sendMsg(msg: String): Boolean

  def consumeMsg(): String
  

  def size(): Int
  
  def start()
}

object MessageQueue {
  def apply(msgQueueType: String="kafka"): MessageQueue = {
    msgQueueType match {
      case "kafka" => KafkaMessageQueue()
      case _       => null
    }
  }
}

trait IMessageQueueFactory {
  def createMessageQueue: MessageQueue
}