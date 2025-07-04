/*
 * Copyright (C) 2025  Linked Ideal LLC.[https://linked-ideal.com/]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
