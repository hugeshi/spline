package za.co.absa.spline.consumer.service.repo

import scala.collection.mutable
import scala.concurrent.ExecutionContext

trait SolutionLineageRepository {
  def findVariableDependencies(attr: String, applicationId: String)(
    implicit ec: ExecutionContext): mutable.Map[String, List[String]]
}
