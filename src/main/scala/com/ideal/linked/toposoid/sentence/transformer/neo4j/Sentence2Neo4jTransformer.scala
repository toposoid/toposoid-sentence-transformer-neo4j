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
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, KnowledgeSentenceSet, PropositionRelation}
import com.ideal.linked.toposoid.protocol.model.base.AnalyzedSentenceObject
import play.api.libs.json.{JsValue, Json}

import io.jvm.uuid.UUID
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

/**
 * The main implementation of this module is the conversion of predicate-argument-analyzed sentence structures into a knowledge graph.
 * Use Neo4J as the knowledge database.
 */
object Sentence2Neo4jTransformer extends LazyLogging{

  val insertScript = new StringBuilder
  val re = "UNION ALL\n$".r
  val langPatternJP: Regex = "^ja_.*".r
  val langPatternEN: Regex = "^en_.*".r

  /**
   * This function automatically separates the proposition into Premise and Claim, recognizes the structure, and registers the data in GraphDB.
   * @param sentences
   */
  def createGraphAuto(knowledgeList:List[Knowledge]): Unit = Try {
    for(s <-knowledgeList.filter(_.sentence.size != 0)){
      insertScript.clear()
      val json:String = Json.toJson(s).toString()
      val parserInfo:(String, String) = s.lang match {
        case langPatternJP() => (conf.getString("SENTENCE_PARSER_JP_WEB_HOST"), "9001")
        case langPatternEN() => (conf.getString("SENTENCE_PARSER_EN_WEB_HOST"), "9007")
        case _ => throw new Exception("It is an invalid locale or an unsupported locale.")
      }
      val parseResult: String = ToposoidUtils.callComponent(json, parserInfo._1, parserInfo._2, "analyzeOneSentence")
      val analyzedSentenceObject: AnalyzedSentenceObject = Json.parse(parseResult).as[AnalyzedSentenceObject]
      analyzedSentenceObject.nodeMap.map(x =>  createQueryForNode(x._2,  x._2.nodeType, s.lang, s.extentInfoJson, x._2.propositionId))
      if(insertScript.size != 0) Neo4JAccessor.executeQuery(re.replaceAllIn(insertScript.stripMargin, ""))
      insertScript.clear()
      analyzedSentenceObject.edgeList.map(createQueryForEdgeForAuto(analyzedSentenceObject.nodeMap, _, s.lang))
      if(insertScript.size != 0) Neo4JAccessor.executeQuery(re.replaceAllIn(insertScript.stripMargin, ""))
    }
  }match {
    case Success(s) => s
    case Failure(e) => throw e
  }

  /**
   * This function explicitly separates the proposition into Premise and Claim, specifies the structure, and registers the data in GraphDB.
   * @param knowledgeSentenceSet
   */
  def createGraph(knowledgeSentenceSet:KnowledgeSentenceSet): Unit ={
    //Sentences in KnowledgeSet have the same propositionId
    val propositionId:String = UUID.random.toString

    //Get a list of positionIds for Premise and Claim respectively
    val premisePropositionIds =  knowledgeSentenceSet.premiseList.map(execute(_, PREMISE.index, propositionId)).toList
    val claimPropositionIds =  knowledgeSentenceSet.claimList.map(execute(_, CLAIM.index, propositionId)).toList

    insertScript.clear()
    //If the target proposition has multiple Premises, create an Edge on them according to knowledgeSentenceSet.premiseLogicRelation
    if(premisePropositionIds.size > 1) executeForLogicRelation(premisePropositionIds, knowledgeSentenceSet.premiseLogicRelation, PREMISE.index)

    //If the target proposition has multiple Claims, create an Edge on them according to knowledgeSentenceSet.premiseLogicRelation
    if(claimPropositionIds.size > 1) executeForLogicRelation(claimPropositionIds, knowledgeSentenceSet.claimLogicRelation, CLAIM.index)

    //If the target proposition has both Premise and CLaim,
    // select one representative for Premise and one representative for Claim and connect them to Edge.
    // The representative is the node with the 0th INDEX.
    if(premisePropositionIds.size > 0 && claimPropositionIds.size > 0) {
      val propositionRelation = PropositionRelation("IMP", 0, 1)
      createLogicRelation(List(premisePropositionIds(0), claimPropositionIds(0)), propositionRelation, -1)
    }
    if(insertScript.size != 0) Neo4JAccessor.executeQuery(re.replaceAllIn(insertScript.stripMargin, ""))
  }

