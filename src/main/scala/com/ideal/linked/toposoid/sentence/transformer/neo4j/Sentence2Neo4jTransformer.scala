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

import com.ideal.linked.common.DeploymentConverter.conf
import com.ideal.linked.data.accessor.neo4j.Neo4JAccessor
import com.ideal.linked.toposoid.common.{CLAIM, PREMISE, ToposoidUtils}
import com.typesafe.scalalogging.LazyLogging
import com.ideal.linked.toposoid.knowledgebase.model.{KnowledgeBaseEdge, KnowledgeBaseNode}
import com.ideal.linked.toposoid.knowledgebase.nlp.model.{NormalizedWord, SynonymList}
import com.ideal.linked.toposoid.knowledgebase.regist.model.Knowledge
import com.ideal.linked.toposoid.protocol.model.base.AnalyzedSentenceObject
import play.api.libs.json.JsResult.Exception
import play.api.libs.json.{JsError, JsValue, Json, __}

import scala.collection.immutable.Set
import scala.util.control.Breaks.{break, breakable}

/**
 * The main implementation of this module is the conversion of predicate-argument-analyzed sentence structures into a knowledge graph.
 * Use Neo4J as the knowledge database.
 */
object Sentence2Neo4jTransformer extends LazyLogging{

  val insertScript = new StringBuilder
  val re = "UNION ALL\n$".r

  /**
   * Main function of this module　
   * @param sentences
   */
  def createGraphAuto(knowledgeList:List[Knowledge]): Unit ={
    for(s <-knowledgeList.filter(_.sentence.size != 0)){
      insertScript.clear()
      val json:String = Json.toJson(s).toString()
      breakable {
        for (i <- 0 to 2) {
          val parseResult: String = ToposoidUtils.callComponent(json, conf.getString("SENTENCE_PARSER_WEB_HOST"), "9001", "analyzeOneSentence")
          if (parseResult != """"{"records":[]}"""") {
            val analyzedSentenceObject: AnalyzedSentenceObject = Json.parse(parseResult).as[AnalyzedSentenceObject]
            analyzedSentenceObject.nodeMap.map(x =>  createQueryForNode(x._2,  x._2.nodeType, s.json))
            if(insertScript.size != 0) Neo4JAccessor.executeQuery(re.replaceAllIn(insertScript.stripMargin, ""))
            insertScript.clear()
            analyzedSentenceObject.edgeList.map(createQueryForEdgeForAuto(analyzedSentenceObject.nodeMap, _))
            if(insertScript.size != 0) Neo4JAccessor.executeQuery(re.replaceAllIn(insertScript.stripMargin, ""))
            break
          }else{
            logger.error(i.toString +  " callComponent Error")
          }
          if(i ==2) Json.parse(parseResult).as[SynonymList]
        }
      }
    }
  }

