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

import com.ideal.linked.toposoid.common.{Neo4JUtils, TransversalState}
import com.typesafe.scalalogging.LazyLogging

object QueryManagementForIndex extends LazyLogging{
  
  def createIndex(neo4JUtils:Neo4JUtils, transversalState: TransversalState): Unit = {
    //CREATE INDEX
    neo4JUtils.executeQuery("CREATE CONSTRAINT premiseNodeIdIndex IF NOT EXISTS ON(n:PremiseNode) ASSERT n.nodeId IS UNIQUE", transversalState)
    neo4JUtils.executeQuery("CREATE CONSTRAINT claimNodeIdIndex IF NOT EXISTS ON(n:ClaimNode) ASSERT n.nodeId IS UNIQUE", transversalState)
    neo4JUtils.executeQuery("CREATE CONSTRAINT synonymNodeIdIndex IF NOT EXISTS ON(n:SynonymNode) ASSERT n.nodeId IS UNIQUE", transversalState)
    neo4JUtils.executeQuery("CREATE CONSTRAINT imageNodeIdIndex IF NOT EXISTS ON(n:ImageNode) ASSERT n.featureId IS UNIQUE", transversalState)

    neo4JUtils.executeQuery("CREATE INDEX premisePropositionIdIndex IF NOT EXISTS FOR (n:PremiseNode) ON (n.propositionId)", transversalState)
    neo4JUtils.executeQuery("CREATE INDEX claimPropositionIdIndex IF NOT EXISTS FOR (n:ClaimNode) ON (n.propositionId)", transversalState)
    neo4JUtils.executeQuery("CREATE INDEX synonymPropositionIdIndex IF NOT EXISTS FOR (n:SynonymNode) ON (n.propositionId)", transversalState)
    neo4JUtils.executeQuery("CREATE INDEX imagePropositionIdIndex IF NOT EXISTS FOR (n:ImageNode) ON (n.propositionId)", transversalState)

    neo4JUtils.executeQuery("CREATE INDEX premiseSentenceIdIndex IF NOT EXISTS FOR (n:PremiseNode) ON (n.sentenceId)", transversalState)
    neo4JUtils.executeQuery("CREATE INDEX claimSentenceIdIndex IF NOT EXISTS FOR (n:ClaimNode) ON (n.sentenceId)", transversalState)
    neo4JUtils.executeQuery("CREATE INDEX synonymSentenceIdIndex IF NOT EXISTS FOR (n:SynonymNode) ON (n.sentenceId)", transversalState)
    neo4JUtils.executeQuery("CREATE INDEX imageSentenceIdIndex IF NOT EXISTS FOR (n:ImageNode) ON (n.sentenceId)", transversalState)

    neo4JUtils.executeQuery("CREATE INDEX premiseSurfaceIndex IF NOT EXISTS FOR (n:PremiseNode) ON (n.surface)", transversalState)
    neo4JUtils.executeQuery("CREATE INDEX claimSurfaceIndex IF NOT EXISTS FOR (n:ClaimNode) ON (n.surface)", transversalState)
    neo4JUtils.executeQuery("CREATE INDEX premiseRelationshipCaseNameIndex IF NOT EXISTS FOR (r:PremiseEdge) ON (r.caseName)", transversalState)
    neo4JUtils.executeQuery("CREATE INDEX claimRelationshipCaseNameIndex IF NOT EXISTS FOR (r:ClaimEdge) ON (r.caseName)", transversalState)
  }
  
}
