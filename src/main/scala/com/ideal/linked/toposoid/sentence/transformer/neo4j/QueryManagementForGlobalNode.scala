
package com.ideal.linked.toposoid.sentence.transformer.neo4j

import com.ideal.linked.toposoid.common._
import com.ideal.linked.toposoid.knowledgebase.regist.model.{KnowledgeForDocument,PropositionRelation}
import com.typesafe.scalalogging.LazyLogging
import scala.util.matching.Regex


object QueryManagementForGlobalNode extends LazyLogging{

  val re = "UNION ALL\n$".r
  //val langPatternJP: Regex = "^ja_.*".r
  //val langPatternEN: Regex = "^en_.*".r

  def existGlobalNode(documentId:String, neo4JUtils:Neo4JUtils, transversalState:TransversalState): Boolean ={
    val query = "MATCH x = (:GlobalNode{documentId:'%s'}) RETURN x".format(documentId)
    val result = neo4JUtils.executeQueryAndReturn(query, transversalState)
    result.records.size > 0
  }

  def executeForGlobalNode(knowledgeForDocument:KnowledgeForDocument, neo4JUtils:Neo4JUtils, transversalState:TransversalState): Unit = {

    if(!knowledgeForDocument.id.equals("") && !existGlobalNode(knowledgeForDocument.id, neo4JUtils, transversalState)){
      createQueryForGlobalNode(
        knowledgeForDocument.id,
        knowledgeForDocument.filename,
        knowledgeForDocument.url,
        knowledgeForDocument.titleOfTopPage,
        neo4JUtils: Neo4JUtils,
        transversalState: TransversalState
      )
    }
  }

  private def createQueryForGlobalNode(documentId: String, filename: String, url: String, titleOfTopPage: String, neo4JUtils:Neo4JUtils, transversalState:TransversalState): Unit = {
    val insertScript = new StringBuilder
    val nodeType: String = "GlobalNode"
    insertScript.append("|MERGE (:%s {documentId:'%s', filename:'%s', url:'%s', titleOfTopPage:\"%s\"})\n".format(
      nodeType,
      documentId,
      filename,
      url,
      titleOfTopPage
    ))

    if (insertScript.size != 0) neo4JUtils.executeQuery(re.replaceAllIn(insertScript.toString().stripMargin, ""), transversalState)
    insertScript.clear()
  }



  def createGlobalLogicRelation(sentenceIds: List[String], propositionRelation: PropositionRelation, sentenceType: Int): StringBuilder = {
    val insertScript = new StringBuilder

    val sourceNodeType: String = sentenceType match {
      case -1 => ToposoidUtils.getNodeType(PREMISE.index, SEMIGLOBAL.index, SENTENCE.index)
      case x => ToposoidUtils.getNodeType(x, SEMIGLOBAL.index, SENTENCE.index)
    }
    val destinationNodeType: String = sentenceType match {
      case -1 => ToposoidUtils.getNodeType(CLAIM.index, SEMIGLOBAL.index, SENTENCE.index)
      case x => ToposoidUtils.getNodeType(x, SEMIGLOBAL.index, SENTENCE.index)
    }
    insertScript.append(("|MATCH (s:%s), (d:%s) WHERE (s.semiGlobalNodeId =~'%s.*' AND  d.semiGlobalNodeId =~'%s.*') MERGE (s)-[:SemiGlobalEdge {logicType:'%s'}]->(d) \n").format(
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
