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

import com.ideal.linked.data.accessor.neo4j.Neo4JAccessor
import com.ideal.linked.toposoid.common.{SENTENCE, ToposoidUtils}
import com.ideal.linked.toposoid.knowledgebase.regist.model.PropositionRelation
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser}
import com.ideal.linked.toposoid.sentence.transformer.neo4j.QueryManagementUtils.convertMap2Json
import com.typesafe.scalalogging.LazyLogging

import scala.util.matching.Regex

object QueryManagementForFeatureNode extends LazyLogging{

  val re = "UNION ALL\n$".r
  val langPatternJP: Regex = "^ja_.*".r
  val langPatternEN: Regex = "^en_.*".r

  def executeForFeature(knowledgeForParser: KnowledgeForParser, sentenceType: Int): Unit = {
    createQueryForFeatureNode(
      knowledgeForParser.propositionId,
      knowledgeForParser.sentenceId,
      knowledgeForParser.knowledge.sentence,
      sentenceType,
      knowledgeForParser.knowledge.lang
    )
  }

  private def createQueryForFeatureNode(propositionId: String, sentenceId: String, sentence: String, sentenceType: Int, lang: String): Unit = {
    val insertScript = new StringBuilder
    val featureNodeId = sentenceId
    //val localContextForFeature = LocalContextForFeature(lang, Map.empty[String, String])
    //val knowledgeFeatureNode = KnowledgeFeatureNode(featureNodeId, propositionId, sentenceId, sentence, sentenceType, localContextForFeature)
    val nodeType: String = ToposoidUtils.getNodeType(sentenceType, SENTENCE.index)
    val referenceIdMap: String = convertMap2Json(Map.empty[String, String])
    insertScript.append("|MERGE (:%s {featureNodeId:'%s', propositionId:'%s', sentenceId:'%s', sentence:\"%s\", referenceIdMap:'%s', lang:'%s'})\n".format(
      nodeType,
      featureNodeId,
      propositionId,
      sentenceId,
      sentence,
      referenceIdMap,
      lang
    ))
    if (insertScript.size != 0) Neo4JAccessor.executeQuery(re.replaceAllIn(insertScript.toString().stripMargin, ""))
    insertScript.clear()
  }

  def executeForFeatureLogicRelation(sentenceIds: List[String], propositionRelations: List[PropositionRelation], sentenceType: Int): StringBuilder = {
    propositionRelations.foldLeft(new StringBuilder) {
      (acc, x) => {
        acc.append(createFeatureLogicRelation(sentenceIds, x, sentenceType))
      }
    }
  }

  def createFeatureLogicRelation(sentenceIds: List[String], propositionRelation: PropositionRelation, sentenceType: Int): StringBuilder = {
    val insertScript = new StringBuilder

    val sourceNodeType: String = ToposoidUtils.getNodeType(sentenceType, SENTENCE.index) match {
      case "UnknownNode" => "PremiseFeatureNode"
      case x => x
    }
    val destinationNodeType: String = ToposoidUtils.getNodeType(sentenceType, SENTENCE.index) match {
      case "UnknownNode" => "ClaimFeatureNode"
      case x => x
    }
    insertScript.append(("|MATCH (s:%s), (d:%s) WHERE (s.featureNodeId =~'%s.*' AND  d.featureNodeId =~'%s.*') MERGE (s)-[:LogicEdge {operator:'%s'}]->(d) \n").format(
      sourceNodeType,
      destinationNodeType,
      sentenceIds(propositionRelation.sourceIndex),
      sentenceIds(propositionRelation.destinationIndex),
      propositionRelation.operator,
      "-" //The lang attribute of LogicalEdge is　defined as　'-'.
    ))
    insertScript.append("|UNION ALL\n")
    insertScript
  }
}
