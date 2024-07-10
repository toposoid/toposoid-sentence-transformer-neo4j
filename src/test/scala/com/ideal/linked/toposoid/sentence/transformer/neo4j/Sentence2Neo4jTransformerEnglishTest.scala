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
import com.ideal.linked.toposoid.common.TransversalState
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, KnowledgeSentenceSet, PropositionRelation}
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import org.neo4j.driver.Result
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatest.flatspec.AnyFlatSpec
import io.jvm.uuid.UUID

class Sentence2Neo4jTransformerEnglishTest extends AnyFlatSpec with BeforeAndAfter with BeforeAndAfterAll {

  val transversalState:TransversalState = TransversalState(username="guest")

  before {
    Neo4JAccessor.delete()
  }

  override def beforeAll(): Unit = {
    Neo4JAccessor.delete()
  }

  override def afterAll(): Unit = {
    Neo4JAccessor.delete()
  }

  "The list of english sentences" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeList = List(KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("That's life.", "en_US", "{}", false)), KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("Seeing is believing.", "en_US" ,"{}", false)))
    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(List.empty[KnowledgeForParser], List.empty[PropositionRelation], knowledgeList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSetForParser, transversalState)
    val result:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ClaimNode{surface:'That'})-[:LocalEdge]->(:ClaimNode{surface:\"\'s\"})<-[:LocalEdge]-(:ClaimNode{surface:'life'}) RETURN x")
    assert(result.hasNext)
    val result2:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ClaimNode{surface:'Seeing'})-[:LocalEdge]->(:ClaimNode{surface:'believing'})<-[:LocalEdge]-(:ClaimNode{surface:'is'}) RETURN x")
    assert(result2.hasNext)
    val result3:Result = Neo4JAccessor.executeQueryAndReturn("MAtCH x = (:SynonymNode{nodeName:'living'})-[:SynonymEdge]->(:ClaimNode{surface:'life'}) return x")
    assert(result3.hasNext)
    val result4: Result = Neo4JAccessor.executeQueryAndReturn("MATCH x = (:SemiGlobalClaimNode{sentence:\"That\'s life.\"}) RETURN x")
    assert(result4.hasNext)
    val result5: Result = Neo4JAccessor.executeQueryAndReturn("MATCH x = (:SemiGlobalClaimNode{sentence:'Seeing is believing.'}) RETURN x")
    assert(result5.hasNext)
    val result6: Result = Neo4JAccessor.executeQueryAndReturn("MATCH x = (:SemiGlobalClaimNode{sentence:\"That\'s life.\"})-[*]-(:SemiGlobalClaimNode{sentence:'Seeing is believing.'}) RETURN x")
    assert(!result6.hasNext)

  }

  "The list of multiple english sentences" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeList = List(KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("That's life. Seeing is believing.", "en_US", "{}", false)))
    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(List.empty[KnowledgeForParser], List.empty[PropositionRelation], knowledgeList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSetForParser, transversalState)
    val result:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ClaimNode{surface:'That'})-[:LocalEdge]->(:ClaimNode{surface:\"\'s\"})<-[:LocalEdge]-(:ClaimNode{surface:'life'}) RETURN x")
    assert(result.hasNext)
    val result2:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ClaimNode{surface:'Seeing'})-[:LocalEdge]->(:ClaimNode{surface:'believing'})<-[:LocalEdge]-(:ClaimNode{surface:'is'}) RETURN x")
    assert(result2.hasNext)
    val result3: Result = Neo4JAccessor.executeQueryAndReturn("MATCH x = (:SemiGlobalClaimNode{sentence:\"That\'s life. Seeing is believing.\"}) RETURN x")
    assert(result3.hasNext)
  }

  "The List of english sentences including a premise" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeList = List(KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("If you can dream it, you can do it.", "en_US", "{}", false)))
    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(List.empty[KnowledgeForParser], List.empty[PropositionRelation], knowledgeList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSetForParser, transversalState)
    val result:Result = Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ClaimNode)-[*..]->(:ClaimNode{surface:'dream'})-[:LocalEdge]->(:ClaimNode{surface:'do'})<-[*..]-(:ClaimNode) RETURN x")
    assert(result.hasNext)
    val result2: Result = Neo4JAccessor.executeQueryAndReturn("MATCH x = (:SemiGlobalClaimNode{sentence:'If you can dream it, you can do it.'}) RETURN x")
    assert(result2.hasNext)

  }

  "The list of english sentences with json" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeList = List(KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("That's life.", "en_US", """{"id":"Test"}""", false)), KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("Seeing is believing.", "en_US", """{"dummy":"!\"#$%&\'()"}""", false)))
    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(List.empty[KnowledgeForParser], List.empty[PropositionRelation], knowledgeList, List(PropositionRelation("AND", 0,1)))
    Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSetForParser, transversalState)
    //val result:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (n:ClaimNode) WHERE n.extentText='{\"id\":\"Test\"}' return x")
    //assert(result.hasNext)
    //val result2:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (n:ClaimNode) WHERE n.extentText=~'.*\"dummy\".*' return x")
    //assert(result2.hasNext)
    val result3: Result = Neo4JAccessor.executeQueryAndReturn("MATCH x = (:SemiGlobalClaimNode{sentence:\"That\'s life.\"})-[:SemiGlobalEdge{logicType:'AND'}]-(:SemiGlobalClaimNode{sentence:'Seeing is believing.'}) RETURN x")
    assert(result3.hasNext)

  }

  "The short english sentence with json" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeList = List(KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("nature", "en_US", """{"id":"Test"}""", false)), KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("nature", "en_US","""{"id":"Test2"}""", false)), KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("", "en_US","""{"id":"Test3"}""", false)))
    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(List.empty[KnowledgeForParser], List.empty[PropositionRelation], knowledgeList, List(PropositionRelation("AND", 0,1)))
    Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSetForParser, transversalState)
    //val result:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (n:ClaimNode) WHERE n.extentText='{\"id\":\"Test\"}' return x")
    //assert(result.hasNext)
    //val result2:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (n:ClaimNode) WHERE n.extentText='{\"id\":\"Test2\"}' return x")
    //assert(result2.hasNext)
    //val result3:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (n:ClaimNode) WHERE n.extentText='{\"id\":\"Test3\"}' return x")
    //assert(!result3.hasNext)
    val result4: Result = Neo4JAccessor.executeQueryAndReturn("MATCH x = (:SemiGlobalClaimNode{sentence:'nature'}) RETURN x")
    assert(result4.hasNext)
    val result5: Result = Neo4JAccessor.executeQueryAndReturn("MATCH x = (:SemiGlobalClaimNode{sentence:'nature'}) RETURN x")
    assert(result5.hasNext)

  }

  "The Empty knowledge" should "not fail" in {
    val knowledgeSetForParser: KnowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(List.empty[KnowledgeForParser], List.empty[PropositionRelation], List.empty[KnowledgeForParser], List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(knowledgeSetForParser, transversalState)
  }

  "The List of Japanese Premises and empty Claims" should "be properly registered in the knowledge database and searchable." in {
    val propositionId = UUID.random.toString
    val knowledgeSetForParser: KnowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
      List(KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("A's hair is not black.", "en_US", "{}", false)),
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("B's hair is not blonde", "en_US", "{}", false)),
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("C's hair is not black.", "en_US", "{}", false))),
      List(PropositionRelation("AND", 0, 1), PropositionRelation("OR", 1, 2)),
      List.empty[KnowledgeForParser], List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(knowledgeSetForParser, transversalState)
    val result:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:PremiseNode{surface:'A'})-[*]->(:PremiseNode{surface:'is',isDenialWord:'true'})-[:LocalEdge{logicType:'AND'}]->(:PremiseNode{surface:'is',isDenialWord:'true'})<-[*]-(:PremiseNode{surface:'B'}) RETURN x")
    assert(result.hasNext)
    val result2:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:PremiseNode{surface:'B'})-[*]->(:PremiseNode{surface:'is',isDenialWord:'true'})-[:LocalEdge{logicType:'OR'}]->(:PremiseNode{surface:'is',isDenialWord:'true'})<-[*]-(:PremiseNode{surface:'C'}) RETURN x")
    assert(result2.hasNext)
    val result3: Result = Neo4JAccessor.executeQueryAndReturn("MATCH x = (:SemiGlobalPremiseNode{sentence:\"A\'s hair is not black.\"})-[:SemiGlobalEdge{logicType:'AND'}]->(:SemiGlobalPremiseNode{sentence:\"B\'s hair is not blonde\"})-[:SemiGlobalEdge{logicType:'OR'}]->(:SemiGlobalPremiseNode{sentence:\"C\'s hair is not black.\"}) RETURN x")
    assert(result3.hasNext)

  }

  "The List of English Claims and empty Premises" should "be properly registered in the knowledge database and searchable." in {
    val propositionId = UUID.random.toString
    val knowledgeSetForParser: KnowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
      List.empty[KnowledgeForParser], List.empty[PropositionRelation],
      List(KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("A's hair is not black.", "en_US", "{}", false)),
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("B's hair is not blonde", "en_US", "{}", false)),
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("C's hair is not black.", "en_US", "{}", false))),
      List(PropositionRelation("AND", 0, 1), PropositionRelation("OR", 1, 2))
    )
    Sentence2Neo4jTransformer.createGraph(knowledgeSetForParser, transversalState)
    val result:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ClaimNode{surface:'A'})-[*]->(:ClaimNode{surface:'is',isDenialWord:'true'})-[:LocalEdge{logicType:'AND'}]->(:ClaimNode{surface:'is',isDenialWord:'true'})<-[*]-(:ClaimNode{surface:'B'}) RETURN x")
    assert(result.hasNext)
    val result2:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ClaimNode{surface:'B'})-[*]->(:ClaimNode{surface:'is',isDenialWord:'true'})-[:LocalEdge{logicType:'OR'}]->(:ClaimNode{surface:'is',isDenialWord:'true'})<-[*]-(:ClaimNode{surface:'C'}) RETURN x")
    assert(result2.hasNext)
    val result3: Result = Neo4JAccessor.executeQueryAndReturn("MATCH x = (:SemiGlobalClaimNode{sentence:\"A\'s hair is not black.\"})-[:SemiGlobalEdge{logicType:'AND'}]->(:SemiGlobalClaimNode{sentence:\"B\'s hair is not blonde\"})-[:SemiGlobalEdge{logicType:'OR'}]->(:SemiGlobalClaimNode{sentence:\"C\'s hair is not black.\"}) RETURN x")
    assert(result3.hasNext)

  }

  "The List of Japanese Claims and Premises" should "be properly registered in the knowledge database and searchable." in {
    val propositionId = UUID.random.toString
    val knowledgeSetForParser: KnowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
      List(KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("A's hair is not black.", "en_US", "{}", false)),
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("B's hair is not blonde", "en_US", "{}", false)),
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("C's hair is not black.", "en_US", "{}", false))),
      List(PropositionRelation("AND", 0, 1), PropositionRelation("OR", 1, 2)),
      List(KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("D's hair is not black.", "en_US", "{}", false)),
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("E's hair is not blonde", "en_US", "{}", false)),
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("F's hair is not black.", "en_US", "{}", false))),
      List(PropositionRelation("OR", 0, 1), PropositionRelation("AND", 1, 2))
    )
    Sentence2Neo4jTransformer.createGraph(knowledgeSetForParser, transversalState)
    val result:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:PremiseNode{surface:'A'})-[*]->(:PremiseNode{surface:'is',isDenialWord:'true'})-[:LocalEdge{logicType:'AND'}]->(:PremiseNode{surface:'is',isDenialWord:'true'})<-[*]-(:PremiseNode{surface:'B'}) RETURN x")
    assert(result.hasNext)
    val result2:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:PremiseNode{surface:'B'})-[*]->(:PremiseNode{surface:'is',isDenialWord:'true'})-[:LocalEdge{logicType:'OR'}]->(:PremiseNode{surface:'is',isDenialWord:'true'})<-[*]-(:PremiseNode{surface:'C'}) RETURN x")
    assert(result2.hasNext)
    val result3:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ClaimNode{surface:'D'})-[*]->(:ClaimNode{surface:'is',isDenialWord:'true'})-[:LocalEdge{logicType:'OR'}]->(:ClaimNode{surface:'is',isDenialWord:'true'})<-[*]-(:ClaimNode{surface:'E'}) RETURN x")
    assert(result3.hasNext)
    val result4:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ClaimNode{surface:'E'})-[*]->(:ClaimNode{surface:'is',isDenialWord:'true'})-[:LocalEdge{logicType:'AND'}]->(:ClaimNode{surface:'is',isDenialWord:'true'})<-[*]-(:ClaimNode{surface:'F'}) RETURN x")
    assert(result4.hasNext)
    val result5: Result = Neo4JAccessor.executeQueryAndReturn("MATCH x = (:PremiseNode{surface:'A'})-[*]->(:PremiseNode{surface:'is'})-[:LocalEdge{logicType:'IMP'}]->(:ClaimNode{surface:'is'})<-[*]-(:ClaimNode{surface:'D'}) RETURN x")
    assert(result5.hasNext)
    val result6: Result = Neo4JAccessor.executeQueryAndReturn("MATCH x = (:SemiGlobalPremiseNode{sentence:\"A\'s hair is not black.\"})-[:SemiGlobalEdge{logicType:'AND'}]->(:SemiGlobalPremiseNode{sentence:\"B\'s hair is not blonde\"})-[:SemiGlobalEdge{logicType:'OR'}]->(:SemiGlobalPremiseNode{sentence:\"C\'s hair is not black.\"}) RETURN x")
    assert(result6.hasNext)
    val result7: Result = Neo4JAccessor.executeQueryAndReturn("MATCH x = (:SemiGlobalClaimNode{sentence:\"D\'s hair is not black.\"})-[:SemiGlobalEdge{logicType:'OR'}]->(:SemiGlobalClaimNode{sentence:\"E\'s hair is not blonde\"})-[:SemiGlobalEdge{logicType:'AND'}]->(:SemiGlobalClaimNode{sentence:\"F\'s hair is not black.\"}) RETURN x")
    assert(result7.hasNext)
    val result8: Result = Neo4JAccessor.executeQueryAndReturn("MATCH x = (:SemiGlobalPremiseNode{sentence:\"A\'s hair is not black.\"})-[:SemiGlobalEdge{logicType:'IMP'}]-(:SemiGlobalClaimNode{sentence:\"D\'s hair is not black.\"}) RETURN x")
    assert(result8.hasNext)

  }

}

