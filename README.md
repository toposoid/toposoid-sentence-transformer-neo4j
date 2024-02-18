# toposoid-sentence-transformer-neo4j
The main implementation of this module is the conversion of predicate-argument-analyzed sentence structures into a knowledge graph. 
Use Neo4J as the knowledge database.
This library is mainly used by toposoid developer in Toposoid projects.
Toposoid is a knowledge base construction platform.(see [Toposoid　Root Project](https://github.com/toposoid/toposoid.git))

[![Unit Test Action](https://github.com/toposoid/toposoid-sentence-transformer-neo4j/actions/workflows/action.yml/badge.svg?branch=main)](https://github.com/toposoid/toposoid-sentence-transformer-neo4j/actions/workflows/action.yml)

## Requirements
* Scala version 2.13.x,   
* Sbt version 1.9.0
* [KNP 4.19](https://nlp.ist.i.kyoto-u.ac.jp/?KNP)

## Recommended environment For Standalone
* Required: at least 8GB of RAM
* Required: 10G or higher　of HDD
* The following microservices must be running
    * toposoid/toposoid-sentence-parser-japanese-web
    * toposoid/toposoid-common-nlp-japanese-web
    * toposoid/toposoid-sentence-parser-english-web
    * toposoid/toposoid-common-nlp-english-web
    * toposoid/corenlp
    * neo4j

## Setup For Standalone
sbt publishLocal

## Usage
Please refer to the test code
```scala
import com.ideal.linked.toposoid.knowledgebase.regist.model.{Knowledge, PropositionRelation}
import com.ideal.linked.toposoid.protocol.model.parser.{KnowledgeForParser, KnowledgeSentenceSetForParser}
import com.ideal.linked.toposoid.sentence.transformer.neo4j.Sentence2Neo4jTransformer
import io.jvm.uuid.UUID

object JapaneseTest extends App {
  //Japanese Simple Sentence
  val knowledgeList = List(KnowledgeForParser(
    UUID.random.toString,
    UUID.random.toString,
    Knowledge("太郎は映画を見た。", "ja_JP", "{}", false)))
  val knowledgeSentenceSetForParser1 = KnowledgeSentenceSetForParser(
    List.empty[KnowledgeForParser],
    List.empty[PropositionRelation],
    knowledgeList,
    List.empty[PropositionRelation])
  Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSetForParser1)

  //Japanese Multiple Sentence
  val knowledgeList1 = List(
    KnowledgeForParser(
      UUID.random.toString,
      UUID.random.toString,
      Knowledge("Bは黒髪ではない。", "ja_JP", "{}", false)),
    KnowledgeForParser(
      UUID.random.toString,
      UUID.random.toString,
      Knowledge("Cはブロンドではない。", "ja_JP", "{}", false)),
    KnowledgeForParser(
      UUID.random.toString,
      UUID.random.toString,
      Knowledge("Aは黒髪ではない。", "ja_JP", "{}", false))
  )

  val knowledgeList2 = List(
    KnowledgeForParser(
      UUID.random.toString,
      UUID.random.toString,
      Knowledge("Dは黒髪ではない。", "ja_JP", "{}", false)),
    KnowledgeForParser(
      UUID.random.toString,
      UUID.random.toString,
      Knowledge("Eはブロンドではない。", "ja_JP", "{}", false)),
    KnowledgeForParser(
      UUID.random.toString,
      UUID.random.toString,
      Knowledge("Fは黒髪ではない。", "ja_JP", "{}", false))
  )
  val knowledgeSentenceSetForParser2 = KnowledgeSentenceSetForParser(
    knowledgeList1,
    List(PropositionRelation("AND", 0, 1), PropositionRelation("OR", 1, 2)),
    knowledgeList2,
    List(PropositionRelation("OR", 0, 1), PropositionRelation("AND", 1, 2)))
  Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSetForParser2)
}

object EnglishTest extends App {
  //English Simple Sentence
  val knowledgeList = List(KnowledgeForParser(
    UUID.random.toString,
    UUID.random.toString,
    Knowledge("That's life.", "en_US", "{}", false)))
  val knowledgeSentenceSetForParser1 = KnowledgeSentenceSetForParser(
    List.empty[KnowledgeForParser],
    List.empty[PropositionRelation],
    knowledgeList,
    List.empty[PropositionRelation])
  Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSetForParser1)

  //English Multiple Sentence
  val knowledgeList1 = List(
    KnowledgeForParser(
      UUID.random.toString,
      UUID.random.toString,
      Knowledge("A's hair is not black.", "en_US", "{}", false)),
    KnowledgeForParser(
      UUID.random.toString,
      UUID.random.toString,
      Knowledge("B's hair is not blonde", "en_US", "{}", false)),
    KnowledgeForParser(
      UUID.random.toString,
      UUID.random.toString,
      Knowledge("C's hair is not black.", "en_US", "{}", false))
  )
  val knowledgeList2 = List(
    KnowledgeForParser(
      UUID.random.toString,
      UUID.random.toString,
      Knowledge("D's hair is not black.", "en_US", "{}", false)),
    KnowledgeForParser(
      UUID.random.toString,
      UUID.random.toString,
      Knowledge("E's hair is not blonde", "en_US", "{}", false)),
    KnowledgeForParser(
      UUID.random.toString,
      UUID.random.toString,
      Knowledge("F's hair is not black.", "en_US", "{}", false))
  )
  val knowledgeSentenceSetForParser2 = KnowledgeSentenceSetForParser(
    knowledgeList1,
    List(PropositionRelation("AND", 0, 1), PropositionRelation("OR", 1, 2)),
    knowledgeList2,
    List(PropositionRelation("OR", 0, 1), PropositionRelation("AND", 1, 2)))
  Sentence2Neo4jTransformer.createGraph(knowledgeSentenceSetForParser2)
}
```

## Note

## License
toposoid/toposoid-sentence-transformer-neo4j is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0.html).

## Author
* Makoto Kubodera([Linked Ideal LLC.](https://linked-ideal.com/))

Thank you!
