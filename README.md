# toposoid-sentence-transformer-neo4j
The main implementation of this module is the conversion of predicate-argument-analyzed sentence structures into a knowledge graph. 
Use Neo4J as the knowledge database.
This library is mainly used by toposoid developer in Toposoid projects.
Toposoid is a knowledge base construction platform.(see [Toposoid　Root Project](https://github.com/toposoid/toposoid.git))

[![Unit Test Action](https://github.com/toposoid/toposoid-sentence-transformer-neo4j/actions/workflows/action.yml/badge.svg?branch=main)](https://github.com/toposoid/toposoid-sentence-transformer-neo4j/actions/workflows/action.yml)

## Requirements
Scala version 2.12.x,   
Sbt version 1.2.8
[KNP 4.19](https://nlp.ist.i.kyoto-u.ac.jp/?KNP)

## Recommended environment
* Required: at least 8GB of RAM
* Required: 10G or higher　of HDD

## Setup
sbt publishLocal

## Usage
Please refer to the test code
```scala
import com.ideal.linked.toposoid.sentence.transformer.neo4j.Sentence2Neo4jTransformer
//Japanese Pattern1
val knowledgeList = List(Knowledge("太郎は映画を見た。", "ja_JP", "{}", false), Knowledge("花子の趣味はガーデニングです。", "ja_JP" ,"{}", false))
Sentence2Neo4jTransformer.createGraphAuto(knowledgeList)
val id:String = "xxxxxxxxxxxxxxxxxxxxxx"
//Japanese Pattern2
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
Sentence2Neo4jTransformer.createGraph(id, knowledgeSet)

//English Pattern1
val knowledgeList = List(Knowledge("That's life.", "en_US", "{}", false), Knowledge("Seeing is believing.", "en_US" ,"{}", false))
Sentence2Neo4jTransformer.createGraphAuto(knowledgeList)
val id2:String = "yyyyyyyyyyyyyyyyyyyy"

//English Pattern2
val knowledgeSet: KnowledgeSentenceSet = KnowledgeSentenceSet(
  List(Knowledge("A's hair is not black.", "en_US", "{}", false),
    Knowledge("B's hair is not blonde", "en_US", "{}", false),
    Knowledge("C's hair is not black.", "en_US", "{}", false)),
  List(PropositionRelation("AND", 0, 1), PropositionRelation("OR", 1, 2)),
  List(Knowledge("D's hair is not black.", "en_US", "{}", false),
    Knowledge("E's hair is not blonde", "en_US", "{}", false),
    Knowledge("F's hair is not black.", "en_US", "{}", false)),
  List(PropositionRelation("OR", 0, 1), PropositionRelation("AND", 1, 2))
)
Sentence2Neo4jTransformer.createGraph(id2, knowledgeSet)
```

## Note

## License
toposoid/toposoid-sentence-transformer-neo4j is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0.html).

## Author
* Makoto Kubodera([Linked Ideal LLC.](https://linked-ideal.com/))

Thank you!
