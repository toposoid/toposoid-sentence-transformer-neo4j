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
import com.ideal.linked.toposoid.knowledgebase.regist.model._
import com.ideal.linked.toposoid.protocol.model.neo4j.Neo4jRecords
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import com.ideal.linked.toposoid.sentence.transformer.neo4j.TestUtilsEx.getAnalyzedPropositionSet
import io.jvm.uuid.UUID
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}

class Sentence2Neo4JTransformerDocumentJapaneseTest extends AnyFlatSpec with BeforeAndAfter with BeforeAndAfterAll {

  val transversalState: TransversalState = TransversalState(userId = "test-user", username = "guest", roleId = 0, csrfToken = "")

  before {
    TestUtilsEx.deleteNeo4JAllData(transversalState)
  }

  override def beforeAll(): Unit = {
    TestUtilsEx.deleteNeo4JAllData(transversalState)
  }

  override def afterAll(): Unit = {
    TestUtilsEx.deleteNeo4JAllData(transversalState)
  }


  "The list of global premise and claim in document" should "be properly registered in the knowledge database and searchable." in {

    val documentId = UUID.random.toString
    val propositionId = UUID.random.toString
    val sentenceId1 = UUID.random.toString
    val sentenceId2 = UUID.random.toString
    val documentPageReference1: DocumentPageReference = DocumentPageReference(pageNo = 1, references = List.empty[String], tableOfContents = List("目次項目1", "目次項目2"), headlines = List.empty[String])
    val documentPageReferencePremise: DocumentPageReference = DocumentPageReference(pageNo = 2, references = List.empty[String], tableOfContents = List.empty[String], headlines = List("TestPremise見出し1", "TestPremise見出し2"))
    val documentPageReferenceClaim: DocumentPageReference = DocumentPageReference(pageNo = 2, references = List.empty[String], tableOfContents = List.empty[String], headlines = List("TestClaim見出し1", "TestClaim見出し2"))
    val documentPageReference3: DocumentPageReference = DocumentPageReference(pageNo = 3, references = List("リファレンス1", "リファレンス2"), tableOfContents = List.empty[String], headlines = List.empty[String])

    val knowledgeForDocument: KnowledgeForDocument = KnowledgeForDocument(id = documentId, filename = "test.pdf", url = "http://xxxx/test.pdf", titleOfTopPage = "テストタイトル")

    val knowledgePremiseList = List(
      KnowledgeForParser(propositionId, sentenceId1, Knowledge("テスト文章1です。", "ja_JP", "{}", false, knowledgeForDocument = knowledgeForDocument, documentPageReference = documentPageReferencePremise)),
    )

    val knowledgeClaimList = List(
      KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("NO_REFFERENCE_" + documentId.toString + "_1", "ja_JP", "{}", false, knowledgeForDocument = knowledgeForDocument, documentPageReference = documentPageReference1)),
      KnowledgeForParser(propositionId, sentenceId2, Knowledge("テスト文章2です。", "ja_JP", "{}", false, knowledgeForDocument = knowledgeForDocument, documentPageReference = documentPageReferenceClaim)),
      KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("NO_REFFERENCE_" + documentId.toString + "_3", "@@_#1", "{}", false, knowledgeForDocument = knowledgeForDocument, documentPageReference = documentPageReference3)),
    )

    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(knowledgePremiseList, List.empty[PropositionRelation], knowledgeClaimList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSentenceSetForParser, transversalState), transversalState)

    val result: Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:GlobalNode {titleOfTopPage: 'テストタイトル', filename: 'test.pdf', url:'http://xxxx/test.pdf'}) RETURN x""", transversalState)
    assert(result.records.size == 1)
  }
}