  /**
   * This function parses the text for each Knowledge and registers it in GraphDB.
   * @param knowledge
   * @param sentenceType
   * @return
   */
  private def execute(knowledge: Knowledge, sentenceType:Int, propositionId:String): String ={

    val json:String = Json.toJson(knowledge).toString()
    val parserInfo:(String, String) = knowledge.lang match {
      case langPatternJP() => (conf.getString("SENTENCE_PARSER_JP_WEB_HOST"), "9001")
      case langPatternEN() => (conf.getString("SENTENCE_PARSER_EN_WEB_HOST"), "9007")
      case _ => throw new Exception("It is an invalid locale or an unsupported locale.")
    }
    val parseResult: String = ToposoidUtils.callComponent(json, parserInfo._1, parserInfo._2, "analyzeOneSentence")
    val analyzedSentenceObject: AnalyzedSentenceObject = Json.parse(parseResult).as[AnalyzedSentenceObject]
    analyzedSentenceObject.nodeMap.map(x =>  createQueryForNode(x._2,  sentenceType, knowledge.lang, knowledge.extentInfoJson, propositionId))
    //As a policy, first register the node.
    //Another option is to loop at the edge and register the node.
    //However, processing becomes complicated because duplicate nodes are created.
    if(insertScript.size != 0) Neo4JAccessor.executeQuery(re.replaceAllIn(insertScript.stripMargin, ""))
    insertScript.clear()
    analyzedSentenceObject.edgeList.map(createQueryForEdge(_, knowledge.lang, sentenceType))
    if(insertScript.size != 0) Neo4JAccessor.executeQuery(re.replaceAllIn(insertScript.stripMargin, ""))

    //Returns the first propositionId.
    analyzedSentenceObject.nodeMap.head._2.propositionId
  }

  /**
   *???This function outputs a query for nodes other than synonyms.
   * @param node
   * @param sentenceType
   */
  private def createQueryForNode(node:KnowledgeBaseNode,sentenceType:Int, lang:String, json:String, propositionId:String): Unit = {

    val nodeType: String = ToposoidUtils.getNodeType(sentenceType)

    insertScript.append("|MERGE (:%s {nodeName: \"%s\", nodeId:'%s', propositionId:'%s', currentId:'%s', parentId:'%s', isMainSection:'%s', surface:\"%s\", normalizedName:\"%s\", dependType:'%s', caseType:'%s', namedEntity:'%s', rangeExpressions:'%s', categories:'%s', domains:'%s', isDenialWord:'%s',isConditionalConnection:'%s',normalizedNameYomi:'%s',surfaceYomi:'%s',modalityType:'%s',logicType:'%s',lang:'%s', extentText:'%s' })\n".format(
      nodeType,
      node.normalizedName,
      node.nodeId,
      propositionId,
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
      node.isDenialWord,
      node.isConditionalConnection,
      node.normalizedNameYomi,
      node.surfaceYomi,
      node.modalityType,
      node.logicType,
      lang,
      json)
    )
    val normalizedWord = NormalizedWord(node.normalizedName)

    val nlpHostInfo:(String, String) = lang match {
      case langPatternJP() => (conf.getString("COMMON_NLP_JP_WEB_HOST"), "9006")
      case langPatternEN() => (conf.getString("COMMON_NLP_EN_WEB_HOST"), "9008")
      case _ => throw new Exception("It is an invalid locale or an unsupported locale.")
    }

    val result: String = ToposoidUtils.callComponent(Json.toJson(normalizedWord).toString(), nlpHostInfo._1, nlpHostInfo._2, "getSynonyms")
    val synonymList: SynonymList = Json.parse(result).as[SynonymList]
    if (synonymList != null && synonymList.synonyms.size > 0) synonymList.synonyms.map(createQueryForSynonymNode(node, _, sentenceType))
  }
  /**
   * This function outputs a query for synony???nodes and edges.
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
   * This function outputs a query for edges for createGraph function.
   * @param nodeMap
   * @param edge
   * @param sentenceType
   */
  private def createQueryForEdge(edge:KnowledgeBaseEdge, lang:String, sentenceType:Int): Unit ={
    val nodeType:String = sentenceType match{
      case PREMISE.index => "PremiseNode"
      case CLAIM.index => "ClaimNode"
    }
    val edgeType:String = sentenceType match{
      case PREMISE.index => "PremiseEdge"
      case CLAIM.index => "ClaimEdge"
    }
    insertScript.append(("|MATCH (s:%s {nodeId: '%s'}), (d:%s {nodeId: '%s'}) MERGE (s)-[:%s {dependType:'%s', caseName:'%s',logicType:'%s'}]->(d) \n").format(
      nodeType,
      edge.sourceId,
      nodeType,
      edge.destinationId,
      edgeType,
      edge.dependType,
      edge.caseStr,
      edge.logicType,
      lang,
    ))
    insertScript.append("|UNION ALL\n")
  }

