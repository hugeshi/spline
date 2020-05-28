package za.co.absa.spline.consumer.service.repo

import za.co.absa.spline.consumer.service.model.sparkjob.{OutputTable, SparkJobIOs}

import scala.concurrent.{ExecutionContext, Future}

trait SparkJobIOsRepository {
  def findJobIOs(appId: String)(implicit ex: ExecutionContext): Future[SparkJobIOs]

  def findJobOutputTables(appId: String)(implicit ex: ExecutionContext): Future[Array[OutputTable]]
}
