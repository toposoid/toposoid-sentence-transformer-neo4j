
package com.ideal.linked.toposoid.sentence.transformer.neo4j

import com.ideal.linked.toposoid.common.{CLAIM, IMAGE, LOCAL, PREMISE, SEMIGLOBAL, SENTENCE, TABLE, ToposoidUtils, TransversalState, Neo4JUtils}
import com.ideal.linked.toposoid.knowledgebase.model.KnowledgeFeatureReference
import com.ideal.linked.toposoid.knowledgebase.regist.model.{KnowledgeForImage, KnowledgeForTable, PropositionRelation}
import com.ideal.linked.toposoid.sentence.transformer.neo4j.QueryManagementUtils.convertList2JsonForKnowledgeFeatureReference
import com.typesafe.scalalogging.LazyLogging

import scala.Option
import scala.util.matching.Regex


object QueryManagementForSemiGlobalNode extends LazyLogging{

  val re = "UNION ALL\n$".r
  //val langPatternJP: Regex = "^ja_.*".r
  //val langPatternEN: Regex = "^en_.*".r

  def executeForSemiGlobalNode(analyzedPropositionPair: AnalyzedPropositionPair, sentenceType:Int, neo4JUtils:Neo4JUtils, transversalState:TransversalState): Unit = {
    val knowledgeForParser = analyzedPropositionPair.knowledgeForParser
    if(analyzedPropositionPair.analyzedSentenceObjects.analyzedSentenceObjects.size > 0){

      createQueryForSemiGlobalNode(
        knowledgeForParser.propositionId,
        knowledgeForParser.sentenceId,
        knowledgeForParser.knowledge.knowledgeForDocument.id,
        knowledgeForParser.knowledge.sentence,
        sentenceType,
        knowledgeForParser.knowledge.lang,
        knowledgeForParser.knowledge.knowledgeForImages,
        knowledgeForParser.knowledge.knowledgeForTables,
        neo4JUtils: Neo4JUtils,
        transversalState: TransversalState
      )
    }
  }

  private def createQueryForSemiGlobalNode(propositionId: String, sentenceId: String, documentId: String, sentence: String, sentenceType: Int, lang: String, knowledgeForImages: List[KnowledgeForImage], knowledgeForTables: List[KnowledgeForTable], neo4JUtils:Neo4JUtils, transversalState:TransversalState): Unit = {
    val insertScript = new StringBuilder
    val semiGlobalNodeId = sentenceId
    //val localContextForFeature = LocalContextForFeature(lang, Map.empty[String, String])
    //val knowledgeFeatureNode = KnowledgeFeatureNode(semiGlobalNodeId, propositionId, sentenceId, sentence, sentenceType, localContextForFeature)
    val nodeType: String = ToposoidUtils.getNodeType(sentenceType, SEMIGLOBAL.index, SENTENCE.index)
    val knowledgeFeatureReference: String = convertList2JsonForKnowledgeFeatureReference(List.empty[KnowledgeFeatureReference])
    insertScript.append("|MERGE (:%s {sentenceId:'%s', propositionId:'%s', documentId:'%s', sentence:\"%s\", knowledgeFeatureReferences:'%s', lang:'%s'})\n".format(
      nodeType,
      sentenceId,
      propositionId,
      documentId,
      sentence,
      knowledgeFeatureReference,
      lang
    ))

    //Create ImageNode
    knowledgeForImages.filter(x => {
      x.imageReference.reference.isWholeSentence
    }).foldLeft(insertScript) {
      (acc, x) => {
        acc.append(createQueryForImageNode(propositionId, sentenceId, sentenceType, x))
      }
    }

    //Create TableNode
    knowledgeForTables.filter(x => {
      x.tableReference.reference.isWholeSentence
    }).foldLeft(insertScript) {
      (acc, x) => {
        acc.append(createQueryForTableNode(propositionId, sentenceId, sentenceType, x))
      }
    }

    if (insertScript.size != 0) neo4JUtils.executeQuery(re.replaceAllIn(insertScript.toString().stripMargin, ""), transversalState)
    insertScript.clear()
  }

