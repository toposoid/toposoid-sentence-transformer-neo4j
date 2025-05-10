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
import com.ideal.linked.toposoid.common.{CLAIM, Neo4JUtils, Neo4JUtilsImpl, PREMISE, ToposoidUtils, TransversalState}
import com.ideal.linked.toposoid.knowledgebase.regist.model.PropositionRelation
import com.ideal.linked.toposoid.protocol.model.base.AnalyzedSentenceObjects
import com.ideal.linked.toposoid.protocol.model.neo4j.Neo4jRecords
import com.ideal.linked.toposoid.protocol.model.parser.KnowledgeForParser
import com.ideal.linked.toposoid.sentence.transformer.neo4j.QueryManagementForIndex.createIndex
import com.ideal.linked.toposoid.sentence.transformer.neo4j.QueryManagementForLocalNode.{createLogicRelation, execute, executeForLogicRelation}
import com.ideal.linked.toposoid.sentence.transformer.neo4j.QueryManagementForSemiGlobalNode.{createSemiGlobalLogicRelation, executeForSemiGlobalLogicRelation, executeForSemiGlobalNode}
import com.ideal.linked.toposoid.sentence.transformer.neo4j.QueryManagementForGlobalNode.executeForGlobalNode
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.{Json, OWrites, Reads}

import scala.util.matching.Regex


/*
trait Neo4JUtils {
  def executeQuery(query: String, transversalState: TransversalState): Unit
  def executeQueryAndReturn(query: String, transversalState: TransversalState): Neo4jRecords
}
*/
case class AnalyzedPropositionPair(analyzedSentenceObjects: AnalyzedSentenceObjects ,knowledgeForParser: KnowledgeForParser)
object AnalyzedPropositionPair {
  implicit val jsonWrites: OWrites[AnalyzedPropositionPair] = Json.writes[AnalyzedPropositionPair]
  implicit val jsonReads: Reads[AnalyzedPropositionPair] = Json.reads[AnalyzedPropositionPair]
}


case class AnalyzedPropositionSet(premiseList:List[AnalyzedPropositionPair],premiseLogicRelation:List[PropositionRelation], claimList:List[AnalyzedPropositionPair], claimLogicRelation:List[PropositionRelation])
object AnalyzedPropositionSet {
  implicit val jsonWrites: OWrites[AnalyzedPropositionSet] = Json.writes[AnalyzedPropositionSet]
  implicit val jsonReads: Reads[AnalyzedPropositionSet] = Json.reads[AnalyzedPropositionSet]
}
/*
class Neo4JUtilsImpl extends Neo4JUtils {
  def executeQuery(query: String, transversalState: TransversalState): Unit = {
    val convertQuery = ToposoidUtils.encodeJsonInJson(query)
    val json = s"""{ "query":"$convertQuery", "target": "" }"""
    val res = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_GRAPHDB_WEB_HOST"), conf.getString("TOPOSOID_GRAPHDB_WEB_PORT"), "executeQuery", transversalState)
    if (!res.equals("""{"status":"OK","message":""}""")) {
      throw new Exception("Cypher query execution failed. " + res)
    }
  }
  def executeQueryAndReturn(query: String, transversalState: TransversalState): Neo4jRecords = {
    val convertQuery = ToposoidUtils.encodeJsonInJson(query)
    val json = s"""{ "query":"$convertQuery", "target": "" }"""
    val jsonResult = ToposoidUtils.callComponent(json, conf.getString("TOPOSOID_GRAPHDB_WEB_HOST"), conf.getString("TOPOSOID_GRAPHDB_WEB_PORT"), "getQueryFormattedResult", transversalState)
    Json.parse(jsonResult).as[Neo4jRecords]
  }
}
*/
/**
 * The main implementation of this module is the conversion of predicate-argument-analyzed sentence structures into a knowledge graph.
 * Use Neo4J as the knowledge database.
 */
object Sentence2Neo4jTransformer extends LazyLogging{

  val re = "UNION ALL\n$".r
  val neo4JUtils = new Neo4JUtilsImpl()

