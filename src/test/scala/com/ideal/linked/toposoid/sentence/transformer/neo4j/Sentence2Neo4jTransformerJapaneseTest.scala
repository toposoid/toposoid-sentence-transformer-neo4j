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
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, KnowledgeSentenceSet, PropositionRelation}
import org.neo4j.driver.Result
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, DiagrammedAssertions, FlatSpec}

class Sentence2Neo4jTransformerJapaneseTest extends FlatSpec with DiagrammedAssertions with BeforeAndAfter with BeforeAndAfterAll {

  before {
    Neo4JAccessor.delete()
  }

  override def beforeAll(): Unit = {
    Neo4JAccessor.delete()
  }

  override def afterAll(): Unit = {
    Neo4JAccessor.delete()
  }

  "The list of japanese sentences" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeList = List(Knowledge("太郎は映画を見た。", "ja_JP", "{}", false), Knowledge("花子の趣味はガーデニングです。", "ja_JP" ,"{}", false))
    Sentence2Neo4jTransformer.createGraphAuto(knowledgeList)
    val result:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ClaimNode{surface:'太郎は'})-[:ClaimEdge]->(:ClaimNode{surface:'見た。'})<-[:ClaimEdge]-(:ClaimNode{surface:'映画を'}) RETURN x")
    assert(result.hasNext)
    val result2:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ClaimNode{surface:'花子の'})-[:ClaimEdge]->(:ClaimNode{surface:'趣味は'})-[:ClaimEdge]->(:ClaimNode{surface:'ガーデニングです。'}) RETURN x")
    assert(result2.hasNext)
    val result3:Result = Neo4JAccessor.executeQueryAndReturn("MAtCH x = (:SynonymNode{nodeName:'フィルム'})-[:SynonymEdge]->(:ClaimNode{surface:'映画を'}) return x")
    assert(result3.hasNext)
  }

  "The list of multiple japanese sentences" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeList = List(Knowledge("二郎は映画を見た。明美の趣味はガーデニングです。", "ja_JP", "{}", false))
    Sentence2Neo4jTransformer.createGraphAuto(knowledgeList)
    val result:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ClaimNode{surface:'二郎は'})-[:ClaimEdge]->(:ClaimNode{surface:'見た。'})<-[:ClaimEdge]-(:ClaimNode{surface:'映画を'}) RETURN x")
    assert(result.hasNext)
    val result2:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ClaimNode{surface:'明美の'})-[:ClaimEdge]->(:ClaimNode{surface:'趣味は'})-[:ClaimEdge]->(:ClaimNode{surface:'ガーデニングです。'}) RETURN x")
    assert(result2.hasNext)
  }

  "The List of japanese sentences including a premise" should "be properly registered in the knowledge database and searchable." in {
    val sentenceList = List(Knowledge("明日が雨ならば、三郎は映画を見るだろう。", "ja_JP", "{}", false))
    Sentence2Neo4jTransformer.createGraphAuto(sentenceList)
    val result:Result = Neo4JAccessor.executeQueryAndReturn("MATCH x = (:PremiseNode{surface:'明日が'})-[:PremiseEdge]->(:PremiseNode{surface:'雨ならば、'})-[:LogicEdge]->(:ClaimNode{surface:'見るだろう。'})<-[*]-(:ClaimNode) RETURN x")
    assert(result.hasNext)
  }

  "The list of japanese sentences with json" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeList = List(Knowledge("三郎は映画を見た。", "ja_JP", """{"id":"Test"}""", false), Knowledge("明美の趣味はガーデニングです。", "ja_JP", """{"日本語":"大丈夫かテスト"}""", false))
    Sentence2Neo4jTransformer.createGraphAuto(knowledgeList)
    val result:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (n:ClaimNode) WHERE n.extentText='{\"id\":\"Test\"}' return x")
    assert(result.hasNext)
    val result2:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (n:ClaimNode) WHERE n.extentText='{\"日本語\":\"大丈夫かテスト\"}' return x")
    assert(result2.hasNext)
  }

  "The short japanese sentence with json" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeList = List(Knowledge("セリヌンティウスである。", "ja_JP", """{"id":"Test"}""", false), Knowledge("セリヌンティウス", "ja_JP","""{"id":"Test2"}""", false), Knowledge("", "ja_JP","""{"id":"Test3"}""", false))
    Sentence2Neo4jTransformer.createGraphAuto(knowledgeList)
    val result:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (n:ClaimNode) WHERE n.extentText='{\"id\":\"Test\"}' return x")
    assert(result.hasNext)
    val result2:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (n:ClaimNode) WHERE n.extentText='{\"id\":\"Test2\"}' return x")
    assert(result2.hasNext)
    val result3:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (n:ClaimNode) WHERE n.extentText='{\"id\":\"Test3\"}' return x")
    assert(!result3.hasNext)
  }

  "The Empty knowledge" should "not fail" in {
    val knowledgeSet:KnowledgeSentenceSet = KnowledgeSentenceSet(List.empty[Knowledge], List.empty[PropositionRelation], List.empty[Knowledge], List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(knowledgeSet)
  }

  "The List of Japanese Premises and empty Claims" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeSet:KnowledgeSentenceSet = KnowledgeSentenceSet(
      List(Knowledge("Bは黒髪ではない。", "ja_JP", "{}", false),
        Knowledge("Cはブロンドではない。", "ja_JP", "{}", false),
        Knowledge("Aは黒髪ではない。", "ja_JP", "{}", false)),
      List(PropositionRelation("AND", 0, 1), PropositionRelation("OR", 1, 2)),
      List.empty[Knowledge], List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(knowledgeSet)
    val result:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:PremiseNode{surface:'Ｃは'})-[:PremiseEdge]->(:PremiseNode{surface:'ブロンドではない。'})<-[:LogicEdge{operator:'AND'}]-(:PremiseNode{surface:'黒髪ではない。'})<-[:PremiseEdge]-(:PremiseNode{surface:'Ｂは'}) RETURN x")
    assert(result.hasNext)
    val result2:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:PremiseNode{surface:'Ａは'})-[:PremiseEdge]->(:PremiseNode{surface:'黒髪ではない。'})<-[:LogicEdge{operator:'OR'}]-(:PremiseNode{surface:'ブロンドではない。'})<-[:PremiseEdge]-(:PremiseNode{surface:'Ｃは'}) RETURN x")
    assert(result2.hasNext)
  }

  "The List of Japanese Claims and empty Premises" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeSet:KnowledgeSentenceSet = KnowledgeSentenceSet(
      List.empty[Knowledge], List.empty[PropositionRelation],
      List(Knowledge("Bは黒髪ではない。", "ja_JP", "{}", false),
        Knowledge("Cはブロンドではない。", "ja_JP", "{}", false),
        Knowledge("Aは黒髪ではない。", "ja_JP", "{}", false)),
      List(PropositionRelation("AND", 0, 1), PropositionRelation("OR", 1, 2))
      )
    Sentence2Neo4jTransformer.createGraph(knowledgeSet)
    val result:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ClaimNode{surface:'Ｃは'})-[:ClaimEdge]->(:ClaimNode{surface:'ブロンドではない。'})<-[:LogicEdge{operator:'AND'}]-(:ClaimNode{surface:'黒髪ではない。'})<-[:ClaimEdge]-(:ClaimNode{surface:'Ｂは'}) RETURN x")
    assert(result.hasNext)
    val result2:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ClaimNode{surface:'Ａは'})-[:ClaimEdge]->(:ClaimNode{surface:'黒髪ではない。'})<-[:LogicEdge{operator:'OR'}]-(:ClaimNode{surface:'ブロンドではない。'})<-[:ClaimEdge]-(:ClaimNode{surface:'Ｃは'}) RETURN x")
    assert(result2.hasNext)
  }

  "The List of Japanese Claims and Premises" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeSet:KnowledgeSentenceSet = KnowledgeSentenceSet(

      List(Knowledge("Bは黒髪ではない。", "ja_JP", "{}", false),
        Knowledge("Cはブロンドではない。", "ja_JP", "{}", false),
        Knowledge("Aは黒髪ではない。", "ja_JP", "{}", false)),
      List(PropositionRelation("AND", 0, 1), PropositionRelation("OR", 1, 2)),
      List(Knowledge("Dは黒髪ではない。", "ja_JP", "{}", false),
        Knowledge("Eはブロンドではない。", "ja_JP", "{}", false),
        Knowledge("Fは黒髪ではない。", "ja_JP", "{}")),
      List(PropositionRelation("OR", 0, 1), PropositionRelation("AND", 1, 2))
    )
    Sentence2Neo4jTransformer.createGraph(knowledgeSet)
    val result:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:PremiseNode{surface:'Ｃは'})-[:PremiseEdge]->(:PremiseNode{surface:'ブロンドではない。'})<-[:LogicEdge{operator:'AND'}]-(:PremiseNode{surface:'黒髪ではない。'})<-[:PremiseEdge]-(:PremiseNode{surface:'Ｂは'}) RETURN x")
    assert(result.hasNext)
    val result2:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:PremiseNode{surface:'Ａは'})-[:PremiseEdge]->(:PremiseNode{surface:'黒髪ではない。'})<-[:LogicEdge{operator:'OR'}]-(:PremiseNode{surface:'ブロンドではない。'})<-[:PremiseEdge]-(:PremiseNode{surface:'Ｃは'}) RETURN x")
    assert(result2.hasNext)
    val result3:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ClaimNode{surface:'Ｄは'})-[:ClaimEdge]->(:ClaimNode{surface:'黒髪ではない。'})-[:LogicEdge{operator:'OR'}]->(:ClaimNode{surface:'ブロンドではない。'})<-[:ClaimEdge]-(:ClaimNode{surface:'Ｅは'}) RETURN x")
    assert(result3.hasNext)
    val result4:Result =Neo4JAccessor.executeQueryAndReturn("MATCH x = (:ClaimNode{surface:'Ｅは'})-[:ClaimEdge]->(:ClaimNode{surface:'ブロンドではない。'})-[:LogicEdge{operator:'AND'}]->(:ClaimNode{surface:'黒髪ではない。'})<-[:ClaimEdge]-(:ClaimNode{surface:'Ｆは'}) RETURN x")
    assert(result4.hasNext)

  }

}

