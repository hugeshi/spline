package za.co.absa.spline.consumer.service.repo

import java.util.UUID

import com.arangodb.async.ArangoDatabaseAsync
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import za.co.absa.spline.consumer.service.bsi.BSIVariableDependencyResolver
import za.co.absa.spline.consumer.service.internal.model.OperationWithSchema
import za.co.absa.spline.consumer.service.model.sparkjob.AttributesMapping
import za.co.absa.spline.persistence.model.{Read, Write}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

@Repository
class SolutionLineageRepositoryImpl @Autowired()(db: ArangoDatabaseAsync, executionPlanRepository: ExecutionPlanRepository) extends SolutionLineageRepository {

  val logger = LoggerFactory.getLogger(classOf[SolutionLineageRepositoryImpl])

  import za.co.absa.spline.persistence.ArangoImplicits._

  override def findVariableDependencies(attr: String, applicationId: String)(implicit ec: ExecutionContext): mutable.Map[String, List[String]] = {
    val variableRelatedOperation = db
      .queryOne[Map[String, String]](
      """
        |for event in progress
        |    SORT event.timestamp desc
        |    filter event.extra.appId == @applicationId
        |    for v in 2 outbound event progressOf,executes
        |        filter @variableName in v.params.table.schema[*].name
        |        LIMIT 1
        |        let variableMap = zip(v.params.table.schema[*].name,v.outputSchema)
        |        LET executeID = PARSE_IDENTIFIER(v._id)
        |        return {
        |            opId:executeID.key,
        |            attrId:variableMap[@variableName]
        |        }
      """.stripMargin,
      Map("applicationId" -> applicationId, "variableName" -> attr)
    )
    val op = Await.result(variableRelatedOperation, 1 seconds)
    val operationId = op.getOrElse("opId", "")
    val executionId = operationId.split(":")(0)
    val attributeId = op.getOrElse("attrId", "")

    val attributesDependencies = new mutable.ListBuffer[mutable.Map[String, List[String]]]
    resolveAttributesDependency(
      UUID.fromString(executionId),
      List(UUID.fromString(attributeId)),
      attributesDependencies)
    mergeMaps((a: List[String], b: List[String]) => (a union b).distinct)(attributesDependencies.toList)
  }

  def mergeMaps[A, B](collisionFunc: (B, B) => B)(listOfMaps: List[mutable.Map[A, B]]): mutable.Map[A, B] = {
    listOfMaps.foldLeft(mutable.Map.empty[A, B]) { (m, s) =>
      for (k <- s.keys) {
        if (m contains k)
          m(k) = collisionFunc(m(k), s(k))
        else
          m(k) = s(k)
      }
      m
    }
  }

  /**
    * @param executionId
    * @param attributeIds
    * @param attributesDependencies
    * @param ec
    */
  def resolveAttributesDependency(executionId: UUID,
                                  attributeIds: List[UUID],
                                  attributesDependencies: mutable.ListBuffer[mutable.Map[String, List[String]]])(
                                   implicit ec: ExecutionContext): Unit = {
    val operationsCompleted = Await.result(findOperationsWithSchema(executionId), 1 seconds)
    val candidateReadsOperationsCompleted = Await.result(fetchCandidateReads(executionId.toString), 1 seconds)
    for (attributeId <- attributeIds) {
      val attributesDependency = BSIVariableDependencyResolver.resolveDependencies(operationsCompleted, attributeId)
      val matchedOperations = candidateReadsOperationsCompleted intersect attributesDependency.operations
      for (matchedOperation <- matchedOperations) {
        val op = Await.result(getOpertionByKey(matchedOperation), 1 seconds)
        val rds = Await.result(getReadDataSource(op._key), 1 seconds)
        val wop = Await.result(getWriteOperation(rds.getOrElse("rdsId", "")), 1 seconds)
        val execId = op._key.split(":")(0)


        val matchedAttributes = op.outputSchema.map(x => {
          x.asInstanceOf[java.util.List[String]].asScala.filter(attr => attributesDependency.attributes.contains(UUID.fromString(attr)))
        }).orNull

        if (matchedAttributes != null && matchedAttributes.length > 0) {
          if (wop == null) {
            val rdsUri = rds.getOrElse("rdsUri", "")
            val attributesMapping = Await.result(getAttributesMappingFromExecutionPlan(execId), 1 seconds)
            val attributeDependency =
              matchedAttributes
                .map(attrId => attributesMapping.getOrElse(attrId.toString, null))
                .filter(_ != null)

            attributesDependencies.append(mutable.Map(rdsUri -> attributeDependency.toList))
          } else {
            val attributeMapping =
              (op.outputSchema.get.asInstanceOf[java.util.List[String]].asScala zip wop.outputSchema.get
                .asInstanceOf[java.util.List[String]]
                .asScala).toMap
            val mappingIds = matchedAttributes
              .map(attr => attributeMapping.getOrElse(attr.toString, null))
              .filter(_ != null)
              .map(UUID.fromString)
            resolveAttributesDependency(
              UUID.fromString(wop._key.split(":")(0)),
              mappingIds.toList,
              attributesDependencies)
          }
        }
      }
    }
  }


