# biblio-user-recommender

## What is this?
As part of a hackathon at [BiblioCommons Inc.](http://bibliocommons.com), I created an Apache Spark job to compute user similarities using ratings information (i.e. star-ratings on books, movies, music, etc.). Using Neo4j, I then dumped the similarities into a graph for querying and viewing. Below is an example subgraph:
![user graph example](https://github.com/kdrakon/biblio-user-recommender/blob/master/graph.png)

## The Job
You can refer to the Spark job [here](https://github.com/kdrakon/biblio-user-recommender/blob/master/src/main/scala/com/bibliocommons/BiblioUserSimilarities.scala). After doing some re-arranging of the input data - which was simply a text file of triples _(item ids, user ids, ratings)_ - I used the [Spark Machine Learning Library (MLlib)](http://spark.apache.org/docs/latest/mllib-guide.html) to compute the user similarities. Specifically, I used the similarity algorithm described here: https://databricks.com/blog/2014/10/20/efficient-similarity-algorithm-now-in-spark-twitter.html.

Afterwards, I output the data to a Neo4j graph database using [AnormCypher](http://www.anormcypher.org/). I simply stored nodes for every distinct user found and created edges (_similar_ relationships weighted by the similarity computation) between these nodes. To save space, I hard-coded a similarity threshold of "25".

## Results
For the hackathon, I was able to demonstrate the potential of using our ratings data to compute relationships between our users. For BiblioCommons, that could provide users with a lot of benefits, such as providing recommendations for reading materials or people to follow.

## What I wanted to discover by using Spark in the hackathon
- how easy or difficult it would be to write this task in Spark/Scala
- the difficulty of getting Spark up and running
- the memory and computational complexity required by running this task as a Spark job
- the ETL required for pulling data from the data source (a database)
- how the _User x Item_ rating matrix would be created
- the actual time it would take to compute the similarities on the matrix (and could this be done on a single worker node within the hackathon time limits?)
Overall, Spark was a pleasant framework to use. The language API is quite straightforward if you have experience with Scala and the collections library. Spark abstracts the memory management and computational work into a distributed task, but I still have much to learn about the inner workings and configurations of this tool. With a test dataset of __4 million ratings__, it seemed feasible for BiblioCommons to use Spark to compute these similarities in a reasonable amount of time (with the right hardware too).

### Why use Neo4j?
Neo4J was a good candidate to extract quick direct and transitive based relationships. For example, we could write simple queries to answer questions like:
- who is similar to me?
- who is similar to the user who is similar to me? (with n levels of depth)
- for anyone who is similar to me and/or transitively similar to me, what ratings did they give to items that I haven't rated myself?
Using these relationships, we could create things like reading lists, popularity lists, or even do something like [collaborative filtering](https://en.wikipedia.org/wiki/Collaborative_filtering).
