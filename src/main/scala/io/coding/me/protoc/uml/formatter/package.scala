package io.coding.me.protoc.uml

import io.coding.me.protoc.uml.model._
import io.coding.me.protoc.uml.{config => c}

package object formatter {

  type UMLFormatter = (Iterable[Types.Type], TypeRepository, c.Config) => String

  case class StructuredStringFormatter(lines: Seq[String], level: Int) {

    def add(fx: Iterable[StructuredStringFormatter]): StructuredStringFormatter = fx.foldLeft(this)((formatter, f) => formatter.add(f))
    def add(f: StructuredStringFormatter): StructuredStringFormatter            = f.lines.foldLeft(this)((formatter, line) => formatter.add(line))

    def +(f: StructuredStringFormatter): StructuredStringFormatter             = add(f)
    def ++(fx: Iterable[StructuredStringFormatter]): StructuredStringFormatter = add(fx)

    def add(s: String): StructuredStringFormatter = addIntented(s)

    def indent: StructuredStringFormatter            = copy(level = level + 1)
    def outdent: StructuredStringFormatter           = if (level > 0) copy(level = level - 1) else this
    def newline: StructuredStringFormatter           = copy(lines = lines :+ "")
    def append(value: String, space: Boolean = true) = copy(lines = lines.tail :+ (lines.last + (if (space) " " + value else value)))

    def withIfElse(condition: Boolean)(ifFormatter: StructuredStringFormatter => StructuredStringFormatter,
                                       elseFormatter: StructuredStringFormatter => StructuredStringFormatter) =
      if (condition) ifFormatter(this)
      else elseFormatter(this)

    def withCondition(c: Boolean)(f: StructuredStringFormatter => StructuredStringFormatter) = if (c) f(this) else this
    def withCurlyBrackets(f: StructuredStringFormatter => StructuredStringFormatter) =
      f(append("{", true).indent).outdent.add("}")

    protected def addIntented(s: String): StructuredStringFormatter =
      copy(lines = lines ++ s.split("\n") /*.filter(_.nonEmpty)*/.map(line => (" " * level + line)))

    override lazy val toString = lines.mkString("\n")
  }

  object StructuredStringFormatter extends StructuredStringFormatter(Nil, 0) {

    final def apply: StructuredStringFormatter = StructuredStringFormatter(Nil, 0)
  }
}
