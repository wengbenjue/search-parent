package search.solr.client.index.manager

import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.protocol.HttpContext
import search.common.entity.enumeration.HttpRequestMethodType
import search.common.http.HttpClientUtil
import search.solr.client.index.manager.impl.DefaultIndexManager
import search.common.util.Logging

import scala.collection.mutable

/**
  * Created by soledede on 2016/2/29.
  */
class IndexManagerRunner(inexer: IndexManager, request: HttpRequestBase, context: HttpClientContext, callback: (HttpContext, HttpResponse) => Unit) extends Runnable with Logging{

  override def run(): Unit = {
    val startTime = System.currentTimeMillis()
    logInfo(s"request start time(ms):$startTime,\tcurrent threadId:${Thread.currentThread().getId}")
    context.setAttribute(DefaultIndexManager.requestStartTime_key,startTime)
    inexer.execute(request, context, callback)
  }
}