  //  /**
  //    * @param executionId
  //    * @param attributeId
  //    * @param ec
  //    */
  //  def resolveAttributesDependency(executionId: String,
  //                                  attributeId: String)(
  //                                   implicit ec: ExecutionContext):  Array[Map[String,Set[String]]]= {
  //
  //    def aggregateAttributeLineage(executionId: String,attributeId: String,aggregateResult: Array[Map[String,Set[String]]]): Unit = {
  //      val execPlanDAG = Await.result(executionPlanRepository.loadExecutionPlanAsDAG(UUID.fromString(executionId)),1 seconds)
  //      val solver = AttributeDependencySolver(execPlanDAG)
  //      val maybeDependencyResolver = AttributeDependencyResolver.forSystemAndAgent(execPlanDAG.systemInfo, execPlanDAG.agentInfo)
  //      val maybeAttrLineage = maybeDependencyResolver.map(solver.lineage(attributeId,_))
  //
  //      val attributeNodes = maybeAttrLineage.map(attributeGraph =>{
  //        val sources = attributeGraph.edges.map(_.source)
  //        val edges = attributeGraph.edges.filterNot(edge =>sources.contains(edge.target))
  //        edges.map(
  //          edge => attributeGraph.nodes.filter(_._id == edge.target)
  //        ).foldLeft(Set[AttributeNode]()){
  //          case (s,e) => s ++ e
  //        }
  //      }).foldLeft(Set[AttributeNode]()){
  //        case (s,e) => s ++ e
  //      }
  //
  //      attributeNodes.map(attributeNode =>{
  //        val readOperation = Await.result(getOpertionByKey(attributeNode.originOpId).filter(_._type == "Read"),1 seconds)
  //        val attributesMapping = Await.result(getAttributesMappingBetweenTwoExecutionPlan(readOperation._key),1 seconds)
  //        attributesMapping match {
  //            case (mapping:AttributesMapping) => {
  //              val operationID = mapping.writeOp;
  //              val indexId = mapping.readSchema.indexOf(attributeNode._id)
  //              val mappingAttrId = mapping.writeSchema(indexId)
  //              val executionId = operationID.split(":")(0)
  //              aggregateAttributeLineage(executionId,mappingAttrId,aggregateResult)
  //            }
  //            case _ => {
  //              aggregateResult :+ (readOperation.inputSources(0) -> Set(attributeNode._id))
  //            }
  //          }
  //      })
  //
  //    }
  //    val dependencies = Array.empty[Map[String,Set[String]]]
  //    aggregateAttributeLineage(executionId,attributeId,dependencies)
  //    dependencies


  //    val operationsCompleted = Await.result(findOperationsWithSchema(executionId), 1 seconds)
  //    val candidateReadsOperationsCompleted = Await.result(fetchCandidateReads(executionId.toString), 1 seconds)
  //    for (attributeId <- attributeIds) {
  //      val attributesDependency = AttributeDependencySolver.resolveDependencies(operationsCompleted, attributeId)
  //      val matchedOperations = candidateReadsOperationsCompleted intersect attributesDependency.operations
  //      for (matchedOperation <- matchedOperations) {
  //        val op = Await.result(getOpertionByKey(matchedOperation), 1 seconds)
  //        val rds = Await.result(getReadDataSource(op._key), 1 seconds)
  //        val wop = Await.result(getWriteOperation(rds.getOrElse("rdsId", "")), 1 seconds)
  //        val execId = op._key.split(":")(0)
  //
  //        val matchedAttributes = mutable.ListBuffer[UUID]()
  //        for (x <- op.outputSchema.get
  //          .asInstanceOf[java.util.List[String]]) {
  //          if (attributesDependency.attributes.contains(UUID.fromString(x))) {
  //            matchedAttributes.+=(UUID.fromString(x))
  //          }
  //        }
  //        if (matchedAttributes.length > 0) {
  //          if (wop == null) {
  //            val rdsUri = rds.getOrElse("rdsUri", "")
  //            val attributesMapping = Await.result(getAttributesMappingFromExecutionPlan(execId), 1 seconds)
  //            val attributeDependency =
  //              matchedAttributes
  //                .map(attrId => attributesMapping.getOrElse(attrId.toString, null))
  //                .filter(_ != null)
  //
  //            attributesDependencies.append(mutable.Map(rdsUri -> attributeDependency.toList))
  //          } else {
  //            val attributeMapping =
  //              (op.outputSchema.get.asInstanceOf[java.util.List[String]].asScala zip wop.outputSchema.get
  //                .asInstanceOf[java.util.List[String]]
  //                .asScala).toMap
  //            val mappingIds = matchedAttributes
  //              .map(attr => attributeMapping.getOrElse(attr.toString, null))
  //              .filter(_ != null)
  //              .map(UUID.fromString)
  //            resolveAttributesDependency(
  //              UUID.fromString(wop._key.split(":")(0)),
  //              mappingIds.toList,
  //              attributesDependencies)
  //          }
  //        }
  //      }
  //    }
  //  }

