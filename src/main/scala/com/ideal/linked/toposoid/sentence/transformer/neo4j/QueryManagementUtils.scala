/*
 * Copyright 2021 Linked Ideal LLC.[https://linked-ideal.com/]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
