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

import com.ideal.linked.toposoid.common.{Neo4JUtilsImpl, TransversalState}
import com.ideal.linked.toposoid.knowledgebase.regist.model._
import com.ideal.linked.toposoid.protocol.model.neo4j.Neo4jRecords
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import com.ideal.linked.toposoid.sentence.transformer.neo4j.TestUtilsEx.getAnalyzedPropositionSet
import io.jvm.uuid.UUID
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}

class Sentence2Neo4JTransformerDocumentEnglishTest extends AnyFlatSpec with BeforeAndAfter with BeforeAndAfterAll {

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
    val documentPageReference1: DocumentPageReference = DocumentPageReference(pageNo = 1, references = List.empty[String], tableOfContents = List("TOC1", "TOC2"), headlines = List.empty[String])
    val documentPageReferencePremise: DocumentPageReference = DocumentPageReference(pageNo = 2, references = List.empty[String], tableOfContents = List.empty[String], headlines = List("TestPremiseHeadline1", "TestPremiseHeadline2"))
    val documentPageReferenceClaim: DocumentPageReference = DocumentPageReference(pageNo = 2, references = List.empty[String], tableOfContents = List.empty[String], headlines = List("TestClaimHeadline", "TestClaimHeadline2"))
    val documentPageReference3: DocumentPageReference = DocumentPageReference(pageNo = 3, references = List("Reference1", "Reference2"), tableOfContents = List.empty[String], headlines = List.empty[String])

    val knowledgeForDocument: KnowledgeForDocument = KnowledgeForDocument(id = documentId, filename = "test.pdf", url = "http://xxxx/test.pdf", titleOfTopPage = "Test-Title")

    val knowledgeClaimHavingTOCList = List(
      KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("NO_REFFERENCE_" + documentId.toString + "_1", "@@_#1", "{}", false, knowledgeForDocument = knowledgeForDocument, documentPageReference = documentPageReference1)),
    )
    val knowledgePremiseList = List(
      KnowledgeForParser(propositionId, sentenceId1, Knowledge("This is a Test Sentence1.", "en_US", "{}", false, knowledgeForDocument = knowledgeForDocument, documentPageReference = documentPageReferencePremise)),
    )
    val knowledgeClaimList = List(
      KnowledgeForParser(propositionId, sentenceId2, Knowledge("This is a Test Sentence2.", "en_US", "{}", false, knowledgeForDocument = knowledgeForDocument, documentPageReference = documentPageReferenceClaim)),
    )
    val knowledgeClaimHavingReferenceList = List(
      KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("NO_REFFERENCE_" + documentId.toString + "_2", "@@_#1", "{}", false, knowledgeForDocument = knowledgeForDocument, documentPageReference = documentPageReference3)),
    )

    val knowledgeSentenceSetForParser1 = KnowledgeSentenceSetForParser(List.empty[KnowledgeForParser], List.empty[PropositionRelation], knowledgeClaimHavingTOCList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSentenceSetForParser1, transversalState), transversalState)
    val knowledgeSentenceSetForParser2 = KnowledgeSentenceSetForParser(knowledgePremiseList, List.empty[PropositionRelation], knowledgeClaimList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSentenceSetForParser2, transversalState), transversalState)
    val knowledgeSentenceSetForParser3 = KnowledgeSentenceSetForParser(List.empty[KnowledgeForParser], List.empty[PropositionRelation], knowledgeClaimHavingReferenceList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSentenceSetForParser3, transversalState), transversalState)

    val result1: Neo4jRecords = TestUtilsEx.executeQueryAndReturn(s"""MATCH x = (:SemiGlobalClaimNode{sentence:'NO_REFFERENCE_${documentId}_1'}) RETURN x""", transversalState)
    assert(result1.records.size == 1)
    val result2: Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:GlobalNode {titleOfTopPage: 'Test-Title', filename: 'test.pdf', url:'http://xxxx/test.pdf'}) RETURN x""", transversalState)
    assert(result2.records.size == 1)
    val result3: Neo4jRecords = TestUtilsEx.executeQueryAndReturn(s"""MATCH x = (:SemiGlobalClaimNode{sentence:'NO_REFFERENCE_${documentId}_2'}) RETURN x""", transversalState)
    assert(result3.records.size == 1)

  }
}