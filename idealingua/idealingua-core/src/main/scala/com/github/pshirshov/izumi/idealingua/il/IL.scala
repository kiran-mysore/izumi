package com.github.pshirshov.izumi.idealingua.il

import com.github.pshirshov.izumi.idealingua.model.il.{DomainId, FinalDefinition, Service}

object IL {

  sealed trait Val

  case class ILDomainId(v: DomainId) extends Val

  case class ILService(v: Service) extends Val
  case class ILInclude(i: String) extends Val

  case class ILDef(v: FinalDefinition) extends Val

}