  def findOperationsWithSchema(execId: UUID)(
    implicit ec: ExecutionContext): Future[Array[OperationWithSchema]] = {
    db.queryStream[OperationWithSchema](
      """
        LET exec = FIRST(FOR ex IN executionPlan FILTER ex._key == @execId RETURN ex)
        LET writeOp = FIRST(FOR v IN 1 OUTBOUND exec executes RETURN v)
        FOR vi IN 0..9999
        OUTBOUND writeOp follows
        COLLECT v = vi
        LET children = (FOR child IN 1 OUTBOUND v follows RETURN child._key)
        RETURN {
          "_id": v._key,
          "schema": v.outputSchema,
          "extra": v.extra,
          "params": v.params,
          "childIds": children
        }
      """,
      Map("execId" -> execId)
    )
      .map(_.toArray)
  }


  def fetchCandidateReads(executionPlanId: String)(implicit ec: ExecutionContext): Future[List[String]] = {
    db.queryStream[String](
      """
        for v in readsFrom filter CONTAINS(v._from,@executionId) return split(v._from,'/')[1]
        """,
      Map("executionId" -> executionPlanId)
    )
      .map(_.toList)
  }

  def getOpertionByKey(operationId: String)(implicit ec: ExecutionContext): Future[Read] = {
    db.queryOne[Read](
      """
        let matchedOperation = first(for op in operation filter op._key == @operationId return op)
        return matchedOperation
      """,
      Map("operationId" -> operationId)
    )
  }

  def getReadDataSource(operationId: String)(implicit ec: ExecutionContext): Future[Map[String, String]] = {
    db.queryOne[Map[String, String]](
      """
        let op = first(for oper in operation filter oper._key == @operationId return oper)
        let rds = first(for rds in 1 outbound op readsFrom return rds)
        return {rdsId:rds._id,rdsUri:rds.uri}
      """,
      Map("operationId" -> operationId)
    )
  }

  def getWriteOperation(dataSourceId: String)(implicit ec: ExecutionContext): Future[Write] = {
    db.queryOne[Write](
      """
        let writeOp = first(for wop in 1 inbound @dataSourceId writesTo return wop)
        return writeOp
      """,
      Map("dataSourceId" -> dataSourceId)
    )
  }

  def getAttributesMappingFromExecutionPlan(executionPlanKey: String)(
    implicit ec: ExecutionContext): Future[Map[String, String]] = {
    db.queryOne[Map[String, String]](
      """
        let attributesMapping =first(for ep in executionPlan
            filter ep._key == @executionPlanKey
            let exAttributes = ep.extra.attributes
            return zip(exAttributes[*].id, exAttributes[*].name))
        return attributesMapping
      """.stripMargin,
      Map("executionPlanKey" -> executionPlanKey)
    )
  }

  def getAttributesMappingBetweenTwoExecutionPlan(readOperationId: String)(implicit ex: ExecutionContext): Future[AttributesMapping] = {
    db.queryOne[AttributesMapping](
      """
        |let mapping = first(
        |            for op in operation filter op._key == @operationId
        |                for rds in 1..1 outbound op readsFrom
        |                    for wop in 1..1 inbound rds writesTo
        |                        return {
        |                            writeOp:wop._key,
        |                            readSchema:op.outputSchema,
        |                            writeSchema:wop.outputSchema
        |                        }
        |            )
        |return mapping
      """.stripMargin,
      Map("operationId" -> readOperationId)
    )
  }

}
