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
import com.ideal.linked.toposoid.knowledgebase.regist.model.{ImageReference, Knowledge, KnowledgeForImage, PropositionRelation, Reference}
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatest.flatspec.AnyFlatSpec
import io.jvm.uuid.UUID
import org.neo4j.driver.Result

class Sentence2Neo4JTransformerImageJapaneseTest extends AnyFlatSpec with BeforeAndAfter with BeforeAndAfterAll {
  before {
    Neo4JAccessor.delete()
  }

  override def beforeAll(): Unit = {
    Neo4JAccessor.delete()
  }

  override def afterAll(): Unit = {
    Neo4JAccessor.delete()
  }

  "The list of local claim images" should "be properly registered in the knowledge database and searchable." in {
    val reference1 = Reference(url = "http://images.cocodataset.org/val2017/000000039769.jpg", surface = "猫が", surfaceIndex = 0, isWholeSentence = false, originalUrlOrReference = "")
    val referenceImage1 = ImageReference(reference = reference1, x = 0, y = 0, width = 128, height = 128)
    val featureId1 = UUID.random.toString
    val knowledgeForImage1 = KnowledgeForImage(featureId1, referenceImage1)

    val reference2 = Reference(url = "http://images.cocodataset.org/train2017/000000428746.jpg", surface = "犬が", surfaceIndex = 0, isWholeSentence = false, originalUrlOrReference = "")
    val referenceImage2 = ImageReference(reference = reference2, x = 0, y = 0, width = 128, height = 128)
    val featureId2 = UUID.random.toString
    val knowledgeForImage2 = KnowledgeForImage(featureId2, referenceImage2)


    val knowledgeList = List(
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("猫が２匹います。", "ja_JP", "{}", false, List(knowledgeForImage1))),
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("犬が一匹います。", "ja_JP" ,"{}", false, List(knowledgeForImage2)))
    )

    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(List.empty[KnowledgeForParser], List.empty[PropositionRelation], knowledgeList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSetForParser)

    val result:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ImageNode{url:'http://images.cocodataset.org/val2017/000000039769.jpg'})-[:ImageEdge]->(:ClaimNode{surface:'猫が'}) RETURN x")
    assert(result.hasNext)

    val result2: Result = Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ImageNode{url:'http://images.cocodataset.org/train2017/000000428746.jpg'})-[:ImageEdge]->(:ClaimNode{surface:'犬が'}) RETURN x")
    assert(result2.hasNext)
  }

  "The list of local premise and claim images" should "be properly registered in the knowledge database and searchable." in {
    val reference1 = Reference(url = "http://images.cocodataset.org/val2017/000000039769.jpg", surface = "猫が", surfaceIndex = 0, isWholeSentence = false, originalUrlOrReference = "")
    val referenceImage1 = ImageReference(reference = reference1, x = 0, y = 0, width = 128, height = 128)
    val featureId1 = UUID.random.toString
    val knowledgeForImage1 = KnowledgeForImage(featureId1, referenceImage1)

    val reference2 = Reference(url = "http://images.cocodataset.org/train2017/000000428746.jpg", surface = "犬が", surfaceIndex = 0, isWholeSentence = false, originalUrlOrReference = "")
    val referenceImage2 = ImageReference(reference = reference2, x = 0, y = 0, width = 128, height = 128)
    val featureId2 = UUID.random.toString
    val knowledgeForImage2 = KnowledgeForImage(featureId2, referenceImage2)


    val knowledgePremiseList = List(
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("猫が２匹います。", "ja_JP", "{}", false, List(knowledgeForImage1))),
    )

    val knowledgeClaimList = List(
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("犬が一匹います。", "ja_JP", "{}", false, List(knowledgeForImage2)))
    )
    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(knowledgePremiseList, List.empty[PropositionRelation], knowledgeClaimList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSetForParser)

    val result: Result = Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ImageNode{url:'http://images.cocodataset.org/val2017/000000039769.jpg'})-[:ImageEdge]->(:PremiseNode{surface:'猫が'}) RETURN x")
    assert(result.hasNext)

    val result2: Result = Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ImageNode{url:'http://images.cocodataset.org/train2017/000000428746.jpg'})-[:ImageEdge]->(:ClaimNode{surface:'犬が'}) RETURN x")
    assert(result2.hasNext)

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
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("猫が２匹います。", "ja_JP", "{}", false, List(knowledgeForImage1))),
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("犬が一匹います。", "ja_JP", "{}", false, List(knowledgeForImage2)))
    )

    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(List.empty[KnowledgeForParser], List.empty[PropositionRelation], knowledgeList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSetForParser)

    val result: Result = Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ImageNode{url:'http://images.cocodataset.org/val2017/000000039769.jpg'})-[:ImageEdge]->(:SemiGlobalClaimNode{sentence:'猫が２匹います。'}) RETURN x")
    assert(result.hasNext)

    val result2: Result = Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ImageNode{url:'http://images.cocodataset.org/train2017/000000428746.jpg'})-[:ImageEdge]->(:SemiGlobalClaimNode{sentence:'犬が一匹います。'}) RETURN x")
    assert(result2.hasNext)

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
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("猫が２匹います。", "ja_JP", "{}", false, List(knowledgeForImage1))),
    )

    val knowledgeClaimList = List(
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("犬が一匹います。", "ja_JP", "{}", false, List(knowledgeForImage2)))
    )
    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(knowledgePremiseList, List.empty[PropositionRelation], knowledgeClaimList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSetForParser)


    val result: Result = Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ImageNode{url:'http://images.cocodataset.org/val2017/000000039769.jpg'})-[:ImageEdge]->(:SemiGlobalPremiseNode{sentence:'猫が２匹います。'}) RETURN x")
    assert(result.hasNext)

    val result2: Result = Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ImageNode{url:'http://images.cocodataset.org/train2017/000000428746.jpg'})-[:ImageEdge]->(:SemiGlobalClaimNode{sentence:'犬が一匹います。'}) RETURN x")
    assert(result2.hasNext)

  }
}
