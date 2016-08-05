package search.solr.client.exception

/**
 * @author soledede
 */
private[search] class SolrClientException(message: String, cause: Throwable)
  extends Exception(message, cause) {

  def this(message: String) = this(message, null)
}
