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
import com.ideal.linked.toposoid.protocol.model.base.{AnalyzedSentenceObject, AnalyzedSentenceObjects}
import com.ideal.linked.toposoid.protocol.model.parser.{InputSentenceForParser, KnowledgeForParser, KnowledgeSentenceSetForParser}
import play.api.libs.json.{JsValue, Json}

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
   * @param propositionIds A list of IDs corresponding to each element in knowledgeList
   * @param knowledgeList
   */
  /*
  @deprecated
  def createGraphAuto(knowledgeForParsers: List[KnowledgeForParser]): Unit = Try {
    for (knowledgeForParser <- knowledgeForParsers){
      if(knowledgeForParser.propositionId.trim != "" &&  knowledgeForParser.sentenceId.trim != "" && knowledgeForParser.knowledge.sentence.size != 0){
        insertScript.clear()
        val json:String = Json.toJson(knowledgeForParser).toString()
        val parserInfo:(String, String) = knowledgeForParser.knowledge.lang match {
          case langPatternJP() => (conf.getString("SENTENCE_PARSER_JP_WEB_HOST"), "9001")
          case langPatternEN() => (conf.getString("SENTENCE_PARSER_EN_WEB_HOST"), "9007")
          case _ => throw new Exception("It is an invalid locale or an unsupported locale.")
        }
        val parseResult: String = ToposoidUtils.callComponent(json, parserInfo._1, parserInfo._2, "analyzeOneSentence")
        val analyzedSentenceObject: AnalyzedSentenceObject = Json.parse(parseResult).as[AnalyzedSentenceObject]
        analyzedSentenceObject.nodeMap.map(x =>  createQueryForNode(x._2,  x._2.nodeType, knowledgeForParser.knowledge.lang, knowledgeForParser.knowledge.extentInfoJson))
        if(insertScript.size != 0) Neo4JAccessor.executeQuery(re.replaceAllIn(insertScript.stripMargin, ""))
        insertScript.clear()
        analyzedSentenceObject.edgeList.map(createQueryForEdgeForAuto(analyzedSentenceObject.nodeMap, _, knowledgeForParser.knowledge.lang))
        if(insertScript.size != 0) Neo4JAccessor.executeQuery(re.replaceAllIn(insertScript.stripMargin, ""))
      }else{
        logger.error("propositionId is empty or sentenceId is empty or knowledge.sentence.size.")
      }
    }
  }match {
    case Success(s) => s
    case Failure(e) => throw e
  }
  */
  /**
   * This function explicitly separates the proposition into Premise and Claim, specifies the structure, and registers the data in GraphDB.
   * @param propositionId Sentences in knowledgeSentenceSet have the same propositionId
   * @param knowledgeSentenceSet
   */
  def createGraph(knowledgeSentenceSetForParser:KnowledgeSentenceSetForParser): Unit ={

    knowledgeSentenceSetForParser.premiseList.map(execute2(_, PREMISE.index))
    knowledgeSentenceSetForParser.claimList.map(execute2(_, CLAIM.index))

    //Get a list of sentenceIds for Premise and Claim respectively
    val premiseSentenceIds = knowledgeSentenceSetForParser.premiseList.map(_.sentenceId)
    val claimSentenceIds = knowledgeSentenceSetForParser.claimList.map(_.sentenceId)

    insertScript.clear()
    //If the target proposition has multiple Premises, create an Edge on them according to knowledgeSentenceSet.premiseLogicRelation
    //if(premisePropositionIds.size > 1) executeForLogicRelation(premisePropositionIds, knowledgeSentenceSetForParser.premiseLogicRelation, PREMISE.index)
    if(premiseSentenceIds.size > 1) executeForLogicRelation(premiseSentenceIds, knowledgeSentenceSetForParser.premiseLogicRelation, PREMISE.index)

    //If the target proposition has multiple Claims, create an Edge on them according to knowledgeSentenceSet.premiseLogicRelation
    if(claimSentenceIds.size > 1) executeForLogicRelation(claimSentenceIds, knowledgeSentenceSetForParser.claimLogicRelation, CLAIM.index)

    //If the target proposition has both Premise and CLaim,
    // select one representative for Premise and one representative for Claim and connect them to Edge.
    // The representative is the node with the 0th INDEX.
    if(premiseSentenceIds.size > 0 && claimSentenceIds.size > 0) {
      val propositionRelation = PropositionRelation("IMP", 0, 1)
      createLogicRelation(List(premiseSentenceIds(0), claimSentenceIds(0)), propositionRelation, -1)
    }
    if(insertScript.size != 0) Neo4JAccessor.executeQuery(re.replaceAllIn(insertScript.stripMargin, ""))
  }

  /**
   * This function parses the text for each Knowledge and registers it in GraphDB.
   * @param knowledge
   * @param sentenceType
   * @return
   */
  private def execute(knowledgeForParser: KnowledgeForParser, sentenceType:Int): Unit ={

    val json:String = Json.toJson(knowledgeForParser).toString()
    val parserInfo:(String, String) = knowledgeForParser.knowledge.lang match {
      case langPatternJP() => (conf.getString("SENTENCE_PARSER_JP_WEB_HOST"), "9001")
      case langPatternEN() => (conf.getString("SENTENCE_PARSER_EN_WEB_HOST"), "9007")
      case _ => throw new Exception("It is an invalid locale or an unsupported locale.")
    }
    val parseResult: String = ToposoidUtils.callComponent(json, parserInfo._1, parserInfo._2, "analyzeOneSentence")
    val analyzedSentenceObject: AnalyzedSentenceObject = Json.parse(parseResult).as[AnalyzedSentenceObject]
    analyzedSentenceObject.nodeMap.map(x =>  createQueryForNode(x._2,  sentenceType, knowledgeForParser.knowledge.lang, knowledgeForParser.knowledge.extentInfoJson))
    //As a policy, first register the node.
    //Another option is to loop at the edge and register the node.
    //However, processing becomes complicated because duplicate nodes are created.
    if(insertScript.size != 0) Neo4JAccessor.executeQuery(re.replaceAllIn(insertScript.stripMargin, ""))
    insertScript.clear()
    analyzedSentenceObject.edgeList.map(createQueryForEdge(_, knowledgeForParser.knowledge.lang, sentenceType))
    if(insertScript.size != 0) Neo4JAccessor.executeQuery(re.replaceAllIn(insertScript.stripMargin, ""))
  }

  private def execute2(knowledgeForParser: KnowledgeForParser, sentenceType:Int): Unit ={

    //Analyze everything as simple sentences as Claims, not just sentenceType
    val inputSentenceForParser = InputSentenceForParser(List.empty[KnowledgeForParser], List(knowledgeForParser))
    val json:String = Json.toJson(inputSentenceForParser).toString()

    val parserInfo:(String, String) = knowledgeForParser.knowledge.lang match {
      case langPatternJP() => (conf.getString("SENTENCE_PARSER_JP_WEB_HOST"), "9001")
      case langPatternEN() => (conf.getString("SENTENCE_PARSER_EN_WEB_HOST"), "9007")
      case _ => throw new Exception("It is an invalid locale or an unsupported locale.")
    }

    val parseResult: String = ToposoidUtils.callComponent(json, parserInfo._1, parserInfo._2, "analyze")
    val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(parseResult).as[AnalyzedSentenceObjects]
    //Since analyzedSentenceObjects is the analysis result of one sentence, it always has one AnalyzedSentenceObject
    if(analyzedSentenceObjects.analyzedSentenceObjects.size > 0){
      val analyzedSentenceObject = analyzedSentenceObjects.analyzedSentenceObjects.head
      analyzedSentenceObject.nodeMap.map(x =>  createQueryForNode(x._2,  sentenceType, knowledgeForParser.knowledge.lang, knowledgeForParser.knowledge.extentInfoJson))
      //As a policy, first register the node.
      //Another option is to loop at the edge and register the node.
      //However, processing becomes complicated because duplicate nodes are created.
      if(insertScript.size != 0) Neo4JAccessor.executeQuery(re.replaceAllIn(insertScript.stripMargin, ""))
      insertScript.clear()
      analyzedSentenceObject.edgeList.map(createQueryForEdge(_, knowledgeForParser.knowledge.lang, sentenceType))
      if(insertScript.size != 0) Neo4JAccessor.executeQuery(re.replaceAllIn(insertScript.stripMargin, ""))
    }
  }



  /**
   *　This function outputs a query for nodes other than synonyms.
   * @param node
   * @param sentenceType
   */
  private def createQueryForNode(node:KnowledgeBaseNode,sentenceType:Int, lang:String, json:String): Unit = {

    val nodeType: String = ToposoidUtils.getNodeType(sentenceType)

    insertScript.append("|MERGE (:%s {nodeName: \"%s\", nodeId:'%s', propositionId:'%s', currentId:'%s', parentId:'%s', isMainSection:'%s', surface:\"%s\", normalizedName:\"%s\", dependType:'%s', caseType:'%s', namedEntity:'%s', rangeExpressions:'%s', categories:'%s', domains:'%s', isDenialWord:'%s',isConditionalConnection:'%s',normalizedNameYomi:'%s',surfaceYomi:'%s',modalityType:'%s',logicType:'%s',lang:'%s', extentText:'%s' })\n".format(
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
      node.isDenialWord,
      node.isConditionalConnection,
      node.normalizedNameYomi,
      node.surfaceYomi,
      node.modalityType,
      node.logicType,
      node.lang,
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
  private def executeForLogicRelation(sentenceIds:List[String], propositionRelations:List[PropositionRelation], sentenceType:Int): Unit ={
    propositionRelations.map(x => createLogicRelation(sentenceIds, x, sentenceType))
  }

  /**
   * This is sub-function of executeForLogicRelation
   * @param propositionIds
   * @param propositionRelation
   * @param sentenceType
   */
  private def createLogicRelation(sentenceIds:List[String], propositionRelation:PropositionRelation, sentenceType:Int): Unit ={
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

    insertScript.append(("|MATCH (s:%s), (d:%s) WHERE (s.nodeId =~'%s.*' AND  d.nodeId =~'%s.*') AND ((s.caseType = '文末'　AND　d.caseType = '文末') OR (s.caseType = 'ROOT'　AND　d.caseType = 'ROOT'))  MERGE (s)-[:LogicEdge {operator:'%s'}]->(d) \n").format(
      sourceNodeType,
      destinationNodeType,
      sentenceIds(propositionRelation.sourceIndex),
      sentenceIds(propositionRelation.destinationIndex),
      propositionRelation.operator,
      "-" //The lang attribute of LogicalEdge is　defined as　'-'.
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

