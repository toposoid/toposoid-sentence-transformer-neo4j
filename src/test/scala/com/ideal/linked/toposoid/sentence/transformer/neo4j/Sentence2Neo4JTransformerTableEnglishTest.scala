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

import com.ideal.linked.toposoid.common.TransversalState
import com.ideal.linked.toposoid.knowledgebase.regist.model._
import com.ideal.linked.toposoid.protocol.model.neo4j.Neo4jRecords
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import com.ideal.linked.toposoid.sentence.transformer.neo4j.TestUtils.getAnalyzedPropositionSet
import io.jvm.uuid.UUID
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}

class Sentence2Neo4JTransformerTableEnglishTest extends AnyFlatSpec with BeforeAndAfter with BeforeAndAfterAll {

  val transversalState:TransversalState = TransversalState(userId="test-user", username="guest", roleId=0, csrfToken = "")

  before {
    TestUtils.deleteNeo4JAllData(transversalState)
  }

  override def beforeAll(): Unit = {
    TestUtils.deleteNeo4JAllData(transversalState)
  }

  override def afterAll(): Unit = {
    TestUtils.deleteNeo4JAllData(transversalState)
  }
  
  "The list of local claim tables" should "be properly registered in the knowledge database and searchable." in {
    val reference1 = Reference(url = "http://xxx/yyy.tsv", surface = "Figure1", surfaceIndex = 3, isWholeSentence = false, originalUrlOrReference = "")
    val referenceTable1 = TableReference(reference = reference1)
    val featureId1 = UUID.random.toString
    val knowledgeForTable1 = KnowledgeForTable(featureId1, referenceTable1)

    val reference2 = Reference(url = "http://xxx/zzz.tsv", surface = "Figure2", surfaceIndex = 3, isWholeSentence = false, originalUrlOrReference = "")
    val referenceTable2 = TableReference(reference = reference2)
    val featureId2 = UUID.random.toString
    val knowledgeForTable2 = KnowledgeForTable(featureId2, referenceTable2)


    val knowledgeList = List(
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("This is a Figure1.", "en_US", "{}", false, List.empty[KnowledgeForImage], List(knowledgeForTable1))),
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("This is a Figure2.", "en_US" ,"{}", false, List.empty[KnowledgeForImage], List(knowledgeForTable2)))
    )

    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(List.empty[KnowledgeForParser], List.empty[PropositionRelation], knowledgeList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSentenceSetForParser, transversalState),transversalState)

    val result: Neo4jRecords = TestUtils.executeQueryAndReturn("""MATCH x = (:TableNode{url:'http://xxx/yyy.tsv'})-[:TableEdge]->(:ClaimNode{surface:'Figure1'}) RETURN x """, transversalState)
     assert(result.records.size == 1)

    val result2:  Neo4jRecords =  TestUtils.executeQueryAndReturn("""MATCH x = (:TableNode{url:'http://xxx/zzz.tsv'})-[:TableEdge]->(:ClaimNode{surface:'Figure2'}) RETURN x """, transversalState)
    assert(result2 .records.size == 1)
  }

  "The list of local premise and claim images" should "be properly registered in the knowledge database and searchable." in {
    val reference1 = Reference(url = "http://xxx/yyy.tsv", surface = "Figure1", surfaceIndex = 3, isWholeSentence = false, originalUrlOrReference = "")
    val referenceTable1 = TableReference(reference = reference1)
    val featureId1 = UUID.random.toString
    val knowledgeForTable1 = KnowledgeForTable(featureId1, referenceTable1)

    val reference2 = Reference(url = "http://xxx/zzz.tsv", surface = "Figure2", surfaceIndex = 3, isWholeSentence = false, originalUrlOrReference = "")
    val referenceTable2 = TableReference(reference = reference2)
    val featureId2 = UUID.random.toString
    val knowledgeForTable2 = KnowledgeForTable(featureId2, referenceTable2)


    val knowledgePremiseList = List(
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("This is a Figure1.", "en_US", "{}", false, List.empty[KnowledgeForImage], List(knowledgeForTable1))),
    )

    val knowledgeClaimList = List(
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("This is a Figure2.", "en_US" ,"{}", false, List.empty[KnowledgeForImage], List(knowledgeForTable2)))
    )

    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(knowledgePremiseList, List.empty[PropositionRelation], knowledgeClaimList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSentenceSetForParser, transversalState),transversalState)

    val result:  Neo4jRecords =  TestUtils.executeQueryAndReturn("""MATCH x = (:TableNode{url:'http://xxx/yyy.tsv'})-[:TableEdge]->(:PremiseNode{surface:'Figure1'}) RETURN x """, transversalState)
     assert(result.records.size == 1)

    val result2:  Neo4jRecords =  TestUtils.executeQueryAndReturn("""MATCH x = (:TableNode{url:'http://xxx/zzz.tsv'})-[:TableEdge]->(:ClaimNode{surface:'Figure2'}) RETURN x """, transversalState)
    assert(result2 .records.size == 1)

  }

  "The list of semi-global claim images" should "be properly registered in the knowledge database and searchable." in {

    val reference1 = Reference(url = "http://xxx/yyy.tsv", surface = "Figure1", surfaceIndex = 3, isWholeSentence = true, originalUrlOrReference = "")
    val referenceTable1 = TableReference(reference = reference1)
    val featureId1 = UUID.random.toString
    val knowledgeForTable1 = KnowledgeForTable(featureId1, referenceTable1)

    val reference2 = Reference(url = "http://xxx/zzz.tsv", surface = "Figure2", surfaceIndex = 3, isWholeSentence = true, originalUrlOrReference = "")
    val referenceTable2 = TableReference(reference = reference2)
    val featureId2 = UUID.random.toString
    val knowledgeForTable2 = KnowledgeForTable(featureId2, referenceTable2)


    val knowledgeList = List(
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("This is a Figure1.", "en_US", "{}", false, List.empty[KnowledgeForImage], List(knowledgeForTable1))),
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("This is a Figure2.", "en_US", "{}", false, List.empty[KnowledgeForImage], List(knowledgeForTable2)))
    )

    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(List.empty[KnowledgeForParser], List.empty[PropositionRelation], knowledgeList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSentenceSetForParser, transversalState), transversalState)

    val result:  Neo4jRecords =  TestUtils.executeQueryAndReturn("""MATCH x = (:TableNode{url:'http://xxx/yyy.tsv'})-[:TableEdge]->(:SemiGlobalClaimNode{sentence:'This is a Figure1.'}) RETURN x """, transversalState)
     assert(result.records.size == 1)

    val result2:  Neo4jRecords =  TestUtils.executeQueryAndReturn("""MATCH x = (:TableNode{url:'http://xxx/zzz.tsv'})-[:TableEdge]->(:SemiGlobalClaimNode{sentence:'This is a Figure2.'}) RETURN x """, transversalState)
    assert(result2 .records.size == 1)

  }

  "The list of semi-global premise and claim images" should "be properly registered in the knowledge database and searchable." in {

    val reference1 = Reference(url = "http://xxx/yyy.tsv", surface = "Figure1", surfaceIndex = 3, isWholeSentence = true, originalUrlOrReference = "")
    val referenceTable1 = TableReference(reference = reference1)
    val featureId1 = UUID.random.toString
    val knowledgeForTable1 = KnowledgeForTable(featureId1, referenceTable1)

    val reference2 = Reference(url = "http://xxx/zzz.tsv", surface = "Figure2", surfaceIndex = 3, isWholeSentence = true, originalUrlOrReference = "")
    val referenceTable2 = TableReference(reference = reference2)
    val featureId2 = UUID.random.toString
    val knowledgeForTable2 = KnowledgeForTable(featureId2, referenceTable2)


    val knowledgePremiseList = List(
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("This is a Figure1.", "en_US", "{}", false, List.empty[KnowledgeForImage], List(knowledgeForTable1))),
    )

    val knowledgeClaimList = List(
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("This is a Figure2.", "en_US", "{}", false, List.empty[KnowledgeForImage], List(knowledgeForTable2)))
    )
    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(knowledgePremiseList, List.empty[PropositionRelation], knowledgeClaimList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSentenceSetForParser, transversalState), transversalState)


    val result:  Neo4jRecords =  TestUtils.executeQueryAndReturn("""MATCH x = (:TableNode{url:'http://xxx/yyy.tsv'})-[:TableEdge]->(:SemiGlobalPremiseNode{sentence:'This is a Figure1.'}) RETURN x """, transversalState)
     assert(result.records.size == 1)

    val result2:  Neo4jRecords =  TestUtils.executeQueryAndReturn("""MATCH x = (:TableNode{url:'http://xxx/zzz.tsv'})-[:TableEdge]->(:SemiGlobalClaimNode{sentence:'This is a Figure2.'}) RETURN x """, transversalState)
    assert(result2 .records.size == 1)

  }

}
