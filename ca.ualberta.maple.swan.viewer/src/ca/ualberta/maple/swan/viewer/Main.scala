/*
 * This source file is part fo the SWAN open-source project.
 *
 * Copyright (c) 2021 the SWAN project authors.
 * Licensed under Apache License v2.0
 *
 * See https://github.com/themaplelab/swan/LICENSE.txt for license information.
 *
 */

package ca.ualberta.maple.swan.viewer

import java.io.{File, FileWriter}

import ca.ualberta.maple.swan.ir.canonical.SWIRLPass
import ca.ualberta.maple.swan.ir.raw.SWIRLGen
import ca.ualberta.maple.swan.ir.{SWIRLPrinter, SWIRLPrinterOptions}
import ca.ualberta.maple.swan.parser.{SILParser, SILPrinterOptions}

object Main {
  def main(args: Array[String]): Unit = {
    if (args.length != 1) {
      throw new RuntimeException("Expected 1 argument: the SIL file path")
    }
    val silPathString = args(0)

    val silFile = new File(silPathString)
    if (!silFile.exists()) {
      throw new RuntimeException("Given SIL file does not exist")
    }

    val silParser = new SILParser(silFile.toPath)
    val silModule = silParser.parseModule()
    val silPrintedText = silParser.print(silModule, new SILPrinterOptions().printLocation(false))

    // Overwrite existing SIL with printed SIL
    val silResultFile = new FileWriter(silPathString, false)
    silResultFile.write(silPrintedText)
    silResultFile.close()

    val swirlModule = SWIRLGen.translateSILModule(silModule)
    val canSwirlModule = SWIRLPass.runPasses(swirlModule)
    val swirlPrinter = new SWIRLPrinter()
    val swirlPrintedText = swirlPrinter.print(canSwirlModule,
      new SWIRLPrinterOptions().printLocation(false).printCFGWhenCanonical(false))

    val swirlResultFile = new FileWriter(silPathString + ".swirl")
    swirlResultFile.write(swirlPrintedText)
    swirlResultFile.close()

    val locationFile = new FileWriter(silPathString + ".loc")

    def locToString(loc: Option[(Int, Int)]): String = {
      if (loc.nonEmpty) {
        s"${loc.get._1}:${loc.get._2}"
      } else {
        ""
      }
    }

    silParser.silLocMap.foreach(item => {
      val swiftLoc = silParser.swiftLocMap.get(item._1)
      val silLoc = silParser.silLocMap(item._1)
      val swirlObject = swirlModule.silMap.get(item._1)
      val swirlLoc = if (swirlObject.nonEmpty) swirlPrinter.locMap.get(swirlObject.get) else None
      locationFile.write(
        s"${locToString(swiftLoc)}," +
        s"${locToString(Some(silLoc))},${locToString(swirlLoc)}\n")
    })

    locationFile.close()
  }
}
