package io.coding.me.protoc.uml

import protocbridge.frontend.PluginFrontend

/** Main entry point when using compiler as protoc plugin */
object Main extends App {

  val plugin = ProtocUMLGenerator()

  val response = PluginFrontend.runWithBytes(plugin, System.in.readAllBytes())

  System.out.write(response)

}
