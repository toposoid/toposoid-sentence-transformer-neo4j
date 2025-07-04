/*
 * Copyright (C) 2025  Linked Ideal LLC.[https://linked-ideal.com/]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
