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

import com.ideal.linked.toposoid.common.TransversalState
import com.ideal.linked.toposoid.knowledgebase.regist.model.{DocumentPageReference, Knowledge, KnowledgeForDocument, KnowledgeForImage, PropositionRelation}
import com.ideal.linked.toposoid.protocol.model.neo4j.Neo4jRecords
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import com.ideal.linked.toposoid.sentence.transformer.neo4j.TestUtilsEx.getAnalyzedPropositionSet
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.scalatest.flatspec.AnyFlatSpec
import io.jvm.uuid.UUID

class Sentence2Neo4jTransformerJapaneseTest extends AnyFlatSpec  with BeforeAndAfter with BeforeAndAfterAll {

  val transversalState:TransversalState = TransversalState(userId="test-user", username="guest", roleId=0, csrfToken = "")

  before {
    TestUtilsEx.deleteNeo4JAllData(transversalState)
  }

  override def beforeAll(): Unit = {
    TestUtilsEx.deleteNeo4JAllData(transversalState)
  }

  override def afterAll(): Unit = {
    TestUtilsEx.deleteNeo4JAllData(transversalState)
  }

  
  "The list of japanese sentences" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeList = List(KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("太郎は映画を見た。", "ja_JP", "{}", false)), KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("花子の趣味はガーデニングです。", "ja_JP" ,"{}", false)))
    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(List.empty[KnowledgeForParser], List.empty[PropositionRelation], knowledgeList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSentenceSetForParser, transversalState), transversalState)
    val result:Neo4jRecords =TestUtilsEx.executeQueryAndReturn("""MATCH x = (:ClaimNode{surface:'太郎は'})-[:LocalEdge]->(:ClaimNode{surface:'見た。'})<-[:LocalEdge]-(:ClaimNode{surface:'映画を'}) RETURN x""", transversalState)
    assert(result.records.size == 1)
    val result2:Neo4jRecords =TestUtilsEx.executeQueryAndReturn("""MATCH x = (:ClaimNode{surface:'花子の'})-[:LocalEdge]->(:ClaimNode{surface:'趣味は'})-[:LocalEdge]->(:ClaimNode{surface:'ガーデニングです。'}) RETURN x""", transversalState)
    assert(result2.records.size == 1)
    val result3:Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MAtCH x = (:SynonymNode{nodeName:'フィルム'})-[:SynonymEdge]->(:ClaimNode{surface:'映画を'}) return x""", transversalState)
    assert(result3.records.size == 1)
    val result4:Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:SemiGlobalClaimNode{sentence:'太郎は映画を見た。'}) RETURN x""", transversalState)
    assert(result4.records.size == 1)
    val result5: Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:SemiGlobalClaimNode{sentence:'花子の趣味はガーデニングです。'}) RETURN x""", transversalState)
    assert(result5.records.size == 1)
    val result6: Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:SemiGlobalClaimNode{sentence:'太郎は映画を見た。'})-[*]-(:SemiGlobalClaimNode{sentence:'花子の趣味はガーデニングです。'}) RETURN x""", transversalState)
    assert(result6.records.size == 0)
  }

  "The list of multiple japanese sentences" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeList = List(KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("二郎は映画を見た。明美の趣味はガーデニングです。", "ja_JP", "{}", false)))
    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(List.empty[KnowledgeForParser], List.empty[PropositionRelation], knowledgeList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSentenceSetForParser, transversalState), transversalState)
    val result:Neo4jRecords =TestUtilsEx.executeQueryAndReturn("""MATCH x = (:ClaimNode{surface:'二郎は'})-[:LocalEdge]->(:ClaimNode{surface:'見た。'})<-[:LocalEdge]-(:ClaimNode{surface:'映画を'}) RETURN x""", transversalState)
    assert(result.records.size == 1)
    val result2:Neo4jRecords =TestUtilsEx.executeQueryAndReturn("""MATCH x = (:ClaimNode{surface:'明美の'})-[:LocalEdge]->(:ClaimNode{surface:'趣味は'})-[:LocalEdge]->(:ClaimNode{surface:'ガーデニングです。'}) RETURN x""", transversalState)
    assert(result2.records.size == 1)
    val result3: Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:SemiGlobalClaimNode{sentence:'二郎は映画を見た。明美の趣味はガーデニングです。'}) RETURN x""", transversalState)
    assert(result3.records.size == 1)
  }

  "The List of japanese sentences including a premise" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeList = List(KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("明日が雨ならば、三郎は映画を見るだろう。", "ja_JP", "{}", false)))
    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(List.empty[KnowledgeForParser], List.empty[PropositionRelation], knowledgeList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSentenceSetForParser, transversalState), transversalState)
    val result:Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:ClaimNode{surface:'明日が'})-[:LocalEdge]->(:ClaimNode{surface:'雨ならば、'})-[:LocalEdge]->(:ClaimNode{surface:'見るだろう。'})<-[*]-(:ClaimNode) RETURN x""", transversalState)
    assert(result.records.size >= 1 )
    val result2: Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:SemiGlobalClaimNode{sentence:'明日が雨ならば、三郎は映画を見るだろう。'}) RETURN x""", transversalState)
    assert(result2.records.size == 1)

  }

  "The list of japanese sentences with json" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeList = List(KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("三郎は映画を見た。", "ja_JP", """{"id":"Test"}""", false)), KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("明美の趣味はガーデニングです。", "ja_JP", """{"日本語":"大丈夫かテスト"}""", false)))
    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(List.empty[KnowledgeForParser], List.empty[PropositionRelation], knowledgeList, List(PropositionRelation("AND", 0,1)))
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSentenceSetForParser, transversalState), transversalState)
    //val result:Neo4jRecords =TestUtils.executeQueryAndReturn("""MATCH x = (n:ClaimNode) WHERE n.extentText='{\"id\":\"Test\"}' return x""", transversalState)
    //assert(result.records.size == 1)
    //val result2:Neo4jRecords =TestUtils.executeQueryAndReturn("""MATCH x = (n:ClaimNode) WHERE n.extentText='{\"日本語\":\"大丈夫かテスト\"}' return x""", transversalState)
    //assert(result2.records.size == 1)
    val result3: Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:SemiGlobalClaimNode{sentence:'三郎は映画を見た。'})-[:SemiGlobalEdge{logicType:'AND'}]-(:SemiGlobalClaimNode{sentence:'明美の趣味はガーデニングです。'}) RETURN x""", transversalState)
    assert(result3.records.size == 1)
  }

  "The short japanese sentence with json" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeList = List(KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("セリヌンティウスである。", "ja_JP", """{"id":"Test"}""", false)), KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("セリヌンティウス", "ja_JP","""{"id":"Test2"}""", false)), KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("", "ja_JP","""{"id":"Test3"}""", false)))
    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(List.empty[KnowledgeForParser], List.empty[PropositionRelation], knowledgeList, List(PropositionRelation("AND", 0,1)))
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSentenceSetForParser, transversalState), transversalState)
    //val result:Neo4jRecords =TestUtils.executeQueryAndReturn("""MATCH x = (n:ClaimNode) WHERE n.extentText='{\"id\":\"Test\"}' return x""", transversalState)
    //assert(result.records.size == 1)
    //val result2:Neo4jRecords =TestUtils.executeQueryAndReturn("""MATCH x = (n:ClaimNode) WHERE n.extentText='{\"id\":\"Test2\"}' return x""", transversalState)
    //assert(result2.records.size == 1)
    //val result3:Neo4jRecords =TestUtils.executeQueryAndReturn("""MATCH x = (n:ClaimNode) WHERE n.extentText='{\"id\":\"Test3\"}' return x""", transversalState)
    //assert(!result3.records.size == 1)
    val result4: Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:SemiGlobalClaimNode{sentence:'セリヌンティウスである。'}) RETURN x""", transversalState)
    assert(result4.records.size == 1)
    val result5: Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:SemiGlobalClaimNode{sentence:'セリヌンティウス'}) RETURN x""", transversalState)
    assert(result5.records.size == 1)
  }

  "The Empty knowledge" should "not fail" in {
    val knowledgeSet:KnowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(List.empty[KnowledgeForParser], List.empty[PropositionRelation], List.empty[KnowledgeForParser], List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSet, transversalState), transversalState)
  }

  "The List of Japanese Premises and empty Claims" should "be properly registered in the knowledge database and searchable." in {
    val propositionId = UUID.random.toString
    val knowledgeSetForParser:KnowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
      List(KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("Bは黒髪ではない。", "ja_JP", "{}", false)),
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("Cはブロンドではない。", "ja_JP", "{}", false)),
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("Aは黒髪ではない。", "ja_JP", "{}", false))),
      List(PropositionRelation("AND", 0, 1), PropositionRelation("OR", 1, 2)),
      List.empty[KnowledgeForParser], List.empty[PropositionRelation])
      Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSetForParser, transversalState), transversalState)
    val result:Neo4jRecords =TestUtilsEx.executeQueryAndReturn("""MATCH x = (:PremiseNode{surface:'Ｃは'})-[:LocalEdge]->(:PremiseNode{surface:'ブロンドではない。'})<-[:LocalEdge{logicType:'AND'}]-(:PremiseNode{surface:'黒髪ではない。'})<-[:LocalEdge]-(:PremiseNode{surface:'Ｂは'}) RETURN x""", transversalState)
    assert(result.records.size == 1)
    val result2:Neo4jRecords =TestUtilsEx.executeQueryAndReturn("""MATCH x = (:PremiseNode{surface:'Ａは'})-[:LocalEdge]->(:PremiseNode{surface:'黒髪ではない。'})<-[:LocalEdge{logicType:'OR'}]-(:PremiseNode{surface:'ブロンドではない。'})<-[:LocalEdge]-(:PremiseNode{surface:'Ｃは'}) RETURN x""", transversalState)
    assert(result2.records.size == 1)
    val result3: Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:SemiGlobalPremiseNode{sentence:'Bは黒髪ではない。'})-[:SemiGlobalEdge{logicType:'AND'}]->(:SemiGlobalPremiseNode{sentence:'Cはブロンドではない。'})-[:SemiGlobalEdge{logicType:'OR'}]->(:SemiGlobalPremiseNode{sentence:'Aは黒髪ではない。'}) RETURN x""", transversalState)
    assert(result3.records.size == 1)


  }

  "The List of Japanese Claims and empty Premises" should "be properly registered in the knowledge database and searchable." in {
    val propositionId = UUID.random.toString
    val knowledgeSetForParser:KnowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
      List.empty[KnowledgeForParser], List.empty[PropositionRelation],
      List(KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("Bは黒髪ではない。", "ja_JP", "{}", false)),
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("Cはブロンドではない。", "ja_JP", "{}", false)),
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("Aは黒髪ではない。", "ja_JP", "{}", false))),
      List(PropositionRelation("AND", 0, 1), PropositionRelation("OR", 1, 2))
      )
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSetForParser, transversalState), transversalState)
    val result:Neo4jRecords =TestUtilsEx.executeQueryAndReturn("""MATCH x = (:ClaimNode{surface:'Ｃは'})-[:LocalEdge]->(:ClaimNode{surface:'ブロンドではない。'})<-[:LocalEdge{logicType:'AND'}]-(:ClaimNode{surface:'黒髪ではない。'})<-[:LocalEdge]-(:ClaimNode{surface:'Ｂは'}) RETURN x""", transversalState)
    assert(result.records.size == 1)
    val result2:Neo4jRecords =TestUtilsEx.executeQueryAndReturn("""MATCH x = (:ClaimNode{surface:'Ａは'})-[:LocalEdge]->(:ClaimNode{surface:'黒髪ではない。'})<-[:LocalEdge{logicType:'OR'}]-(:ClaimNode{surface:'ブロンドではない。'})<-[:LocalEdge]-(:ClaimNode{surface:'Ｃは'}) RETURN x""", transversalState)
    assert(result2.records.size == 1)
    val result3: Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:SemiGlobalClaimNode{sentence:'Bは黒髪ではない。'})-[:SemiGlobalEdge{logicType:'AND'}]->(:SemiGlobalClaimNode{sentence:'Cはブロンドではない。'})-[:SemiGlobalEdge{logicType:'OR'}]->(:SemiGlobalClaimNode{sentence:'Aは黒髪ではない。'}) RETURN x""", transversalState)
    assert(result3.records.size == 1)

  }

  "The List of Japanese Claims and Premises" should "be properly registered in the knowledge database and searchable." in {
    val propositionId = UUID.random.toString
    val knowledgeSentenceSetForParser:KnowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(
      List(KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("Bは黒髪ではない。", "ja_JP", "{}", false)),
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("Cはブロンドではない。", "ja_JP", "{}", false)),
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("Aは黒髪ではない。", "ja_JP", "{}", false))),
      List(PropositionRelation("AND", 0, 1), PropositionRelation("OR", 1, 2)),
      List(KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("Dは黒髪ではない。", "ja_JP", "{}", false)),
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("Eはブロンドではない。", "ja_JP", "{}", false)),
        KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("Fは黒髪ではない。", "ja_JP", "{}"))),
      List(PropositionRelation("OR", 0, 1), PropositionRelation("AND", 1, 2))
    )
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSentenceSetForParser, transversalState), transversalState)
    val result:Neo4jRecords =TestUtilsEx.executeQueryAndReturn("""MATCH x = (:PremiseNode{surface:'Ｃは'})-[:LocalEdge]->(:PremiseNode{surface:'ブロンドではない。'})<-[:LocalEdge{logicType:'AND'}]-(:PremiseNode{surface:'黒髪ではない。'})<-[:LocalEdge]-(:PremiseNode{surface:'Ｂは'}) RETURN x""", transversalState)
    assert(result.records.size == 1)
    val result2:Neo4jRecords =TestUtilsEx.executeQueryAndReturn("""MATCH x = (:PremiseNode{surface:'Ａは'})-[:LocalEdge]->(:PremiseNode{surface:'黒髪ではない。'})<-[:LocalEdge{logicType:'OR'}]-(:PremiseNode{surface:'ブロンドではない。'})<-[:LocalEdge]-(:PremiseNode{surface:'Ｃは'}) RETURN x""", transversalState)
    assert(result2.records.size == 1)
    val result3:Neo4jRecords =TestUtilsEx.executeQueryAndReturn("""MATCH x = (:ClaimNode{surface:'Ｄは'})-[:LocalEdge]->(:ClaimNode{surface:'黒髪ではない。'})-[:LocalEdge{logicType:'OR'}]->(:ClaimNode{surface:'ブロンドではない。'})<-[:LocalEdge]-(:ClaimNode{surface:'Ｅは'}) RETURN x""", transversalState)
    assert(result3.records.size == 1)
    val result4:Neo4jRecords =TestUtilsEx.executeQueryAndReturn("""MATCH x = (:ClaimNode{surface:'Ｅは'})-[:LocalEdge]->(:ClaimNode{surface:'ブロンドではない。'})-[:LocalEdge{logicType:'AND'}]->(:ClaimNode{surface:'黒髪ではない。'})<-[:LocalEdge]-(:ClaimNode{surface:'Ｆは'}) RETURN x""", transversalState)
    assert(result4.records.size == 1)
    val result5: Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:PremiseNode{surface:'Ｂは'})-[:LocalEdge]->(:PremiseNode{surface:'黒髪ではない。'})-[:LocalEdge{logicType:'IMP'}]->(:ClaimNode{surface:'黒髪ではない。'})<-[:LocalEdge]-(:ClaimNode{surface:'Ｄは'}) RETURN x""", transversalState)
    assert(result5.records.size == 1)
    val result6: Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:SemiGlobalPremiseNode{sentence:'Bは黒髪ではない。'})-[:SemiGlobalEdge{logicType:'AND'}]->(:SemiGlobalPremiseNode{sentence:'Cはブロンドではない。'})-[:SemiGlobalEdge{logicType:'OR'}]->(:SemiGlobalPremiseNode{sentence:'Aは黒髪ではない。'}) RETURN x""", transversalState)
    assert(result6.records.size == 1)
    val result7: Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:SemiGlobalClaimNode{sentence:'Dは黒髪ではない。'})-[:SemiGlobalEdge{logicType:'OR'}]->(:SemiGlobalClaimNode{sentence:'Eはブロンドではない。'})-[:SemiGlobalEdge{logicType:'AND'}]->(:SemiGlobalClaimNode{sentence:'Fは黒髪ではない。'}) RETURN x""", transversalState)
    assert(result7.records.size == 1)
    val result8: Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:SemiGlobalPremiseNode{sentence:'Bは黒髪ではない。'})-[:SemiGlobalEdge{logicType:'IMP'}]-(:SemiGlobalClaimNode{sentence:'Dは黒髪ではない。'}) RETURN x""", transversalState)
    assert(result8.records.size == 1)


  }

  "The list of japanese sentences with documentId" should "be properly registered in the knowledge database and searchable." in {
    val knowledgeForDocument = KnowledgeForDocument(id=UUID.random.toString, filename = "TEST.pdf", url = "http://hoge/TEST.pdf", titleOfTopPage = "TextTitle")
    val documentPageReference1 =  DocumentPageReference(pageNo = 1, references = List.empty[String], tableOfContents = List.empty[String], headlines = List.empty[String])
    val documentPageReference2 =  DocumentPageReference(pageNo = 2, references = List.empty[String], tableOfContents = List.empty[String], headlines = List.empty[String])
    val knowledgeList = List(
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("太郎は映画を見た。", "ja_JP", "{}", false, knowledgeForDocument=knowledgeForDocument, documentPageReference = documentPageReference1)),
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("花子の趣味はガーデニングです。", "ja_JP", "{}", false, knowledgeForDocument=knowledgeForDocument, documentPageReference = documentPageReference2)))
    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(List.empty[KnowledgeForParser], List.empty[PropositionRelation], knowledgeList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSentenceSetForParser, transversalState), transversalState)
    val result: Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:GlobalNode{filename:'TEST.pdf'}) RETURN x""", transversalState)
    assert(result.records.size == 1)
  }

}

