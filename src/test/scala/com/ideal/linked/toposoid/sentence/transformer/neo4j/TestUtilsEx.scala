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
import com.ideal.linked.toposoid.common.{Neo4JUtilsImpl, ToposoidUtils, TransversalState}
import com.ideal.linked.toposoid.protocol.model.base.AnalyzedSentenceObjects
import com.ideal.linked.toposoid.protocol.model.neo4j.Neo4jRecords
import com.ideal.linked.toposoid.protocol.model.parser.{InputSentenceForParser, KnowledgeForParser, KnowledgeSentenceSetForParser}
import play.api.libs.json.Json

import scala.util.matching.Regex


object TestUtilsEx {
  //val langPatternJP: Regex = "^ja_.*".r
  //val langPatternEN: Regex = "^en_.*".r
  val neo4JUtils = new Neo4JUtilsImpl()

  def deleteNeo4JAllData(transversalState:TransversalState): Unit = {
    val query = "MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE n,r"
    val neo4JUtils = new Neo4JUtilsImpl()
    neo4JUtils.executeQuery(query, transversalState)
  }

  def executeQueryAndReturn(query:String, transversalState:TransversalState): Neo4jRecords = {
    neo4JUtils.executeQueryAndReturn(query:String, transversalState:TransversalState)
  }


  private def parse(knowledgeForParser: KnowledgeForParser, transversalState:TransversalState): AnalyzedPropositionPair = {

    //Analyze everything as simple sentences as Claims, not just sentenceType
    val inputSentenceForParser = InputSentenceForParser(List.empty[KnowledgeForParser], List(knowledgeForParser))
    val json: String = Json.toJson(inputSentenceForParser).toString()
    val analyzedSentenceObjects: AnalyzedSentenceObjects = knowledgeForParser.knowledge.lang match {
      case ToposoidUtils.langPatternJP() => {
        val host = conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_HOST")
        val port = conf.getString("TOPOSOID_SENTENCE_PARSER_JP_WEB_PORT")
        val parseResult: String = ToposoidUtils.callComponent(json, host, port, "analyze", transversalState)
        Json.parse(parseResult).as[AnalyzedSentenceObjects]
      }
      case ToposoidUtils.langPatternEN() => {
        val host = conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_HOST")
        val port = conf.getString("TOPOSOID_SENTENCE_PARSER_EN_WEB_PORT")
        val parseResult: String = ToposoidUtils.callComponent(json, host, port, "analyze", transversalState)
        Json.parse(parseResult).as[AnalyzedSentenceObjects]
      }
      case ToposoidUtils.langPatternSpecialSymbol1() => {
        val aso = ToposoidUtils.parseSpecialSymbol(knowledgeForParser)
        AnalyzedSentenceObjects(List(aso))
      }
      case _ => throw new Exception("It is an invalid locale or an unsupported locale.")
    }
    AnalyzedPropositionPair(analyzedSentenceObjects = analyzedSentenceObjects, knowledgeForParser = knowledgeForParser)
  }

  def getAnalyzedPropositionSet(knowledgeSentenceSetForParser:KnowledgeSentenceSetForParser, transversalState:TransversalState):AnalyzedPropositionSet = {

    val premiseList = knowledgeSentenceSetForParser.premiseList.map(parse(_, transversalState))
    val claimList = knowledgeSentenceSetForParser.claimList.map(parse(_, transversalState))

    AnalyzedPropositionSet(
      premiseList = premiseList,
      premiseLogicRelation = knowledgeSentenceSetForParser.premiseLogicRelation,
      claimList = claimList,
      claimLogicRelation = knowledgeSentenceSetForParser.claimLogicRelation)
  }

}
