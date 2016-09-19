package search.es.client

import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders
import org.elasticsearch.index.query.functionscore.fieldvaluefactor.FieldValueFactorFunctionBuilder
import org.elasticsearch.index.query.functionscore.gauss.GaussDecayFunctionBuilder
import search.common.util.Logging

/**
  * Created by soledede.weng on 2016/9/19.
  */
private[search] object Function extends Logging {


  /**
    * 根据指定字段进行衰减并入相关性打分算法
    *
    * @param field
    * @param scale  衰减的最大范围
    * @param offset 衰减的间隔
    * @param decay  衰减速率
    * @return
    */
  def gaussDecayFunction(field: String, scale: String = "1w", offset: String, decay: Double): GaussDecayFunctionBuilder = {
    val gaussDecayFunction = ScoreFunctionBuilders.gaussDecayFunction(field, scale)
    if (offset != null && !offset.isEmpty) gaussDecayFunction.setOffset(offset)
    if (decay != null && decay > 0) gaussDecayFunction.setDecay(decay)
    gaussDecayFunction
  }


  /**
    * 因子函数查询
    *
    * @param field    需要进行函数计算的字段值，如点击率，投票数等
    * @param modifier 对 fator*field 进行函数二次处理，进行归一化处理
    * @param factor   对field进行影响的因子
    * @return
    */
  def fieldValueFactorFunction(field: String, modifier: String = "none", factor: Float = 1.0f): FieldValueFactorFunctionBuilder = {
    if (field == null) {
      logError("field must not be null for fieldValueFactor function query")
      return null
    }
    val factorFunction = ScoreFunctionBuilders.fieldValueFactorFunction(field).factor(factor)
    if (modifier.trim.equalsIgnoreCase("log1p")) {
      factorFunction.modifier(FieldValueFactorFunction.Modifier.LOG1P)
    } else if (modifier.trim.equalsIgnoreCase("none")) {
      factorFunction.modifier(FieldValueFactorFunction.Modifier.NONE)
    } else if (modifier.trim.equalsIgnoreCase("log")) {
      factorFunction.modifier(FieldValueFactorFunction.Modifier.LOG)
    } else if (modifier.trim.equalsIgnoreCase("log2p")) {
      factorFunction.modifier(FieldValueFactorFunction.Modifier.LOG2P)
    } else if (modifier.trim.equalsIgnoreCase("ln")) {
      factorFunction.modifier(FieldValueFactorFunction.Modifier.LN)
    } else if (modifier.trim.equalsIgnoreCase("ln1p")) {
      factorFunction.modifier(FieldValueFactorFunction.Modifier.LN1P)
    } else if (modifier.trim.equalsIgnoreCase("ln2p")) {
      factorFunction.modifier(FieldValueFactorFunction.Modifier.LN2P)
    } else if (modifier.trim.equalsIgnoreCase("square")) {
      factorFunction.modifier(FieldValueFactorFunction.Modifier.SQUARE)
    } else if (modifier.trim.equalsIgnoreCase("sqrt")) {
      factorFunction.modifier(FieldValueFactorFunction.Modifier.SQRT)
    } else if (modifier.trim.equalsIgnoreCase("reciprocal")) {
      factorFunction.modifier(FieldValueFactorFunction.Modifier.RECIPROCAL)
    }
    factorFunction
  }

}
