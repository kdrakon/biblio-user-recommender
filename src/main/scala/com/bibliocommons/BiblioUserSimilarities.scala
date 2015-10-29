package com.bibliocommons

import com.bibliocommons.JobLogger.logger
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
    val userIdMap = sc.broadcast(ratings.map(line => line.split(",")(1)).distinct().zipWithUniqueId().collectAsMap())
    val numberOfUsers = userIdMap.value.size * 2
    logger.info(s"$numberOfUsers users found")

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

      Vectors.sparse(numberOfUsers, userRatingPairs)
    })

    logger.info("Starting similarity computation...")
    val ratingsMatrix = new RowMatrix(rowVectors)
    val similarityMatrix = ratingsMatrix.columnSimilarities(0.4)

    logger.info("Similarity computation complete...")
    logger.info(similarityMatrix.numRows())
    logger.info(similarityMatrix.numCols())

    similarityMatrix.entries.take(100).foreach(matrixEntry => {
      val user1 = matrixEntry.i
      val user2 = matrixEntry.j
      val sim = matrixEntry.value
      logger.info(s"User $user1 is similar to $user2 by $sim")
    })
  }
}