  /**
   * This function explicitly separates the proposition into Premise and Claim, specifies the structure, and registers the data in GraphDB.
   * @param propositionId Sentences in knowledgeSentenceSet have the same propositionId
   * @param knowledgeSentenceSet
   */
  def createGraph(analyzedPropositionSet:AnalyzedPropositionSet, transversalState: TransversalState, neo4JUtilsObject :Neo4JUtils=null): Unit = {

      val neo4JUtils = Option(neo4JUtilsObject) match {
        case Some(x) => x
        case None => this.neo4JUtils
      }

      val insertScript = new StringBuilder
      analyzedPropositionSet.premiseList.map(execute(_, PREMISE.index, neo4JUtils, transversalState))
      analyzedPropositionSet.claimList.map(execute(_, CLAIM.index, neo4JUtils, transversalState))
      analyzedPropositionSet.premiseList.map(executeForSemiGlobalNode(_, PREMISE.index, neo4JUtils, transversalState))
      analyzedPropositionSet.claimList.map(executeForSemiGlobalNode(_, CLAIM.index, neo4JUtils, transversalState))

      if(analyzedPropositionSet.claimList.size > 0) {
        val knowledgeForDocumentRep = analyzedPropositionSet.claimList.head.knowledgeForParser.knowledge.knowledgeForDocument
        executeForGlobalNode(knowledgeForDocumentRep, neo4JUtils, transversalState)
      }

      //Get a list of sentenceIds for Premise and Claim respectively
      //val premiseSentenceIds = analyzedPropositionSet.premiseList.map(_.knowledgeForParser.sentenceId)
      //val claimSentenceIds = analyzedPropositionSet.claimList.map(_.knowledgeForParser.sentenceId)

    val noReferenceRegex:Regex = "^(NO_REFERENCE)_.+_[0-9]+$".r
    val premiseSentences = analyzedPropositionSet.premiseList.filter(x => x.analyzedSentenceObjects.analyzedSentenceObjects.filter(y => y.nodeMap.filter(z => {
      val pas = z._2.predicateArgumentStructure
      !noReferenceRegex.matches(pas.surface) && (pas.caseType.equals("文末") || pas.caseType.equals("ROOT"))}).size > 0).size > 0)
    val claimSentences = analyzedPropositionSet.claimList.filter(x => x.analyzedSentenceObjects.analyzedSentenceObjects.filter(y => y.nodeMap.filter(z => {
      val pas = z._2.predicateArgumentStructure
      !noReferenceRegex.matches(pas.surface) && (pas.caseType.equals("文末") || pas.caseType.equals("ROOT"))}).size > 0).size > 0)
    val premiseSentenceIds = premiseSentences.map(_.knowledgeForParser.sentenceId)
    val claimSentenceIds = claimSentences.map(_.knowledgeForParser.sentenceId)

    insertScript.clear()
      //If the target proposition has multiple Premises, create an Edge on them according to knowledgeSentenceSet.premiseLogicRelation
      //if(premisePropositionIds.size > 1) executeForLogicRelation(premisePropositionIds, knowledgeSentenceSetForParser.premiseLogicRelation, PREMISE.index)
      if (premiseSentenceIds.size > 1) {
        insertScript.append(executeForLogicRelation(premiseSentenceIds, analyzedPropositionSet.premiseLogicRelation, PREMISE.index))
        insertScript.append(executeForSemiGlobalLogicRelation(premiseSentenceIds, analyzedPropositionSet.premiseLogicRelation, PREMISE.index))
      }

      //If the target proposition has multiple Claims, create an Edge on them according to knowledgeSentenceSet.premiseLogicRelation
      if (claimSentenceIds.size > 1) {
        insertScript.append(executeForLogicRelation(claimSentenceIds, analyzedPropositionSet.claimLogicRelation, CLAIM.index))
        insertScript.append(executeForSemiGlobalLogicRelation(claimSentenceIds, analyzedPropositionSet.claimLogicRelation, CLAIM.index))
      }

      //If the target proposition has both Premise and CLaim,
      // select one representative for Premise and one representative for Claim and connect them to Edge.
      // The representative is the node with the 0th INDEX.
      if (premiseSentenceIds.size > 0 && claimSentenceIds.size > 0) {
        val propositionRelation = PropositionRelation("IMP", 0, 1)
        insertScript.append(createLogicRelation(List(premiseSentenceIds.head, claimSentenceIds.head), propositionRelation, -1))
        insertScript.append(createSemiGlobalLogicRelation(List(premiseSentenceIds.head, claimSentenceIds.head), propositionRelation, -1))
      }
      if (insertScript.size != 0) neo4JUtils.executeQuery(re.replaceAllIn(insertScript.toString().stripMargin, ""), transversalState)

      createIndex(neo4JUtils, transversalState)
  }
}

