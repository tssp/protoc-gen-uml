package io.coding.me.protoc.uml

import protocbridge.frontend.PluginFrontend

/** Main entry point when using compiler as protoc plugin */
object Main extends App {

  val plugin   = ProtocUMLGenerator()
  val response = PluginFrontend.runWithInputStream(plugin, System.in)

  System.out.write(response)
}
