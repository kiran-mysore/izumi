package com.github.pshirshov.izumi.idealingua.model.typespace

import com.github.pshirshov.izumi.idealingua.model.common.TypeId.{DTOId, InterfaceId}
import com.github.pshirshov.izumi.idealingua.model.common.{SigParam, StructureId, TypeId}
import com.github.pshirshov.izumi.idealingua.model.typespace.structures.ConverterDef

class TypespaceToolsImpl(types: TypeCollection) extends TypespaceTools {
  def idToParaName(id: TypeId): String = id.name.toLowerCase

  def mkConverter(innerFields: List[SigParam], outerFields: List[SigParam], targetId: StructureId): ConverterDef = {
    val allFields = innerFields ++ outerFields
    val outerParams = outerFields.map(_.source).distinct

    assert(innerFields.groupBy(_.targetFieldName).forall(_._2.size == 1), s"$targetId: Contradictive inner fields: ${innerFields.mkString("\n  ")}")
    assert(outerFields.groupBy(_.targetFieldName).forall(_._2.size == 1), s"$targetId: Contradictive outer fields: ${outerFields.mkString("\n  ")}")
    assert(allFields.groupBy(_.targetFieldName).forall(_._2.size == 1), s"$targetId: Contradictive fields: ${allFields.mkString("\n  ")}")
    assert(outerParams.groupBy(_.sourceName).forall(_._2.size == 1), s"$targetId: Contradictive outer params: ${outerParams.mkString("\n  ")}")

    // TODO: pass definition instead of id
    ConverterDef(
      targetId
      , allFields
      , outerParams
    )
  }

  override def implId(id: InterfaceId): DTOId = {
    DTOId(id, types.toDtoName(id))
  }

  override def defnId(id: StructureId): InterfaceId = {
    id match {
      case d: DTOId if types.isInterfaceEphemeral(d) =>
        types.interfaceEphemeralsReversed(d)

      case d: DTOId =>
        InterfaceId(id, types.toInterfaceName(id))

      case i: InterfaceId =>
        i
    }
  }
}