  /**
   *　This function outputs a query for nodes other than synonyms.
   * @param node
   * @param sentenceType
   */
  private def createQueryForNode(node:KnowledgeBaseNode,sentenceType:Int, json:String): Unit = {

    val nodeType: String = ToposoidUtils.getNodeType(sentenceType)

    insertScript.append("|MERGE (:%s {nodeName: '%s', nodeId:'%s', propositionId:'%s', currentId:'%s', parentId:'%s', isMainSection:'%s', surface:'%s', normalizedName:'%s', dependType:'%s', caseType:'%s', namedEntity:'%s', rangeExpressions:'%s', categories:'%s', domains:'%s', isDenial:'%s',isConditionalConnection:'%s',normalizedNameYomi:'%s',surfaceYomi:'%s',modalityType:'%s',logicType:'%s',extentText:'%s' })\n".format(
      nodeType,
      node.normalizedName,
      node.nodeId,
      node.propositionId,
      node.currentId,
      node.parentId,
      node.isMainSection,
      node.surface,
      node.normalizedName,
      node.dependType,
      node.caseType,
      node.namedEntity,
      convertNestedMapToJson(node.rangeExpressions),
      convertMap2Json(node.categories),
      convertMap2Json(node.domains),
      node.isDenial,
      node.isConditionalConnection,
      node.normalizedNameYomi,
      node.surfaceYomi,
      node.modalityType,
      node.logicType,
      json)
    )
    val normalizedWord = NormalizedWord(node.normalizedName)
    breakable {
      for (i <- 0 to 2) {
        val result: String = ToposoidUtils.callComponent(Json.toJson(normalizedWord).toString(), conf.getString("COMMON_NLP_WEB_HOST"), "9006", "getSynonyms")
        if (result != """"{"records":[]}"""") {
          val synonymList: SynonymList = Json.parse(result).as[SynonymList]
          if (synonymList != null && synonymList.synonyms.size > 0) synonymList.synonyms.map(createQueryForSynonymNode(node, _, sentenceType))
          break
        }else{
          logger.error(i.toString +  " callComponent Error")
        }
        if(i ==2) Json.parse(result).as[SynonymList]
      }
    }
  }
  /**
   * This function outputs a query for synony　nodes and edges.
   * @param node
   * @param synonym
   * @param sentenceType
   */
  private def createQueryForSynonymNode(node:KnowledgeBaseNode, synonym:String, sentenceType:Int): Unit = {
    val nodeType:String = ToposoidUtils.getNodeType(sentenceType)
    insertScript.append("|MERGE (:SynonymNode {nodeId:'%s', nodeName:'%s', propositionId:'%s'})\n".format(synonym + "_" + node.nodeId, synonym,  node.propositionId))
    insertScript.append("|UNION ALL\n")
    insertScript.append("|MATCH (s:SynonymNode {nodeId: '%s'}), (d:%s {nodeId: '%s'}) MERGE (s)-[:SynonymEdge {similality:0.5}]->(d)\n".format(synonym + "_" + node.nodeId, nodeType, node.nodeId))
    insertScript.append("|UNION ALL\n")
  }

  /**
   * This function outputs a query for edges.
   * @param nodeMap
   * @param edge
   */
  private def createQueryForEdgeForAuto(nodeMap:Map[String, KnowledgeBaseNode], edge:KnowledgeBaseEdge): Unit ={

    val sourceNode:Option[KnowledgeBaseNode] = nodeMap.get(edge.sourceId)
    val destinationNode:Option[KnowledgeBaseNode] = nodeMap.get(edge.destinationId)

    val sourceNodeType:String = ToposoidUtils.getNodeType(sourceNode.get.nodeType)
    val destinationNodeType:String =  ToposoidUtils.getNodeType(destinationNode.get.nodeType)

    if(sourceNode.get.nodeType == destinationNode.get.nodeType){
      //エッジの両端のノードのタイプが同じ
      val edgeType = destinationNode.get.nodeType match{
        case PREMISE.index => "PremiseEdge"
        case CLAIM.index => "ClaimEdge"
      }
      insertScript.append(("|MATCH (s:%s {nodeId: '%s'}), (d:%s {nodeId: '%s'}) MERGE (s)-[:%s {dependType:'%s', caseName:'%s',logicType:'%s'}]->(d) \n").format(
        sourceNodeType,
        edge.sourceId,
        destinationNodeType,
        edge.destinationId,
        edgeType,
        edge.dependType,
        edge.caseStr,
        edge.logicType
      ))
    }else{
      //エッジの両端のノードのタイプが異なる -> LogicEdgeになる
      insertScript.append(("|MATCH (s:%s {nodeId: '%s'}), (d:%s {nodeId: '%s'}) MERGE (s)-[:LogicEdge {operator:'%s'}]->(d) \n").format(
        sourceNodeType,
        edge.sourceId,
        destinationNodeType,
        edge.destinationId,
        edge.logicType
      ))
    }
    insertScript.append("|UNION ALL\n")
  }

  /**
   * Convert named entity information to Json representation
   * @param m
   * @return
   */
  private def convertNestedMapToJson(m:Map[String, Map[String, String]]): String ={
    val json: JsValue = Json.toJson(m)
    Json.stringify(json)
  }

  /**
   * Convert Map object to Json representation
   * @param m
   * @return
   */
  private def convertMap2Json(m:Map[String, String]): String ={
    val json: JsValue = Json.toJson(m)
    Json.stringify(json)
  }

}

