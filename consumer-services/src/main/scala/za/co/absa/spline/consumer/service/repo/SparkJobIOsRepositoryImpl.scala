package za.co.absa.spline.consumer.service.repo

import com.arangodb.async.ArangoDatabaseAsync
import org.springframework.beans.factory.annotation.Autowired
import za.co.absa.spline.consumer.service.model.sparkjob.{OutputTable, SparkJobIOs}

import scala.concurrent.{ExecutionContext, Future}

class SparkJobIOsRepositoryImpl @Autowired()(db: ArangoDatabaseAsync) extends SparkJobIOsRepository {

  import za.co.absa.spline.persistence.ArangoImplicits._

  override def findJobIOs(appId: String)(implicit ex: ExecutionContext): Future[Array[SparkJobIOs]] = {
    db.queryOne[SparkJobIOs](
      """
        |let writeops = (for event in progress
        |    filter event.extra.appId == @appId
        |    for ep in 2 outbound event progressOf,executes
        |    return ep)
        |
        |let writes = (for wop in writeops
        |    for ep in 1 outbound wop writesTo
        |    return ep)
        |
        |let reads = (for event in progress
        |    filter event.extra.appId == @appId
        |    for ep in 2 outbound event progressOf,depends
        |    return distinct ep)
        |
        |let inputs = (for r in reads
        |    filter r not in writes
        |    return r.uri)
        |
        |let outputs = (for wop in writeops
        |    return wop.outputSource)
        |
        |return {
        |    inputs:inputs,
        |    outputs:outputs
        |}
      """.stripMargin,
      Map("appId" -> appId)
    )
  }

  override def findJobOutputTables(appId: String)(implicit ex: ExecutionContext): Future[Array[OutputTable]] = {
    db.queryStream[OutputTable](
      """
        |let writes = (for event in progress
        |    filter event.extra.appId == @appId
        |    for ep in 2 outbound event progressOf,executes
        |    return ep)
        |for write in writes
        |    return {
        |             database:write.params.table.identifier.database,
        |             table:write.params.table.identifier.table,
        |             schema: write.params.table.schema[*].name
        |           }
      """.stripMargin,
      Map("appId" -> appId)
    )
      .map(_.toArray)
  }
}
