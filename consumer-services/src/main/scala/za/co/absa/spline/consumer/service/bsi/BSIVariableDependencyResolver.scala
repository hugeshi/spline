package za.co.absa.spline.consumer.service.bsi

import java.util.UUID

import za.co.absa.spline.consumer.service.internal.model.OperationWithSchema

import scala.collection.JavaConverters._
import scala.collection.mutable

object BSIVariableDependencyResolver {

  def resolveDependencies(operations: Seq[OperationWithSchema], attributeID: UUID): AttributeDependencies = {

    val inputSchemaResolver = createInputSchemaResolver(operations)

    val attributeDependencies = operations
      .map(resolveDependencies(_, inputSchemaResolver))
      .reduceOption(mergeDependencies)
      .getOrElse(Map.empty)
      .getOrElse(attributeID, Set.empty)

    val operationDependencies = resolveOperationDependencies(operations, attributeDependencies + attributeID)

    AttributeDependencies(attributeDependencies.toSeq, operationDependencies)
  }

  private def resolveDependencies(op: OperationWithSchema,
                                  inputSchemaOf: OperationWithSchema => Array[UUID]): Map[UUID, Set[UUID]] =
    op.extra("name") match {
      case "Project" => resolveExpressionList(op.params("projectList"), op.schema)
      case "Aggregate" => resolveExpressionList(op.params("aggregateExpressions"), op.schema)
      case "SubqueryAlias" => resolveSubqueryAlias(inputSchemaOf(op), op.schema)
      case "Generate" => resolveGenerator(op)
      case "MapPartitions" => resolveMapPartitions(inputSchemaOf(op), op.schema)
      case "SerializeFromObject" => resolveSerializeFromObject(inputSchemaOf(op), op.schema)
      case "DeserializeToObject" => resolveDeserializeToObject(inputSchemaOf(op), op.schema)
      case _ => Map.empty
    }

  private def resolveExpressionList(exprList: Any, schema: Seq[UUID]): Map[UUID, Set[UUID]] =
    asScalaListOfMaps[String, Any](exprList)
      .zip(schema)
      .map { case (expr, attrId) => attrId -> toAttrDependencies(expr) }
      .toMap

  private def resolveSerializeFromObject(inputSchema: Seq[UUID], outputSchema: Seq[UUID]): Map[UUID, Set[UUID]] =
    outputSchema.map { outAtt =>
      outAtt -> inputSchema.toSet[UUID]
    }.toMap

  private def resolveMapPartitions(inputSchema: Seq[UUID], outputSchema: Seq[UUID]): Map[UUID, Set[UUID]] =
    inputSchema
      .zip(outputSchema)
      .map { case (inAtt, outAtt) => outAtt -> Set(inAtt) }
      .toMap

  private def resolveDeserializeToObject(inputSchema: Seq[UUID], outputSchema: Seq[UUID]): Map[UUID, Set[UUID]] =
    outputSchema.map { outAtt =>
      outAtt -> inputSchema.toSet[UUID]
    }.toMap

  private def resolveSubqueryAlias(inputSchema: Seq[UUID], outputSchema: Seq[UUID]): Map[UUID, Set[UUID]] =
    inputSchema
      .zip(outputSchema)
      .map { case (inAtt, outAtt) => outAtt -> Set(inAtt) }
      .toMap

  private def resolveGenerator(op: OperationWithSchema): Map[UUID, Set[UUID]] = {

    val expression = asScalaMap[String, Any](op.params("generator"))
    val dependencies = toAttrDependencies(expression)

    val keyId = asScalaListOfMaps[String, String](op.params("generatorOutput"))
      .head("refId")

    Map(UUID.fromString(keyId) -> dependencies)
  }

  private def toAttrDependencies(expr: mutable.Map[String, Any]): Set[UUID] = expr("_typeHint") match {
    case "model.AttrRef" => Set(UUID.fromString(expr("refId").asInstanceOf[String]))
    case "model.Alias" => toAttrDependencies(asScalaMap[String, Any](expr("child")))
    case _ if expr.contains("children") =>
      asScalaListOfMaps[String, Any](expr("children"))
        .map(toAttrDependencies)
        .reduce(_ union _)
    case _ => Set.empty
  }

  private def mergeDependencies(acc: Map[UUID, Set[UUID]], newDependencies: Map[UUID, Set[UUID]]) =
    newDependencies.foldLeft(acc) {
      case (acc, (newKey, newValue)) =>
        // add old dependencies to the new dependencies when they contain one of old keys
        val addToNewValue = acc.flatMap {
          case (k, v) if newValue(k) => v
          case _ => Nil
        }

        val updatedNewValue = newValue.union(addToNewValue.toSet)

        // add new dependencies to all dependencies that contains the new key
        val updatedAcc = acc.map {
          case (k, v) if v(newKey) => k -> v.union(updatedNewValue)
          case (k, v) => k -> v
        }

        updatedAcc + (newKey -> updatedNewValue)
    }

  private def createInputSchemaResolver(operations: Seq[OperationWithSchema]): OperationWithSchema => Array[UUID] = {

    val operationMap = operations.map(op => op._id -> op).toMap

    (op: OperationWithSchema) => {
      if (op.childIds.isEmpty) {
        Array.empty
      } else {
        val inputOp = operationMap(op.childIds.head)
        inputOp.schema
      }
    }
  }

  private def resolveOperationDependencies(operations: Seq[OperationWithSchema], attributes: Set[UUID]): Seq[String] =
    operations
      .filter(_.schema.exists(attributes(_)))
      .map(_._id)

  private def asScalaMap[K, V](javaMap: Any) =
    javaMap.asInstanceOf[java.util.Map[K, V]].asScala

  private def asScalaListOfMaps[K, V](javaList: Any) =
    javaList.asInstanceOf[java.util.List[java.util.Map[K, V]]].asScala.map(_.asScala)

}
