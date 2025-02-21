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

class Sentence2Neo4JTransformerImageEnglishTest extends AnyFlatSpec with BeforeAndAfter with BeforeAndAfterAll {

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
  
  "The list of local claim images" should "be properly registered in the knowledge database and searchable." in {
    val reference1 = Reference(url = "http://images.cocodataset.org/val2017/000000039769.jpg", surface = "cats", surfaceIndex = 3, isWholeSentence = false, originalUrlOrReference = "")
    val referenceImage1 = ImageReference(reference = reference1, x = 0, y = 0, width = 128, height = 128)
    val featureId1 = UUID.random.toString
    val knowledgeForImage1 = KnowledgeForImage(featureId1, referenceImage1)

    val reference2 = Reference(url = "http://images.cocodataset.org/train2017/000000428746.jpg", surface = "dog", surfaceIndex = 3, isWholeSentence = false, originalUrlOrReference = "")
    val referenceImage2 = ImageReference(reference = reference2, x = 0, y = 0, width = 128, height = 128)
    val featureId2 = UUID.random.toString
    val knowledgeForImage2 = KnowledgeForImage(featureId2, referenceImage2)


    val knowledgeList = List(
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("There are two cats.", "en_US", "{}", false, List(knowledgeForImage1))),
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("There is a dog.", "en_US" ,"{}", false, List(knowledgeForImage2)))
    )

    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(List.empty[KnowledgeForParser], List.empty[PropositionRelation], knowledgeList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSentenceSetForParser, transversalState),transversalState)

    val result: Neo4jRecords = TestUtils.executeQueryAndReturn("""MATCH x = (:ImageNode{url:'http://images.cocodataset.org/val2017/000000039769.jpg'})-[:ImageEdge]->(:ClaimNode{surface:'cats'}) RETURN x """, transversalState)
     assert(result.records.size == 1)

    val result2:  Neo4jRecords =  TestUtils.executeQueryAndReturn("""MATCH x = (:ImageNode{url:'http://images.cocodataset.org/train2017/000000428746.jpg'})-[:ImageEdge]->(:ClaimNode{surface:'dog'}) RETURN x """, transversalState)
    assert(result2 .records.size == 1)
  }

  "The list of local premise and claim images" should "be properly registered in the knowledge database and searchable." in {
    val reference1 = Reference(url = "http://images.cocodataset.org/val2017/000000039769.jpg", surface = "cats", surfaceIndex = 3, isWholeSentence = false, originalUrlOrReference = "")
    val referenceImage1 = ImageReference(reference = reference1, x = 0, y = 0, width = 128, height = 128)
    val featureId1 = UUID.random.toString
    val knowledgeForImage1 = KnowledgeForImage(featureId1, referenceImage1)

    val reference2 = Reference(url = "http://images.cocodataset.org/train2017/000000428746.jpg", surface = "dog", surfaceIndex = 3, isWholeSentence = false, originalUrlOrReference = "")
    val referenceImage2 = ImageReference(reference = reference2, x = 0, y = 0, width = 128, height = 128)
    val featureId2 = UUID.random.toString
    val knowledgeForImage2 = KnowledgeForImage(featureId2, referenceImage2)


    val knowledgePremiseList = List(
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("There are two cats.", "en_US", "{}", false, List(knowledgeForImage1))),
    )

    val knowledgeClaimList = List(
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("There is a dog.", "en_US", "{}", false, List(knowledgeForImage2)))
    )
    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(knowledgePremiseList, List.empty[PropositionRelation], knowledgeClaimList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSentenceSetForParser, transversalState),transversalState)

    val result:  Neo4jRecords =  TestUtils.executeQueryAndReturn("""MATCH x = (:ImageNode{url:'http://images.cocodataset.org/val2017/000000039769.jpg'})-[:ImageEdge]->(:PremiseNode{surface:'cats'}) RETURN x """, transversalState)
     assert(result.records.size == 1)

    val result2:  Neo4jRecords =  TestUtils.executeQueryAndReturn("""MATCH x = (:ImageNode{url:'http://images.cocodataset.org/train2017/000000428746.jpg'})-[:ImageEdge]->(:ClaimNode{surface:'dog'}) RETURN x """, transversalState)
    assert(result2 .records.size == 1)

  }

  "The list of semi-global claim images" should "be properly registered in the knowledge database and searchable." in {

    val reference1 = Reference(url = "http://images.cocodataset.org/val2017/000000039769.jpg", surface = "", surfaceIndex = 0, isWholeSentence = true, originalUrlOrReference = "")
    val referenceImage1 = ImageReference(reference = reference1, x = 0, y = 0, width = 128, height = 128)
    val featureId1 = UUID.random.toString
    val knowledgeForImage1 = KnowledgeForImage(featureId1, referenceImage1)

    val reference2 = Reference(url = "http://images.cocodataset.org/train2017/000000428746.jpg", surface = "", surfaceIndex = 0, isWholeSentence = true, originalUrlOrReference = "")
    val referenceImage2 = ImageReference(reference = reference2, x = 0, y = 0, width = 128, height = 128)
    val featureId2 = UUID.random.toString
    val knowledgeForImage2 = KnowledgeForImage(featureId2, referenceImage2)


    val knowledgeList = List(
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("There are two cats.", "en_US", "{}", false, List(knowledgeForImage1))),
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("There is a dog.", "en_US", "{}", false, List(knowledgeForImage2)))
    )

    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(List.empty[KnowledgeForParser], List.empty[PropositionRelation], knowledgeList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSentenceSetForParser, transversalState), transversalState)

    val result:  Neo4jRecords =  TestUtils.executeQueryAndReturn("""MATCH x = (:ImageNode{url:'http://images.cocodataset.org/val2017/000000039769.jpg'})-[:ImageEdge]->(:SemiGlobalClaimNode{sentence:'There are two cats.'}) RETURN x """, transversalState)
     assert(result.records.size == 1)

    val result2:  Neo4jRecords =  TestUtils.executeQueryAndReturn("""MATCH x = (:ImageNode{url:'http://images.cocodataset.org/train2017/000000428746.jpg'})-[:ImageEdge]->(:SemiGlobalClaimNode{sentence:'There is a dog.'}) RETURN x """, transversalState)
    assert(result2 .records.size == 1)

  }

  "The list of semi-global premise and claim images" should "be properly registered in the knowledge database and searchable." in {

    val reference1 = Reference(url = "http://images.cocodataset.org/val2017/000000039769.jpg", surface = "", surfaceIndex = 0, isWholeSentence = true, originalUrlOrReference = "")
    val referenceImage1 = ImageReference(reference = reference1, x = 0, y = 0, width = 128, height = 128)
    val featureId1 = UUID.random.toString
    val knowledgeForImage1 = KnowledgeForImage(featureId1, referenceImage1)

    val reference2 = Reference(url = "http://images.cocodataset.org/train2017/000000428746.jpg", surface = "", surfaceIndex = 0, isWholeSentence = true, originalUrlOrReference = "")
    val referenceImage2 = ImageReference(reference = reference2, x = 0, y = 0, width = 128, height = 128)
    val featureId2 = UUID.random.toString
    val knowledgeForImage2 = KnowledgeForImage(featureId2, referenceImage2)


    val knowledgePremiseList = List(
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("There are two cats.", "en_US", "{}", false, List(knowledgeForImage1))),
    )

    val knowledgeClaimList = List(
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("There is a dog.", "en_US", "{}", false, List(knowledgeForImage2)))
    )
    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(knowledgePremiseList, List.empty[PropositionRelation], knowledgeClaimList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSentenceSetForParser, transversalState), transversalState)


    val result:  Neo4jRecords =  TestUtils.executeQueryAndReturn("""MATCH x = (:ImageNode{url:'http://images.cocodataset.org/val2017/000000039769.jpg'})-[:ImageEdge]->(:SemiGlobalPremiseNode{sentence:'There are two cats.'}) RETURN x """, transversalState)
     assert(result.records.size == 1)

    val result2:  Neo4jRecords =  TestUtils.executeQueryAndReturn("""MATCH x = (:ImageNode{url:'http://images.cocodataset.org/train2017/000000428746.jpg'})-[:ImageEdge]->(:SemiGlobalClaimNode{sentence:'There is a dog.'}) RETURN x """, transversalState)
    assert(result2 .records.size == 1)

  }

}
