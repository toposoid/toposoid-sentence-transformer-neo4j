
package com.ideal.linked.toposoid.sentence.transformer.neo4j
import com.ideal.linked.toposoid.knowledgebase.model.KnowledgeFeatureReference
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.{JsValue, Json}

object QueryManagementUtils  extends LazyLogging {
  /**
   * Convert named entity information to Json representation
   *
   * @param m
   * @return
   */
  def convertNestedMapToJson(m: Map[String, Map[String, String]]): String = {
    val json: JsValue = Json.toJson(m)
    Json.stringify(json)
  }

  /**
   * Convert Map object to Json representation
   *
   * @param m
   * @return
   */
  def convertMap2Json(m: Map[String, String]): String = {
    val json: JsValue = Json.toJson(m)
    Json.stringify(json)
  }

  def convertList2Json(l: List[String]): String = {
    val json: JsValue = Json.toJson(l)
    Json.stringify(json)
  }

  def convertList2JsonForKnowledgeFeatureReference(l: List[KnowledgeFeatureReference]): String = {
    val json: JsValue = Json.toJson(l)
    Json.stringify(json)
  }


}
