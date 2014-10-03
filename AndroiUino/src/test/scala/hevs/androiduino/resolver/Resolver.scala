package hevs.androiduino.resolver

import hevs.androiduino.apps.TestGeneratorApp
import hevs.androiduino.dsl.components.ComponentManager
import hevs.androiduino.dsl.components.core.Constant
import hevs.androiduino.dsl.components.digital.{DigitalInput, DigitalOutput}
import hevs.androiduino.dsl.components.fundamentals.uint1
import hevs.androiduino.dsl.generator.{Resolver, CodeGenerator, DotGenerator}

object ResolverCode {

  // Inputs
  val btn1 = DigitalInput(4)
  val cst1 = Constant(uint1(true))

  // Outputs
  val led1 = DigitalOutput(7)
  val led2 = DigitalOutput(8)
  val led3 = DigitalOutput(9)

  // Connecting stuff
  btn1.out --> led1.in
  btn1.out --> led2.in

  cst1.out --> led3.in
}

object ResolverTest extends TestGeneratorApp {

  val source = ResolverCode // The the main code

  CodeGenerator.printWarnings()
  Resolver.resolveCode()

  // Generate the C code and the DOT graph
  //val code = CodeGenerator.generateCodeFile(fileName, fileName)
  //val dot = DotGenerator.generateDotFile(ComponentManager.cpGraph, fileName, fileName)

  // Print code and dot as result
  //println(code)
  //println("\n***\n")
  //println(dot)
}