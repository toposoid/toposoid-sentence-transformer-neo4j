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
import com.ideal.linked.toposoid.knowledgebase.regist.model.Knowledge
import org.neo4j.driver.Result
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, DiagrammedAssertions, FlatSpec}

class Sentence2Neo4jTransformerEnglishTest extends FlatSpec with DiagrammedAssertions with BeforeAndAfter with BeforeAndAfterAll {

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
    val knowledgeList = List(Knowledge("That's life.", "en_US", "{}"), Knowledge("Seeing is believing.", "en_US" ,"{}"))
    Sentence2Neo4jTransformer.createGraphAuto(knowledgeList)
    val result:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ClaimNode{surface:'That'})-[:ClaimEdge]->(:ClaimNode{surface:\"\'s\"})<-[:ClaimEdge]-(:ClaimNode{surface:'life'}) RETURN x")
    assert(result.hasNext)
    val result2:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ClaimNode{surface:'Seeing'})-[:ClaimEdge]->(:ClaimNode{surface:'believing'})<-[:ClaimEdge]-(:ClaimNode{surface:'is'}) RETURN x")
    assert(result2.hasNext)
    val result3:Result = Neo4JAccessor.executeQueryAndReturn("MAtCH x = (:SynonymNode{nodeName:'living'})-[:SynonymEdge]->(:ClaimNode{surface:'life'}) return x")
    assert(result3.hasNext)
  }

  "The list of multiple english sentences" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeList = List(Knowledge("That's life. Seeing is believing.", "en_US", "{}"))
    Sentence2Neo4jTransformer.createGraphAuto(knowledgeList)
    val result:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ClaimNode{surface:'That'})-[:ClaimEdge]->(:ClaimNode{surface:\"\'s\"})<-[:ClaimEdge]-(:ClaimNode{surface:'life'}) RETURN x")
    assert(result.hasNext)
    val result2:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ClaimNode{surface:'Seeing'})-[:ClaimEdge]->(:ClaimNode{surface:'believing'})<-[:ClaimEdge]-(:ClaimNode{surface:'is'}) RETURN x")
    assert(result2.hasNext)
  }

  "The List of english sentences including a premise" should "be properly registered in the knowledge database and searchable." in {
    val sentenceList = List(Knowledge("If you can dream it, you can do it.", "en_US", "{}"))
    Sentence2Neo4jTransformer.createGraphAuto(sentenceList)
    val result:Result = Neo4JAccessor.executeQueryAndReturn("MATCH x = (:PremiseNode)-[*..]->(:PremiseNode{surface:'dream'})-[:LogicEdge]->(:ClaimNode{surface:'do'})<-[*..]-(:ClaimNode) RETURN x")
    assert(result.hasNext)
  }

  "The list of english sentences with json" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeList = List(Knowledge("That's life.", "en_US", """{"id":"Test"}"""), Knowledge("Seeing is believing.", "en_US", """{"dummy":"!\"#$%&\'()"}"""))
    Sentence2Neo4jTransformer.createGraphAuto(knowledgeList)
    val result:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (n:ClaimNode) WHERE n.extentText='{\"id\":\"Test\"}' return x")
    assert(result.hasNext)
    val result2:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (n:ClaimNode) WHERE n.extentText=~'.*\"dummy\".*' return x")
    assert(result2.hasNext)
  }

  "The short english sentence with json" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeList = List(Knowledge("naature", "en_US", """{"id":"Test"}"""), Knowledge("naature", "en_US","""{"id":"Test2"}"""), Knowledge("", "en_US","""{"id":"Test3"}"""))
    Sentence2Neo4jTransformer.createGraphAuto(knowledgeList)
    val result:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (n:ClaimNode) WHERE n.extentText='{\"id\":\"Test\"}' return x")
    assert(result.hasNext)
    val result2:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (n:ClaimNode) WHERE n.extentText='{\"id\":\"Test2\"}' return x")
    assert(result2.hasNext)
    val result3:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (n:ClaimNode) WHERE n.extentText='{\"id\":\"Test3\"}' return x")
    assert(!result3.hasNext)
  }



}

