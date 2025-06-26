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

    val knowledgePremiseList = List(
      KnowledgeForParser(propositionId, sentenceId1, Knowledge("This is a Test Sentence1.", "en_US", "{}", false, knowledgeForDocument = knowledgeForDocument, documentPageReference = documentPageReferencePremise)),
    )

    val knowledgeClaimList = List(
      KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("NO_REFFERENCE_" + documentId.toString + "_1", "en_US", "{}", false, knowledgeForDocument = knowledgeForDocument, documentPageReference = documentPageReference1)),
      KnowledgeForParser(propositionId, sentenceId2, Knowledge("This is a Test Sentence2.", "en_US", "{}", false, knowledgeForDocument = knowledgeForDocument, documentPageReference = documentPageReferenceClaim)),
      KnowledgeForParser(propositionId, UUID.random.toString, Knowledge("NO_REFFERENCE_" + documentId.toString + "_3", "@@_#1", "{}", false, knowledgeForDocument = knowledgeForDocument, documentPageReference = documentPageReference3)),
    )

    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(knowledgePremiseList, List.empty[PropositionRelation], knowledgeClaimList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSentenceSetForParser, transversalState), transversalState)

    val result: Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:GlobalNode {titleOfTopPage: 'Test-Title', filename: 'test.pdf', url:'http://xxxx/test.pdf'}) RETURN x""", transversalState)
    assert(result.records.size == 1)
  }
}