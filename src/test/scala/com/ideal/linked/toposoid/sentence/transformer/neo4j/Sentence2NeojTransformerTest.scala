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

class Sentence2NeojTransformerTest extends FlatSpec with DiagrammedAssertions with BeforeAndAfter with BeforeAndAfterAll {

  after {
    Neo4JAccessor.delete()
  }

  override def beforeAll(): Unit = {
    Neo4JAccessor.delete()
  }

  override def afterAll(): Unit = {
    Neo4JAccessor.delete()
  }

  "The list of sentences" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeList = List(Knowledge("太郎は映画を見た。", "{}"), Knowledge("花子の趣味はガーデニングです。","{}"))
    Sentence2Neo4jTransformer.createGraphAuto(knowledgeList)
    val result:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ClaimNode{surface:'太郎は'})-[:ClaimEdge]->(:ClaimNode{surface:'見た。'})<-[:ClaimEdge]-(:ClaimNode{surface:'映画を'}) RETURN x")
    assert(result.hasNext)
    val result2:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ClaimNode{surface:'花子の'})-[:ClaimEdge]->(:ClaimNode{surface:'趣味は'})-[:ClaimEdge]->(:ClaimNode{surface:'ガーデニングです。'}) RETURN x")
    assert(result2.hasNext)
    val result3:Result = Neo4JAccessor.executeQueryAndReturn("MAtCH x = (:SynonymNode{nodeName:'フィルム'})-[:SynonymEdge]->(:ClaimNode{surface:'映画を'}) return x")
    assert(result3.hasNext)
  }

  "The list of multiple sentences" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeList = List(Knowledge("二郎は映画を見た。明美の趣味はガーデニングです。", "{}"))
    Sentence2Neo4jTransformer.createGraphAuto(knowledgeList)
    val result:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ClaimNode{surface:'二郎は'})-[:ClaimEdge]->(:ClaimNode{surface:'見た。'})<-[:ClaimEdge]-(:ClaimNode{surface:'映画を'}) RETURN x")
    assert(result.hasNext)
    val result2:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ClaimNode{surface:'明美の'})-[:ClaimEdge]->(:ClaimNode{surface:'趣味は'})-[:ClaimEdge]->(:ClaimNode{surface:'ガーデニングです。'}) RETURN x")
    assert(result2.hasNext)
  }

  "The List of sentences including a premise" should "be properly registered in the knowledge database and searchable." in {
    val sentenceList = List(Knowledge("明日が雨ならば、三郎は映画を見るだろう。", "{}"))
    Sentence2Neo4jTransformer.createGraphAuto(sentenceList)
    val result:Result = Neo4JAccessor.executeQueryAndReturn("MATCH x = (:PremiseNode{surface:'明日が'})-[:PremiseEdge]->(:PremiseNode{surface:'雨ならば、'})-[:LogicEdge]->(:ClaimNode{surface:'見るだろう。'})<-[*]-(:ClaimNode) RETURN x")
    assert(result.hasNext)
  }

  "The list of sentences with json" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeList = List(Knowledge("三郎は映画を見た。", """{"id":"Test"}"""), Knowledge("明美の趣味はガーデニングです。","""{"日本語":"大丈夫かテスト"}"""))
    Sentence2Neo4jTransformer.createGraphAuto(knowledgeList)
    val result:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (n:ClaimNode) WHERE n.extentText='{\"id\":\"Test\"}' return x")
    assert(result.hasNext)
    val result2:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (n:ClaimNode) WHERE n.extentText='{\"日本語\":\"大丈夫かテスト\"}' return x")
    assert(result2.hasNext)
  }

  "The short sentence with json" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeList = List(Knowledge("セリヌンティウスである。", """{"id":"Test"}"""), Knowledge("セリヌンティウス","""{"id":"Test2"}"""), Knowledge("","""{"id":"Test3"}"""))
    Sentence2Neo4jTransformer.createGraphAuto(knowledgeList)
    val result:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (n:ClaimNode) WHERE n.extentText='{\"id\":\"Test\"}' return x")
    assert(result.hasNext)
    val result2:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (n:ClaimNode) WHERE n.extentText='{\"id\":\"Test2\"}' return x")
    assert(result2.hasNext)
    val result3:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (n:ClaimNode) WHERE n.extentText='{\"id\":\"Test3\"}' return x")
    assert(!result3.hasNext)
  }

}

