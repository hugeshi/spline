package za.co.absa.spline.consumer.service.repo

import com.arangodb.async.ArangoDatabaseAsync
import org.apache.commons.text.similarity.LevenshteinDistance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import za.co.absa.spline.consumer.service.model.bsi.SolutionMetrics
import za.co.absa.spline.consumer.service.model.sparkjob.SparkAppInfo

import scala.concurrent.{ExecutionContext, Future}


@Repository
class BSISolutionRepositoryImpl @Autowired()(db: ArangoDatabaseAsync) extends BSISolutionRepository {

  import za.co.absa.spline.persistence.ArangoImplicits._


  override def findAppNameBySolutionName(solutionName: String)(implicit ec: ExecutionContext): Future[SolutionMetrics] = {
    val allSparkAppInfo = db
      .queryStream[SparkAppInfo](
      """
        |FOR ex IN executionPlan
        |    RETURN {
        |        "appName": ex.extra.appName,
        |        "appId": ex.extra.appId,
        |        "createTime":ex._created
        |    }
        |""".stripMargin
    )
      .map(_.toArray)
    val editDistance = LevenshteinDistance.getDefaultInstance
    allSparkAppInfo.map(infoList => {
      val latestAppInfoList = infoList
        .groupBy(_.appName)
        .map(_._2.maxBy(_.createTime))
        .toList

      val matchedApp = latestAppInfoList
        .map(sparkInfo => {
          val term = sparkInfo.appName.replaceAll("[^a-zA-Z0-9]", "")
          val query = solutionName.replaceAll("[^a-zA-Z0-9]", "")
          val distance = editDistance.apply(term, query)
          (sparkInfo.appName, sparkInfo.appId, distance)
        })
        .minBy(_._3)

      new SolutionMetrics(matchedApp._1, matchedApp._2)
    })
  }
}
