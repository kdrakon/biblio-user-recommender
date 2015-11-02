package com.bibliocommons

import com.bibliocommons.JobLogger.logger
import com.bibliocommons.Neo4JUserGraph.{addSimilarity, addUserNode}
import org.anormcypher.{Cypher, Neo4jREST}
import org.apache.log4j.Logger
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.apache.spark.{SparkConf, SparkContext}

object JobLogger extends Serializable {
  val logger = Logger.getLogger("BiblioUserSimilarities")
}

object BiblioUserSimilarities {

  def main(args: Array[String]): Unit = {

    val conf = new SparkConf().setAppName("User_Similarities")
    val sc = new SparkContext(conf)

    logger.info("Starting file loading...")
    val ratings = sc.textFile("/home/kdrakon/git/biblio-user-recommender/src/main/resources/ratings.csv").cache()

    logger.info("Mapping user ids...")
    val userIds = ratings.map(line => line.split(",")(1)).distinct()
    val userIdZip = userIds.zipWithUniqueId()
    val userIdMap = sc.broadcast(userIdZip.collectAsMap())
    val invertedUserIdMap = sc.broadcast(userIdMap.value.map(_.swap))
    val numberOfUsers = sc.broadcast(userIdZip.values.max().toInt)

    logger.info("Starting grouping of items...")
    val itemGroupedRatings = ratings.groupBy(_.split(",")(0)).cache()

    val rowVectors = itemGroupedRatings.map(itemGroup => {

      val itemRatings = itemGroup._2

      val userRatingPairsWithDupes = itemRatings.map(line => {
        val split = line.split(",")
        val userUniqueId = userIdMap.value(split(1)).toInt
        val rating = split(2).toDouble
        (userUniqueId, rating)
      })

      // hack b/c my input data has duplicate ratings
      val userRatingPairs = userRatingPairsWithDupes.groupBy(pair => pair._1).flatMap(pairGroup => pairGroup._2.take(1)).toSeq

      Vectors.sparse(numberOfUsers.value + 1, userRatingPairs)
    })

    logger.info("Starting similarity computation...")
    val ratingsMatrix = new RowMatrix(rowVectors)
    val similarityMatrix = ratingsMatrix.columnSimilarities(0.4)

    val similarityThreshold = 25.0
    logger.info(s"Graphing similarities with threshold of $similarityThreshold ...")
    similarityMatrix.entries.filter(_.value >= similarityThreshold).foreach(entry => {
      val userA = invertedUserIdMap.value(entry.i)
      val userB = invertedUserIdMap.value(entry.j)
      addUserNode(userA)
      addUserNode(userB)
      addSimilarity(userA, userB, entry.value)
    })
  }
}

object Neo4JUserGraph extends Serializable {

  implicit val connection = Neo4jREST("localhost", 7474, "/db/data/", "neo4j", "default")

  def addUserNode(userId: String): Unit = {
    Cypher(s"CREATE (:USER {identifier:$userId})").execute()
  }

  def addItemNode(itemId: String): Unit = {
    Cypher(s"CREATE (:ITEM {identifier:'$itemId'})").execute()
  }

  def addUserRating(userId: String, itemId: String, rating: Double): Unit = {
    Cypher(s"MATCH (u:USER {identifier:$userId}),(i:ITEM {identifier:'$itemId'}) CREATE UNIQUE (u)-[:RATED {rating:$rating}]->(i)").execute()
  }

  def addSimilarity(userA: String, userB: String, similarity: Double): Unit = {
    Cypher(s"MATCH (a:USER {identifier:$userA}),(b:USER {identifier:$userB}) CREATE (a)-[:SIMILAR {similarity:$similarity}]->(b)").execute()
  }

}
