package search.es.client

import org.elasticsearch.common.lucene.search.function.CombineFunction
import org.elasticsearch.index.query.functionscore.{FunctionScoreQueryBuilder, ScoreFunctionBuilder}
import org.elasticsearch.index.query.{MatchQueryBuilder, MultiMatchQueryBuilder, QueryBuilder, QueryBuilders}

/**
  * Created by soledede.weng on 2016/9/19.
  */
private[search] object Query {

  def functionScoreQuery(scoreFunctionBuilder: ScoreFunctionBuilder, scoreMode: String = "multiply", boostMode: String = "multiply", qb: QueryBuilder): FunctionScoreQueryBuilder = {
    val functionQuery = QueryBuilders.functionScoreQuery(qb)
    functionQuery.scoreMode("multiply")
    if (scoreMode != null) functionQuery.scoreMode(scoreMode)
    functionQuery.boostMode(CombineFunction.MULT)
    if (boostMode.trim.equalsIgnoreCase("multiply")) {
      functionQuery.boostMode(CombineFunction.MULT)
    } else if (boostMode.trim.equalsIgnoreCase("replace")) {
      functionQuery.boostMode(CombineFunction.REPLACE)
    } else if (boostMode.trim.equalsIgnoreCase("sum")) {
      functionQuery.boostMode(CombineFunction.SUM)
    } else if (boostMode.trim.equalsIgnoreCase("avg")) {
      functionQuery.boostMode(CombineFunction.AVG)
    } else if (boostMode.trim.equalsIgnoreCase("min")) {
      functionQuery.boostMode(CombineFunction.MIN)
    } else if (boostMode.trim.equalsIgnoreCase("max")) {
      functionQuery.boostMode(CombineFunction.MAX)
    }
    if (scoreFunctionBuilder == null) return null
    functionQuery.add(scoreFunctionBuilder)
  }


  /**
    *
    * @param query
    * @param op
    * @param tieBreaker
    * @param fuzziness
    * @param minimumShouldMatch
    * @param queryType
    * @param fields
    * @return
    */
  def multiMatchQuery(query: Object, op: String, tieBreaker: Float, fuzziness: String, minimumShouldMatch: String, queryType: String, fields: String*): MultiMatchQueryBuilder = {
    val multiMatchQuery = QueryBuilders.multiMatchQuery(query, fields: _*)

    if (fuzziness != null && !fuzziness.isEmpty) multiMatchQuery.fuzziness(fuzziness)

    if (minimumShouldMatch != null && !minimumShouldMatch.isEmpty) multiMatchQuery.minimumShouldMatch(minimumShouldMatch)

    var operator: MatchQueryBuilder.Operator = MatchQueryBuilder.Operator.OR
    if (op != null && op.trim.equalsIgnoreCase("and")) {
      operator = MatchQueryBuilder.Operator.AND
    }
    var tieBreakerF = tieBreaker
    if (tieBreaker == null) tieBreakerF = 0.0f

    var searchType: MultiMatchQueryBuilder.Type = MultiMatchQueryBuilder.Type.BEST_FIELDS
    if (queryType != null && (queryType.trim.equalsIgnoreCase("most_fields") || queryType.trim.equalsIgnoreCase("mostFields") || queryType.trim.equalsIgnoreCase("most"))) {
      searchType = MultiMatchQueryBuilder.Type.MOST_FIELDS
    } else if (queryType != null && (queryType.trim.equalsIgnoreCase("cross_fields") || queryType.trim.equalsIgnoreCase("crossFields") || queryType.trim.equalsIgnoreCase("cross"))) {
      searchType = MultiMatchQueryBuilder.Type.CROSS_FIELDS
    } else if (queryType != null && queryType.trim.equalsIgnoreCase("phrase")) {
      searchType = MultiMatchQueryBuilder.Type.PHRASE
    } else if (queryType != null && (queryType.trim.equalsIgnoreCase("phrase_prefix") || queryType.trim.equalsIgnoreCase("phrasePrefix"))) {
      searchType = MultiMatchQueryBuilder.Type.PHRASE_PREFIX
    }
    multiMatchQuery
      .operator(operator)
      .tieBreaker(tieBreakerF)
      .`type`(searchType)
  }

}
