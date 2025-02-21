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
import com.ideal.linked.toposoid.common.{CLAIM, IMAGE, LOCAL, PREDICATE_ARGUMENT, PREMISE, SYNONYM, ToposoidUtils, TransversalState}
import com.ideal.linked.toposoid.knowledgebase.model.{KnowledgeBaseEdge, KnowledgeBaseNode}
import com.ideal.linked.toposoid.knowledgebase.nlp.model.{NormalizedWord, SynonymList}
import com.ideal.linked.toposoid.knowledgebase.regist.model.{KnowledgeForImage, PropositionRelation}
import com.ideal.linked.toposoid.protocol.model.base.{AnalyzedSentenceObject, AnalyzedSentenceObjects}
import com.ideal.linked.toposoid.protocol.model.parser.{AnalyzedPropositionPair, InputSentenceForParser, KnowledgeForParser}
import com.ideal.linked.toposoid.sentence.transformer.neo4j.QueryManagementUtils.{convertList2Json, convertList2JsonForKnowledgeFeatureReference, convertMap2Json, convertNestedMapToJson}
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.Json

import scala.util.matching.Regex
object QueryManagementForLocalNode  extends LazyLogging{


  val re = "UNION ALL\n$".r
  val langPatternJP: Regex = "^ja_.*".r
  val langPatternEN: Regex = "^en_.*".r

  def execute(analyzedPropositionPair: AnalyzedPropositionPair, sentenceType:Int, neo4JUtils: Neo4JUtils, transversalState: TransversalState): Unit = {

    val insertScript = new StringBuilder
    val analyzedSentenceObjects = analyzedPropositionPair.analyzedSentenceObjects.analyzedSentenceObjects
    val knowledgeForParser = analyzedPropositionPair.knowledgeForParser
    //Analyze everything as simple sentences as Claims, not just sentenceType
    //Since analyzedSentenceObjects is the analysis result of one sentence, it always has one AnalyzedSentenceObject
    if (analyzedSentenceObjects.size > 0) {
      val analyzedSentenceObject = analyzedSentenceObjects.head
      analyzedSentenceObject.nodeMap.foldLeft(insertScript) {
        (acc, x) => {
          acc.append(createQueryForNode(x._2, sentenceType, knowledgeForParser.knowledge.lang, knowledgeForParser.knowledge.knowledgeForImages, transversalState))
        }
      }

      //As a policy, first register the node.
      //Another option is to loop at the edge and register the node.
      //However, processing becomes complicated because duplicate nodes are created.
      if (insertScript.size != 0) neo4JUtils.executeQuery(re.replaceAllIn(insertScript.toString().stripMargin, ""), transversalState)
      insertScript.clear()
      analyzedSentenceObject.edgeList.foldLeft(insertScript) {
        (acc, x) => {
          acc.append(createQueryForEdge(x, knowledgeForParser.knowledge.lang, sentenceType))
        }
      }
      if (insertScript.size != 0) neo4JUtils.executeQuery(re.replaceAllIn(insertScript.toString().stripMargin, ""), transversalState)
    }
  }

