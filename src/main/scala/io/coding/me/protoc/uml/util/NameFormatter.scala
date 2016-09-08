package io.coding.me.protoc.uml.util

import java.lang.Character

/** Simple functionality to convert a name string into different case formats. */
object NameFormatter {

  /** Separator chars used to create different cases. */
  private val separator = Set('-', '_')

  def toCamelCase(s: String) = {

    @scala.annotation.tailrec
    def internal(chars: List[Char], firstCharacter: Boolean, prevSeparator: Boolean, acc: List[Char]): List[Char] = chars match {

      case x :: cx if separator(x)                    => internal(cx, firstCharacter, true, acc)
      case x :: cx if prevSeparator || firstCharacter => internal(cx, false, false, acc :+ Character.toUpperCase(x))
      case x :: cx                                    => internal(cx, false, false, acc :+ x)
      case _                                          => acc
    }

    internal(s.toList, true, false, Nil).mkString
  }

  implicit class ImplicitNameFormatter(s: String) {

    lazy val toCamelCase: String = NameFormatter.toCamelCase(s)
  }
}
