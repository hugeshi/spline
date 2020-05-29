package za.co.absa.spline.consumer.service.repo

import za.co.absa.spline.consumer.service.model.bsi.SolutionMetrics

import scala.concurrent.{ExecutionContext, Future}

trait BSISolutionRepository {
  def findAppNameBySolutionName(solutionName: String)(implicit ec: ExecutionContext): Future[SolutionMetrics]
}