  /*
  def execute2(knowledgeForParser: KnowledgeForParser, sentenceType: Int, neo4JUtils:Neo4JUtils, transversalState:TransversalState): Unit = {

    val insertScript = new StringBuilder
    //Analyze everything as simple sentences as Claims, not just sentenceType
    val inputSentenceForParser = InputSentenceForParser(List.empty[KnowledgeForParser], List(knowledgeForParser))
    val json: String = Json.toJson(inputSentenceForParser).toString()

    val parserInfo: (String, String) = knowledgeForParser.knowledge.lang match {
      case langPatternJP() => (conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT"))
      case langPatternEN() => (conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST"), conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT"))
      case _ => throw new Exception("It is an invalid locale or an unsupported locale.")
    }

    val parseResult: String = ToposoidUtils.callComponent(json, parserInfo._1, parserInfo._2, "analyze", transversalState)
    val analyzedSentenceObjects: AnalyzedSentenceObjects = Json.parse(parseResult).as[AnalyzedSentenceObjects]
    //Since analyzedSentenceObjects is the analysis result of one sentence, it always has one AnalyzedSentenceObject
    if (analyzedSentenceObjects.analyzedSentenceObjects.size > 0) {
      val analyzedSentenceObject = analyzedSentenceObjects.analyzedSentenceObjects.head

      analyzedSentenceObject.nodeMap.foldLeft(insertScript){
        (acc, x) => {
          acc.append(createQueryForNode(x._2, sentenceType, knowledgeForParser.knowledge.lang, knowledgeForParser.knowledge.knowledgeForImages, transversalState))
        }
      }

      //As a policy, first register the node.
      //Another option is to loop at the edge and register the node.
      //However, processing becomes complicated because duplicate nodes are created.
      if (insertScript.size != 0) neo4JUtils.executeQuery(re.replaceAllIn(insertScript.toString().stripMargin, ""), transversalState)
      insertScript.clear()
      analyzedSentenceObject.edgeList.foldLeft(insertScript){
        (acc, x) => {
          acc.append(createQueryForEdge(x, knowledgeForParser.knowledge.lang, sentenceType))
        }
      }
      if (insertScript.size != 0) neo4JUtils.executeQuery(re.replaceAllIn(insertScript.toString().stripMargin, ""), transversalState)
    }
  }
  */
  /**
   * 　This function outputs a query for nodes other than synonyms.
   *
   * @param node
   * @param sentenceType
   */
  private def createQueryForNode(node: KnowledgeBaseNode, sentenceType: Int, lang: String, knowledgeForImages:List[KnowledgeForImage], transversalState:TransversalState): StringBuilder = {

    val nodeType: String = ToposoidUtils.getNodeType(sentenceType, LOCAL.index, PREDICATE_ARGUMENT.index)
    val insertScript= new StringBuilder
    //val testList = List(KnowledgeFeatureReference("id1", 0, "", "", 0, """{"test": "hoge"}"""), KnowledgeFeatureReference("id2", 0, "", "", 0, """{"test2": "fuga"}"""))

    insertScript.append("|MERGE (:%s {nodeName: \"%s\", nodeId:'%s', propositionId:'%s', sentenceId:'%s', currentId:'%s', parentId:'%s', isMainSection:'%s', surface:\"%s\", normalizedName:\"%s\", dependType:'%s', caseType:'%s', namedEntity:'%s', rangeExpressions:'%s', categories:'%s', domains:'%s', knowledgeFeatureReferences:'%s', isDenialWord:'%s',isConditionalConnection:'%s',normalizedNameYomi:'%s',surfaceYomi:'%s',modalityType:'%s',logicType:'%s',morphemes:'%s',lang:'%s'})\n".format(
      nodeType,
      node.predicateArgumentStructure.normalizedName,
      node.nodeId,
      node.propositionId,
      node.sentenceId,
      node.predicateArgumentStructure.currentId,
      node.predicateArgumentStructure.parentId,
      node.predicateArgumentStructure.isMainSection,
      node.predicateArgumentStructure.surface,
      node.predicateArgumentStructure.normalizedName,
      node.predicateArgumentStructure.dependType,
      node.predicateArgumentStructure.caseType,
      node.localContext.namedEntity,
      convertNestedMapToJson(node.localContext.rangeExpressions),
      convertMap2Json(node.localContext.categories),
      convertMap2Json(node.localContext.domains),
      convertList2JsonForKnowledgeFeatureReference(node.localContext.knowledgeFeatureReferences),
      node.predicateArgumentStructure.isDenialWord,
      node.predicateArgumentStructure.isConditionalConnection,
      node.predicateArgumentStructure.normalizedNameYomi,
      node.predicateArgumentStructure.surfaceYomi,
      node.predicateArgumentStructure.modalityType,
      node.predicateArgumentStructure.parallelType,
      convertList2Json(node.predicateArgumentStructure.morphemes),
      node.localContext.lang
    ))
    val normalizedWord = NormalizedWord(node.predicateArgumentStructure.normalizedName)

    val nlpHostInfo: (String, String) = lang match {
      case langPatternJP() => (conf.getString("TOPOSOID_COMMON_NLP_JP_WEB_HOST"), conf.getString("TOPOSOID_COMMON_NLP_JP_WEB_PORT"))
      case langPatternEN() => (conf.getString("TOPOSOID_COMMON_NLP_EN_WEB_HOST"), conf.getString("TOPOSOID_COMMON_NLP_EN_WEB_PORT"))
      case _ => throw new Exception("It is an invalid locale or an unsupported locale.")
    }

    //create ImageNode
    knowledgeForImages.filter(x => {
      x.imageReference.reference.surfaceIndex == node.predicateArgumentStructure.currentId && x.imageReference.reference.surface == node.predicateArgumentStructure.surface && !x.imageReference.reference.isWholeSentence
    }).foldLeft(insertScript){
      (acc, x) => {
        acc.append(createQueryForImageNode(node, sentenceType, x))
      }
    }

    //create SynonymNode
    val result: String = ToposoidUtils.callComponent(Json.toJson(normalizedWord).toString(), nlpHostInfo._1, nlpHostInfo._2, "getSynonyms", transversalState)
    val synonymList: SynonymList = Json.parse(result).as[SynonymList]
    if (synonymList != null && synonymList.synonyms.size > 0) {
      synonymList.synonyms.foldLeft(insertScript){
        (acc, x) => {
          acc.append(createQueryForSynonymNode(node, x, sentenceType))
        }
      }
    }
    else insertScript
  }