  /**
   * This function outputs a query for logical edges.
   * This is only used by createGraph function
   * @param propositionIds
   * @param propositionRelations
   * @param sentenceType
   */
  private def executeForLogicRelation(propositionIds:List[String], propositionRelations:List[PropositionRelation], sentenceType:Int): Unit ={
    propositionRelations.map(x => createLogicRelation(propositionIds, x, sentenceType))
  }

  /**
   * This is sub-function of executeForLogicRelation
   * @param propositionIds
   * @param propositionRelation
   * @param sentenceType
   */
  private def createLogicRelation(propositionIds:List[String], propositionRelation:PropositionRelation, sentenceType:Int): Unit ={
    val sourceNodeType:String = sentenceType match{
      case PREMISE.index => "PremiseNode"
      case CLAIM.index => "ClaimNode"
      case _ => "PremiseNode"
    }
    val destinationNodeType:String = sentenceType match{
      case PREMISE.index => "PremiseNode"
      case CLAIM.index => "ClaimNode"
      case _ => "ClaimNode"
    }

    insertScript.append(("|MATCH (s:%s), (d:%s) WHERE (s.nodeId =~'%s.*' AND  d.nodeId =~'%s.*') AND ((s.caseType = '??????'???AND???d.caseType = '??????') OR (s.caseType = 'ROOT'???AND???d.caseType = 'ROOT'))  MERGE (s)-[:LogicEdge {operator:'%s'}]->(d) \n").format(
      sourceNodeType,
      destinationNodeType,
      propositionIds(propositionRelation.sourceIndex),
      propositionIds(propositionRelation.destinationIndex),
      propositionRelation.operator,
      "-" //The lang attribute of LogicalEdge is???defined as???'-'.
    ))
    print(insertScript)
    insertScript.append("|UNION ALL\n")
  }

  /**
   * This function outputs a query for edges.
   * @param nodeMap
   * @param edge
   */
  private def createQueryForEdgeForAuto(nodeMap:Map[String, KnowledgeBaseNode], edge:KnowledgeBaseEdge, lang:String): Unit ={

    val sourceNode:Option[KnowledgeBaseNode] = nodeMap.get(edge.sourceId)
    val destinationNode:Option[KnowledgeBaseNode] = nodeMap.get(edge.destinationId)

    val sourceNodeType:String = ToposoidUtils.getNodeType(sourceNode.get.nodeType)
    val destinationNodeType:String =  ToposoidUtils.getNodeType(destinationNode.get.nodeType)

    if(sourceNode.get.nodeType == destinationNode.get.nodeType){
      //In this case, the node types at both ends of the edge are the same.
      val edgeType = destinationNode.get.nodeType match{
        case PREMISE.index => "PremiseEdge"
        case CLAIM.index => "ClaimEdge"
      }
      insertScript.append(("|MATCH (s:%s {nodeId: '%s'}), (d:%s {nodeId: '%s'}) MERGE (s)-[:%s {dependType:'%s', caseName:'%s',logicType:'%s', lang:'%s'}]->(d) \n").format(
        sourceNodeType,
        edge.sourceId,
        destinationNodeType,
        edge.destinationId,
        edgeType,
        edge.dependType,
        edge.caseStr,
        edge.logicType,
        lang
      ))
    }else{
      //In this case, the types of nodes at both ends of the edge are different. That is, LogicEdge
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

