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
import com.ideal.linked.toposoid.common.{CLAIM, IMAGE, LOCAL, PREDICATE_ARGUMENT, PREMISE, SEMIGLOBAL, SENTENCE, ToposoidUtils}
import com.ideal.linked.toposoid.knowledgebase.model.{KnowledgeBaseNode, KnowledgeFeatureReference}
import com.ideal.linked.toposoid.knowledgebase.regist.model.{KnowledgeForImage, PropositionRelation}
import com.ideal.linked.toposoid.protocol.model.parser.KnowledgeForParser
import com.ideal.linked.toposoid.sentence.transformer.neo4j.QueryManagementUtils.convertList2JsonForKnowledgeFeatureReference
import com.typesafe.scalalogging.LazyLogging

import scala.util.matching.Regex

object QueryManagementForSemiGlobalNode extends LazyLogging{

  val re = "UNION ALL\n$".r
  val langPatternJP: Regex = "^ja_.*".r
  val langPatternEN: Regex = "^en_.*".r

  def executeForSemiGlobalNode(knowledgeForParser: KnowledgeForParser, sentenceType: Int): Unit = {
    createQueryForSemiGlobalNode(
      knowledgeForParser.propositionId,
      knowledgeForParser.sentenceId,
      knowledgeForParser.knowledge.sentence,
      sentenceType,
      knowledgeForParser.knowledge.lang,
      knowledgeForParser.knowledge.KnowledgeForImages
    )
  }

  private def createQueryForSemiGlobalNode(propositionId: String, sentenceId: String, sentence: String, sentenceType: Int, lang: String, knowledgeForImages: List[KnowledgeForImage]): Unit = {
    val insertScript = new StringBuilder
    val semiGlobalNodeId = sentenceId
    //val localContextForFeature = LocalContextForFeature(lang, Map.empty[String, String])
    //val knowledgeFeatureNode = KnowledgeFeatureNode(semiGlobalNodeId, propositionId, sentenceId, sentence, sentenceType, localContextForFeature)
    val nodeType: String = ToposoidUtils.getNodeType(sentenceType, SEMIGLOBAL.index, SENTENCE.index)
    val knowledgeFeatureReference: String = convertList2JsonForKnowledgeFeatureReference(List.empty[KnowledgeFeatureReference])
    insertScript.append("|MERGE (:%s {semiGlobalNodeId:'%s', propositionId:'%s', sentenceId:'%s', sentence:\"%s\", knowledgeFeatureReference:'%s', lang:'%s'})\n".format(
      nodeType,
      semiGlobalNodeId,
      propositionId,
      sentenceId,
      sentence,
      knowledgeFeatureReference,
      lang
    ))

    //Create ImageNode
    knowledgeForImages.filter(x => {
      x.imageReference.reference.isWholeSentence
    }).foldLeft(insertScript) {
      (acc, x) => {
        acc.append(createQueryForImageNode(semiGlobalNodeId, propositionId, sentenceId, sentenceType, x))
      }
    }

    if (insertScript.size != 0) Neo4JAccessor.executeQuery(re.replaceAllIn(insertScript.toString().stripMargin, ""))
    insertScript.clear()
  }

  private def createQueryForImageNode(semiGlobalNodeId: String, propositionId:String, sentenceId:String, sentenceType: Int, knowledgeForImage: KnowledgeForImage): StringBuilder = {
    val nodeType: String = ToposoidUtils.getNodeType(sentenceType, SEMIGLOBAL.index, SENTENCE.index)
    val imageNodeType: String = ToposoidUtils.getNodeType(sentenceType, LOCAL.index, IMAGE.index)
    val insertScript = new StringBuilder
    insertScript.append("|MERGE (:%s {featureId:'%s', url:'%s', propositionId:'%s', sentenceId:'%s', source:'%s'})\n".format(imageNodeType, knowledgeForImage.id, knowledgeForImage.imageReference.reference.url, propositionId, sentenceId, knowledgeForImage.imageReference.reference.originalUrlOrReference))
    insertScript.append("|UNION ALL\n")
    insertScript.append("|MATCH (s:%s {featureId: '%s'}), (d:%s {semiGlobalNodeId: '%s'}) MERGE (s)-[:ImageEdge]->(d)\n".format(imageNodeType, knowledgeForImage.id, nodeType, semiGlobalNodeId))
    insertScript.append("|UNION ALL\n")
    insertScript
  }

  def executeForSemiGlobalLogicRelation(sentenceIds: List[String], propositionRelations: List[PropositionRelation], sentenceType: Int): StringBuilder = {
    propositionRelations.foldLeft(new StringBuilder) {
      (acc, x) => {
        acc.append(createSemiGlobalLogicRelation(sentenceIds, x, sentenceType))
      }
    }
  }

  def createSemiGlobalLogicRelation(sentenceIds: List[String], propositionRelation: PropositionRelation, sentenceType: Int): StringBuilder = {
    val insertScript = new StringBuilder

    val sourceNodeType: String = sentenceType match {
      case -1 => ToposoidUtils.getNodeType(PREMISE.index, SEMIGLOBAL.index, SENTENCE.index)
      case x => ToposoidUtils.getNodeType(x, SEMIGLOBAL.index, SENTENCE.index)
    }
    val destinationNodeType: String = sentenceType match {
      case -1 => ToposoidUtils.getNodeType(CLAIM.index, SEMIGLOBAL.index, SENTENCE.index)
      case x => ToposoidUtils.getNodeType(x, SEMIGLOBAL.index, SENTENCE.index)
    }
    insertScript.append(("|MATCH (s:%s), (d:%s) WHERE (s.semiGlobalNodeId =~'%s.*' AND  d.semiGlobalNodeId =~'%s.*') MERGE (s)-[:LogicEdge {operator:'%s'}]->(d) \n").format(
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
