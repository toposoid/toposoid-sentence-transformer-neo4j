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
import com.ideal.linked.toposoid.common.{CLAIM, PREMISE}
import com.ideal.linked.toposoid.knowledgebase.regist.model.PropositionRelation
import com.ideal.linked.toposoid.protocol.model.parser.KnowledgeSentenceSetForParser
import com.ideal.linked.toposoid.sentence.transformer.neo4j.QueryManagementForLocalNode.{createLogicRelation, execute, executeForLogicRelation}
import com.ideal.linked.toposoid.sentence.transformer.neo4j.QueryManagementForSemiGlobalNode.{createSemiGlobalLogicRelation, executeForSemiGlobalLogicRelation, executeForSemiGlobalNode}
import com.typesafe.scalalogging.LazyLogging

/**
 * The main implementation of this module is the conversion of predicate-argument-analyzed sentence structures into a knowledge graph.
 * Use Neo4J as the knowledge database.
 */
object Sentence2Neo4jTransformer extends LazyLogging{

  val re = "UNION ALL\n$".r

  /**
   * This function explicitly separates the proposition into Premise and Claim, specifies the structure, and registers the data in GraphDB.
   * @param propositionId Sentences in knowledgeSentenceSet have the same propositionId
   * @param knowledgeSentenceSet
   */
  def createGraph(knowledgeSentenceSetForParser:KnowledgeSentenceSetForParser): Unit ={

    val insertScript = new StringBuilder
    knowledgeSentenceSetForParser.premiseList.map(execute(_, PREMISE.index))
    knowledgeSentenceSetForParser.claimList.map(execute(_, CLAIM.index))
    knowledgeSentenceSetForParser.premiseList.map(executeForSemiGlobalNode(_, PREMISE.index))
    knowledgeSentenceSetForParser.claimList.map(executeForSemiGlobalNode(_, CLAIM.index))

    //Get a list of sentenceIds for Premise and Claim respectively
    val premiseSentenceIds = knowledgeSentenceSetForParser.premiseList.map(_.sentenceId)
    val claimSentenceIds = knowledgeSentenceSetForParser.claimList.map(_.sentenceId)

    insertScript.clear()
    //If the target proposition has multiple Premises, create an Edge on them according to knowledgeSentenceSet.premiseLogicRelation
    //if(premisePropositionIds.size > 1) executeForLogicRelation(premisePropositionIds, knowledgeSentenceSetForParser.premiseLogicRelation, PREMISE.index)
    if(premiseSentenceIds.size > 1) {
      insertScript.append(executeForLogicRelation(premiseSentenceIds, knowledgeSentenceSetForParser.premiseLogicRelation, PREMISE.index))
      insertScript.append(executeForSemiGlobalLogicRelation(premiseSentenceIds, knowledgeSentenceSetForParser.premiseLogicRelation, PREMISE.index))
    }

    //If the target proposition has multiple Claims, create an Edge on them according to knowledgeSentenceSet.premiseLogicRelation
    if(claimSentenceIds.size > 1) {
      insertScript.append(executeForLogicRelation(claimSentenceIds, knowledgeSentenceSetForParser.claimLogicRelation, CLAIM.index))
      insertScript.append(executeForSemiGlobalLogicRelation(claimSentenceIds, knowledgeSentenceSetForParser.claimLogicRelation, CLAIM.index))
    }

    //If the target proposition has both Premise and CLaim,
    // select one representative for Premise and one representative for Claim and connect them to Edge.
    // The representative is the node with the 0th INDEX.
    if(premiseSentenceIds.size > 0 && claimSentenceIds.size > 0) {
      val propositionRelation = PropositionRelation("IMP", 0, 1)
      insertScript.append(createLogicRelation(List(premiseSentenceIds(0), claimSentenceIds(0)), propositionRelation, -1))
      insertScript.append(createSemiGlobalLogicRelation(List(premiseSentenceIds(0), claimSentenceIds(0)), propositionRelation, -1))
    }
    if(insertScript.size != 0) Neo4JAccessor.executeQuery(re.replaceAllIn(insertScript.toString().stripMargin, ""))

    //CREATE INDEX
    Neo4JAccessor.executeQuery("CREATE CONSTRAINT premiseNodeIdIndex IF NOT EXISTS ON(n:PremiseNode) ASSERT n.nodeId IS UNIQUE")
    Neo4JAccessor.executeQuery("CREATE CONSTRAINT claimNodeIdIndex IF NOT EXISTS ON(n:ClaimNode) ASSERT n.nodeId IS UNIQUE")
    Neo4JAccessor.executeQuery("CREATE CONSTRAINT synonymNodeIdIndex IF NOT EXISTS ON(n:SynonymNode) ASSERT n.nodeId IS UNIQUE")
    Neo4JAccessor.executeQuery("CREATE CONSTRAINT imageNodeIdIndex IF NOT EXISTS ON(n:ImageNode) ASSERT n.featureId IS UNIQUE")

    Neo4JAccessor.executeQuery("CREATE INDEX premisePropositionIdIndex IF NOT EXISTS FOR (n:PremiseNode) ON (n.propositionId)")
    Neo4JAccessor.executeQuery("CREATE INDEX claimPropositionIdIndex IF NOT EXISTS FOR (n:ClaimNode) ON (n.propositionId)")
    Neo4JAccessor.executeQuery("CREATE INDEX synonymPropositionIdIndex IF NOT EXISTS FOR (n:SynonymNode) ON (n.propositionId)")
    Neo4JAccessor.executeQuery("CREATE INDEX imagePropositionIdIndex IF NOT EXISTS FOR (n:ImageNode) ON (n.propositionId)")

    Neo4JAccessor.executeQuery("CREATE INDEX premiseSentenceIdIndex IF NOT EXISTS FOR (n:PremiseNode) ON (n.sentenceId)")
    Neo4JAccessor.executeQuery("CREATE INDEX claimSentenceIdIndex IF NOT EXISTS FOR (n:ClaimNode) ON (n.sentenceId)")
    Neo4JAccessor.executeQuery("CREATE INDEX synonymSentenceIdIndex IF NOT EXISTS FOR (n:SynonymNode) ON (n.sentenceId)")
    Neo4JAccessor.executeQuery("CREATE INDEX imageSentenceIdIndex IF NOT EXISTS FOR (n:ImageNode) ON (n.sentenceId)")

    Neo4JAccessor.executeQuery("CREATE INDEX premiseSurfaceIndex IF NOT EXISTS FOR (n:PremiseNode) ON (n.surface)")
    Neo4JAccessor.executeQuery("CREATE INDEX claimSurfaceIndex IF NOT EXISTS FOR (n:ClaimNode) ON (n.surface)")
    Neo4JAccessor.executeQuery("CREATE INDEX premiseRelationshipCaseNameIndex IF NOT EXISTS FOR (r:PremiseEdge) ON (r.caseName)")
    Neo4JAccessor.executeQuery("CREATE INDEX claimRelationshipCaseNameIndex IF NOT EXISTS FOR (r:ClaimEdge) ON (r.caseName)")

  }
}

