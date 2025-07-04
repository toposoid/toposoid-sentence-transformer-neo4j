
package com.ideal.linked.toposoid.sentence.transformer.neo4j

import com.ideal.linked.toposoid.common.TransversalState
import com.ideal.linked.toposoid.knowledgebase.regist.model._
import com.ideal.linked.toposoid.protocol.model.neo4j.Neo4jRecords
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import com.ideal.linked.toposoid.sentence.transformer.neo4j.TestUtilsEx.getAnalyzedPropositionSet
import io.jvm.uuid.UUID
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}

class Sentence2Neo4JTransformerTableJapaneseTest extends AnyFlatSpec with BeforeAndAfter with BeforeAndAfterAll {

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

  "The list of local claim images" should "be properly registered in the knowledge database and searchable." in {
    val reference1 = Reference(url = "http://xxx/yyy.tsv", surface = "表１です。", surfaceIndex = 1, isWholeSentence = false, originalUrlOrReference = "")
    val referenceTable1 = TableReference(reference = reference1)
    val featureId1 = UUID.random.toString
    val knowledgeForTable1 = KnowledgeForTable(featureId1, referenceTable1)

    val reference2 = Reference(url = "http://xxx/zzz.tsv", surface = "表２です。", surfaceIndex = 1, isWholeSentence = false, originalUrlOrReference = "")
    val referenceTable2 = TableReference(reference = reference2)
    val featureId2 = UUID.random.toString
    val knowledgeForTable2 = KnowledgeForTable(featureId2, referenceTable2)


    val knowledgeList = List(
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("これは表１です。", "ja_JP", "{}", false, List.empty[KnowledgeForImage], List(knowledgeForTable1))),
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("これは表２です。", "ja_JP" ,"{}", false, List.empty[KnowledgeForImage], List(knowledgeForTable2)))
    )

    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(List.empty[KnowledgeForParser], List.empty[PropositionRelation], knowledgeList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSentenceSetForParser, transversalState), transversalState)

    val result:Neo4jRecords =TestUtilsEx.executeQueryAndReturn("""MATCH x = (:TableNode{url:'http://xxx/yyy.tsv'})-[:TableEdge]->(:ClaimNode{surface:'表１です。'}) RETURN x""", transversalState)
    assert(result.records.size == 1)

    val result2: Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:TableNode{url:'http://xxx/zzz.tsv'})-[:TableEdge]->(:ClaimNode{surface:'表２です。'}) RETURN x""", transversalState)
    assert(result2.records.size == 1)
  }

  "The list of local premise and claim images" should "be properly registered in the knowledge database and searchable." in {
    val reference1 = Reference(url = "http://xxx/yyy.tsv", surface = "表１です。", surfaceIndex = 1, isWholeSentence = false, originalUrlOrReference = "")
    val referenceTable1 = TableReference(reference = reference1)
    val featureId1 = UUID.random.toString
    val knowledgeForTable1 = KnowledgeForTable(featureId1, referenceTable1)

    val reference2 = Reference(url = "http://xxx/zzz.tsv", surface = "表２です。", surfaceIndex = 1, isWholeSentence = false, originalUrlOrReference = "")
    val referenceTable2 = TableReference(reference = reference2)
    val featureId2 = UUID.random.toString
    val knowledgeForTable2 = KnowledgeForTable(featureId2, referenceTable2)


    val knowledgePremiseList = List(
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("これは表１です。", "ja_JP", "{}", false, List.empty[KnowledgeForImage], List(knowledgeForTable1))),
    )

    val knowledgeClaimList = List(
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("これは表２です。", "ja_JP", "{}", false, List.empty[KnowledgeForImage], List(knowledgeForTable2)))
    )
    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(knowledgePremiseList, List.empty[PropositionRelation], knowledgeClaimList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSentenceSetForParser, transversalState), transversalState)

    val result: Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:TableNode{url:'http://xxx/yyy.tsv'})-[:TableEdge]->(:PremiseNode{surface:'表１です。'}) RETURN x""", transversalState)
    assert(result.records.size == 1)

    val result2: Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:TableNode{url:'http://xxx/zzz.tsv'})-[:TableEdge]->(:ClaimNode{surface:'表２です。'}) RETURN x""", transversalState)
    assert(result2.records.size == 1)

  }

  "The list of semi-global claim images" should "be properly registered in the knowledge database and searchable." in {

    val reference1 = Reference(url = "http://xxx/yyy.tsv", surface = "", surfaceIndex = 1, isWholeSentence = true, originalUrlOrReference = "")
    val referenceTable1 = TableReference(reference = reference1)
    val featureId1 = UUID.random.toString
    val knowledgeForTable1 = KnowledgeForTable(featureId1, referenceTable1)

    val reference2 = Reference(url = "http://xxx/zzz.tsv", surface = "", surfaceIndex = 1, isWholeSentence = true, originalUrlOrReference = "")
    val referenceTable2 = TableReference(reference = reference2)
    val featureId2 = UUID.random.toString
    val knowledgeForTable2 = KnowledgeForTable(featureId2, referenceTable2)


    val knowledgeList = List(
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("これは表１です。", "ja_JP", "{}", false, List.empty[KnowledgeForImage], List(knowledgeForTable1))),
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("これは表２です。", "ja_JP", "{}", false, List.empty[KnowledgeForImage], List(knowledgeForTable2)))
    )

    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(List.empty[KnowledgeForParser], List.empty[PropositionRelation], knowledgeList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSentenceSetForParser, transversalState), transversalState)

    val result: Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:TableNode{url:'http://xxx/yyy.tsv'})-[:TableEdge]->(:SemiGlobalClaimNode{sentence:'これは表１です。'}) RETURN x""", transversalState)
    assert(result.records.size == 1)

    val result2: Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:TableNode{url:'http://xxx/zzz.tsv'})-[:TableEdge]->(:SemiGlobalClaimNode{sentence:'これは表２です。'}) RETURN x""", transversalState)
    assert(result2.records.size == 1)

  }

  "The list of semi-global premise and claim images" should "be properly registered in the knowledge database and searchable." in {

    val reference1 = Reference(url = "http://xxx/yyy.tsv", surface = "", surfaceIndex = 1, isWholeSentence = true, originalUrlOrReference = "")
    val referenceTable1 = TableReference(reference = reference1)
    val featureId1 = UUID.random.toString
    val knowledgeForTable1 = KnowledgeForTable(featureId1, referenceTable1)

    val reference2 = Reference(url = "http://xxx/zzz.tsv", surface = "", surfaceIndex = 1, isWholeSentence = true, originalUrlOrReference = "")
    val referenceTable2 = TableReference(reference = reference2)
    val featureId2 = UUID.random.toString
    val knowledgeForTable2 = KnowledgeForTable(featureId2, referenceTable2)


    val knowledgePremiseList = List(
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("これは表１です。", "ja_JP", "{}", false, List.empty[KnowledgeForImage], List(knowledgeForTable1))),
    )

    val knowledgeClaimList = List(
      KnowledgeForParser(UUID.random.toString, UUID.random.toString, Knowledge("これは表２です。", "ja_JP", "{}", false, List.empty[KnowledgeForImage], List(knowledgeForTable2)))
    )
    val knowledgeSentenceSetForParser = KnowledgeSentenceSetForParser(knowledgePremiseList, List.empty[PropositionRelation], knowledgeClaimList, List.empty[PropositionRelation])
    Sentence2Neo4jTransformer.createGraph(getAnalyzedPropositionSet(knowledgeSentenceSetForParser, transversalState), transversalState)


    val result: Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:TableNode{url:'http://xxx/yyy.tsv'})-[:TableEdge]->(:SemiGlobalPremiseNode{sentence:'これは表１です。'}) RETURN x""", transversalState)
    assert(result.records.size == 1)

    val result2: Neo4jRecords = TestUtilsEx.executeQueryAndReturn("""MATCH x = (:TableNode{url:'http://xxx/zzz.tsv'})-[:TableEdge]->(:SemiGlobalClaimNode{sentence:'これは表２です。'}) RETURN x""", transversalState)
    assert(result2.records.size == 1)

  }
 
}
