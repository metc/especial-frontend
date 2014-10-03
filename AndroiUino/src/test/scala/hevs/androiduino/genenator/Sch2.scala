package hevs.androiduino.genenator

import hevs.androiduino.apps.TestGeneratorApp
import hevs.androiduino.dsl.components.ComponentManager
import hevs.androiduino.dsl.components.core.Constant
import hevs.androiduino.dsl.components.digital.DigitalOutput
import hevs.androiduino.dsl.components.fundamentals.uint1
import hevs.androiduino.dsl.components.logic.And
import hevs.androiduino.dsl.generator.{CodeGenerator, DotGenerator}

object Sch2Code {

  // Inputs
  val cst1 = Constant(uint1(true))
  val cst2 = Constant(uint1(true))

  // Logic
  val and1 = And() // TODO: connect here ?

  // Output
  val led1 = DigitalOutput(7)

  // Connecting stuff
  and1.out --> led1.in
  cst1.out --> and1.in1
  cst2.out --> and1.in2
}

object Sch2Test extends TestGeneratorApp {

  val source = Sch2Code // The the main code

  // Generate the C code and the DOT graph
  val code = CodeGenerator.generateCodeFile(fileName, fileName)
  val dot = DotGenerator.generateDotFile(ComponentManager.cpGraph, fileName, fileName)

  // Print code and dot as result
  println(code)
  println("\n***\n")
  println(dot)
}