  /**
   * This function outputs a query for synony　nodes and edges.
   *
   * @param node
   * @param synonym
   * @param sentenceType
   */
  private def createQueryForSynonymNode(node: KnowledgeBaseNode, synonym: String, sentenceType: Int): StringBuilder = {
    val nodeType: String = ToposoidUtils.getNodeType(sentenceType, LOCAL.index, PREDICATE_ARGUMENT.index)
    val synonymNodeType:String = ToposoidUtils.getNodeType(sentenceType, LOCAL.index, SYNONYM.index)
    val insertScript = new StringBuilder
    insertScript.append("|MERGE (:%s {nodeId:'%s', nodeName:'%s', propositionId:'%s', sentenceId:'%s'})\n".format(synonymNodeType, synonym + "_" + node.nodeId, synonym, node.propositionId, node.sentenceId))
    insertScript.append("|UNION ALL\n")
    insertScript.append("|MATCH (s:%s {nodeId: '%s'}), (d:%s {nodeId: '%s'}) MERGE (s)-[:SynonymEdge {similarity:0.5}]->(d)\n".format(synonymNodeType, synonym + "_" + node.nodeId, nodeType, node.nodeId))
    insertScript.append("|UNION ALL\n")
    insertScript
  }

  /**
   *
   * @param node
   * @param synonym
   * @param sentenceType
   * @return
   */
  private def createQueryForImageNode(node: KnowledgeBaseNode, sentenceType: Int, knowledgeForImage:KnowledgeForImage): StringBuilder = {
    val nodeType: String = ToposoidUtils.getNodeType(sentenceType, LOCAL.index, PREDICATE_ARGUMENT.index)
    val imageNodeType:String = ToposoidUtils.getNodeType(sentenceType, LOCAL.index, IMAGE.index)
    val insertScript = new StringBuilder
    insertScript.append("|MERGE (:%s {featureId:'%s', url:'%s', propositionId:'%s', sentenceId:'%s', source:'%s'})\n".format(imageNodeType, knowledgeForImage.id, knowledgeForImage.imageReference.reference.url, node.propositionId, node.sentenceId, knowledgeForImage.imageReference.reference.originalUrlOrReference))
    insertScript.append("|UNION ALL\n")
    insertScript.append("|MATCH (s:%s {featureId: '%s'}), (d:%s {nodeId: '%s'}) MERGE (s)-[:ImageEdge]->(d)\n".format(imageNodeType, knowledgeForImage.id, nodeType, node.nodeId))
    insertScript.append("|UNION ALL\n")
    insertScript
  }



  /**
   * This function outputs a query for edges for createGraph function.
   *
   * @param nodeMap
   * @param edge
   * @param sentenceType
   */
  private def createQueryForEdge(edge: KnowledgeBaseEdge, lang: String, sentenceType: Int): StringBuilder = {
    val insertScript = new StringBuilder
    val nodeType: String = sentenceType match {
      case PREMISE.index => "PremiseNode"
      case CLAIM.index => "ClaimNode"
    }
    /*
    val edgeType: String = sentenceType match {
      case PREMISE.index => "PremiseEdge"
      case CLAIM.index => "ClaimEdge"
    }
     */
    insertScript.append(("|MATCH (s:%s {nodeId: '%s'}), (d:%s {nodeId: '%s'}) MERGE (s)-[:LocalEdge {dependType:'%s', caseName:'%s', parallelType:'%s', hasInclusion:'%s', logicType:'%s'}]->(d) \n").format(
      nodeType,
      edge.sourceId,
      nodeType,
      edge.destinationId,
      edge.dependType,
      edge.caseStr,
      edge.parallelType,
      edge.hasInclusion,
      edge.logicType
    ))
    insertScript.append("|UNION ALL\n")
    insertScript
  }

  /**
   * This function outputs a query for logical edges.
   * This is only used by createGraph function
   *
   * @param propositionIds
   * @param propositionRelations
   * @param sentenceType
   */
  def executeForLogicRelation(sentenceIds: List[String], propositionRelations: List[PropositionRelation], sentenceType: Int): StringBuilder = {
    propositionRelations.foldLeft(new StringBuilder){
      (acc, x) => {
        acc.append(createLogicRelation(sentenceIds, x, sentenceType))
      }
    }

  }

  /**
   * This is sub-function of executeForLogicRelation
   *
   * @param propositionIds
   * @param propositionRelation
   * @param sentenceType
   */
  def createLogicRelation(sentenceIds: List[String], propositionRelation: PropositionRelation, sentenceType: Int): StringBuilder = {
    val insertScript = new StringBuilder
    val sourceNodeType: String = sentenceType match {
      case PREMISE.index => "PremiseNode"
      case CLAIM.index => "ClaimNode"
      case _ => "PremiseNode"
    }
    val destinationNodeType: String = sentenceType match {
      case PREMISE.index => "PremiseNode"
      case CLAIM.index => "ClaimNode"
      case _ => "ClaimNode"
    }

    insertScript.append(("|MATCH (s:%s), (d:%s) WHERE (s.nodeId =~'%s.*' AND  d.nodeId =~'%s.*') AND ((s.caseType = '文末'　AND　d.caseType = '文末') OR (s.caseType = 'ROOT'　AND　d.caseType = 'ROOT'))  MERGE (s)-[:LocalEdge {dependType:'-', caseName:'', parallelType:'-', hasInclusion:'false', logicType:'%s'}]->(d) \n").format(
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

