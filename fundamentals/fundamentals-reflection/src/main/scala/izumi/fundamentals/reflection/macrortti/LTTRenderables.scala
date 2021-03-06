package izumi.fundamentals.reflection.macrortti

import izumi.functional.{Renderable, WithRenderableSyntax}
import izumi.fundamentals.platform.language.unused
import izumi.fundamentals.reflection.macrortti.LightTypeTagRef.SymName.SymLiteral
import izumi.fundamentals.reflection.macrortti.LightTypeTagRef._

trait LTTRenderables extends WithRenderableSyntax {

  def r_SymName(sym: SymName, hasPrefix: Boolean): String

  implicit lazy val r_LightTypeTag: Renderable[LightTypeTagRef] = {
    case a: AbstractReference =>
      a.render()
  }

  implicit lazy val r_AbstractReference: Renderable[AbstractReference] = {
    case a: AppliedReference =>
      a.render()
    case l: Lambda =>
      l.render()
  }

  implicit lazy val r_AppliedReference: Renderable[AppliedReference] = {
    case a: AppliedNamedReference =>
      a.render()
    case i: IntersectionReference =>
      i.render()
    case r: Refinement =>
      r.render()
  }

  implicit lazy val r_Refinement: Renderable[Refinement] = (value: Refinement) => {
    s"(${value.reference.render()} & ${value.decls.map(_.render()).toSeq.sorted.mkString("{", ", ", "}")})"
  }

  implicit lazy val r_RefinementDecl: Renderable[RefinementDecl] = {
    case RefinementDecl.Signature(name, input, output) =>
      s"def $name${input.map(_.render()).mkString("(", ", ", ")")}: ${output.render()}"
    case RefinementDecl.TypeMember(name, tpe) =>
      s"type $name = $tpe"
  }

  implicit lazy val r_AppliedNamedReference: Renderable[AppliedNamedReference] = {
    case n: NameReference =>
      n.render()
    case f: FullReference =>
      f.render()
  }

  implicit lazy val r_Lambda: Renderable[Lambda] = (value: Lambda) => {
    s"λ ${value.input.map(_.render()).mkString(",")} → ${value.output.render()}"
  }

  implicit lazy val r_LambdaParameter: Renderable[LambdaParameter] = (value: LambdaParameter) => {
    s"%${value.name}"
  }

  implicit lazy val r_NameRefRenderer: Renderable[NameReference] = (value: NameReference) => {
    val r = r_SymName(value.ref, value.prefix.isDefined)

    val rr = value.boundaries match {
      case _: Boundaries.Defined =>
        s"$r|${value.boundaries.render()}"
      case Boundaries.Empty =>
        r
    }

    value.prefix match {
      case Some(p) =>
        s"${p.render()}::$rr"
      case None =>
        rr
    }
  }

  implicit lazy val r_FullReference: Renderable[FullReference] = (value: FullReference) => {
    s"${value.asName.render()}${value.parameters.map(_.render()).mkString("[", ",", "]")}"
  }

  implicit lazy val r_IntersectionReference: Renderable[IntersectionReference] = (value: IntersectionReference) => {
    value.refs.map(_.render()).mkString("{", " & ", "}")
  }

  implicit lazy val r_TypeParam: Renderable[TypeParam] = (value: TypeParam) => {
    s"${value.variance.render()}${value.ref}"
  }

  implicit lazy val r_Variance: Renderable[Variance] = {
    case Variance.Invariant => "="
    case Variance.Contravariant => "-"
    case Variance.Covariant => "+"
  }

  implicit lazy val r_Boundaries: Renderable[Boundaries] = {
    case Boundaries.Defined(bottom, top) =>
      s"<${bottom.render()}..${top.render()}>"

    case Boundaries.Empty =>
      ""
  }

}

object LTTRenderables {

  object Short extends LTTRenderables {
    def r_SymName(sym: SymName, @unused hasPrefix: Boolean): String = {
      sym match {
        case SymLiteral(c) => c
        case _ => sym.name.split('.').last
      }
    }
  }

  object Long extends LTTRenderables {
    def r_SymName(sym: SymName, @unused hasPrefix: Boolean): String = {
      sym.name
    }
  }
}