  private def createQueryForImageNode(propositionId:String, sentenceId:String, sentenceType: Int, knowledgeForImage: KnowledgeForImage): StringBuilder = {
    val nodeType: String = ToposoidUtils.getNodeType(sentenceType, SEMIGLOBAL.index, SENTENCE.index)
    val imageNodeType: String = ToposoidUtils.getNodeType(sentenceType, LOCAL.index, IMAGE.index)
    val insertScript = new StringBuilder
    insertScript.append("|MERGE (:%s {featureId:'%s', url:'%s', propositionId:'%s', sentenceId:'%s', source:'%s'})\n".format(imageNodeType, knowledgeForImage.id, knowledgeForImage.imageReference.reference.url, propositionId, sentenceId, knowledgeForImage.imageReference.reference.originalUrlOrReference))
    insertScript.append("|UNION ALL\n")
    insertScript.append("|MATCH (s:%s {featureId: '%s'}), (d:%s {sentenceId: '%s'}) MERGE (s)-[:ImageEdge]->(d)\n".format(imageNodeType, knowledgeForImage.id, nodeType, sentenceId))
    insertScript.append("|UNION ALL\n")
    insertScript
  }

  private def createQueryForTableNode(propositionId: String, sentenceId: String, sentenceType: Int, knowledgeForTable: KnowledgeForTable): StringBuilder = {
    val nodeType: String = ToposoidUtils.getNodeType(sentenceType, SEMIGLOBAL.index, SENTENCE.index)
    val tableNodeType: String = ToposoidUtils.getNodeType(sentenceType, LOCAL.index, TABLE.index)
    val insertScript = new StringBuilder
    insertScript.append("|MERGE (:%s {featureId:'%s', url:'%s', propositionId:'%s', sentenceId:'%s', source:'%s'})\n".format(tableNodeType, knowledgeForTable.id, knowledgeForTable.tableReference.reference.url, propositionId, sentenceId, knowledgeForTable.tableReference.reference.originalUrlOrReference))
    insertScript.append("|UNION ALL\n")
    insertScript.append("|MATCH (s:%s {featureId: '%s'}), (d:%s {sentenceId: '%s'}) MERGE (s)-[:TableEdge]->(d)\n".format(tableNodeType, knowledgeForTable.id, nodeType, sentenceId))
    insertScript.append("|UNION ALL\n")
    insertScript
  }


  def executeForSemiGlobalLogicRelation(sentenceIds: List[String], propositionRelations: List[PropositionRelation], sentenceType: Int): StringBuilder = {
    propositionRelations.foldLeft(new StringBuilder) {
      (acc, x) => {
        acc.append(createSemiGlobalLogicRelation(sentenceIds, x, sentenceType))
      }
    }
  }

  def createSemiGlobalLogicRelation(sentenceIds: List[String], propositionRelation: PropositionRelation, sentenceType: Int): StringBuilder = {
    val insertScript = new StringBuilder

    val sourceNodeType: String = sentenceType match {
      case -1 => ToposoidUtils.getNodeType(PREMISE.index, SEMIGLOBAL.index, SENTENCE.index)
      case x => ToposoidUtils.getNodeType(x, SEMIGLOBAL.index, SENTENCE.index)
    }
    val destinationNodeType: String = sentenceType match {
      case -1 => ToposoidUtils.getNodeType(CLAIM.index, SEMIGLOBAL.index, SENTENCE.index)
      case x => ToposoidUtils.getNodeType(x, SEMIGLOBAL.index, SENTENCE.index)
    }
    insertScript.append(("|MATCH (s:%s), (d:%s) WHERE (s.sentenceId =~'%s.*' AND  d.sentenceId =~'%s.*') MERGE (s)-[:SemiGlobalEdge {logicType:'%s'}]->(d) \n").format(
      sourceNodeType,
      destinationNodeType,
      sentenceIds(propositionRelation.sourceIndex),
      sentenceIds(propositionRelation.destinationIndex),
      propositionRelation.operator,
      "-" //The lang attribute of LogicalEdge is　defined as　'-'.
    ))
    insertScript.append("|UNION ALL\n")
    insertScript
  }
}
