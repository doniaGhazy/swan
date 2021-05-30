/*
 * Copyright (c) 2021 the SWAN project authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This software has dependencies with other licenses.
 * See https://github.com/themaplelab/swan/doc/LICENSE.md.
 */

import ca.ualberta.maple.swan.ir.Error;
import ca.ualberta.maple.swan.ir.*;
import ca.ualberta.maple.swan.ir.canonical.SWIRLPass;
import ca.ualberta.maple.swan.parser.SILModule;
import ca.ualberta.maple.swan.parser.SILPrinter;
import ca.ualberta.maple.swan.parser.SILPrinterOptions;
import ca.ualberta.maple.swan.spds.analysis.AnalysisType;
import ca.ualberta.maple.swan.spds.analysis.TaintAnalysis;
import ca.ualberta.maple.swan.spds.analysis.TaintAnalysisOptions;
import ca.ualberta.maple.swan.test.TestDriver;
import ca.ualberta.maple.swan.utils.Logging;
import org.junit.jupiter.api.Test;
import scala.collection.mutable.ArrayBuffer;
import scala.collection.mutable.HashSet;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Objects;

public class SPDSTests {

    static TaintAnalysis.Specification spec;

    static {
        HashSet<String> sources = new HashSet<>();
        HashSet<String> sinks = new HashSet<>();
        HashSet<String> sanitizers = new HashSet<>();
        sources.add("playground.source() -> Swift.String");
        sinks.add("playground.sink(sunk: Swift.String) -> ()");
        sanitizers.add("playground.sanitizer(tainted: Swift.String) -> Swift.String");
        spec = new TaintAnalysis.Specification("Testing", sources, sinks, sanitizers);
    }

    @Test
    void testSPDSSIL() throws URISyntaxException {
        File silFile = new File(Objects.requireNonNull(getClass().getClassLoader()
                .getResource("playground/spds/playground.sil")).toURI());
        Logging.printInfo("(SPDS Playground) testSPDSSIL: Testing " + silFile.getName());
        TestDriver.TestDriverOptions options = new TestDriver.TestDriverOptions();
        options.addSILCallBack((SILModule module) -> {
            SILPrinterOptions opts = new SILPrinterOptions();
            String result = new SILPrinter().print(module, opts);
            // Logging.printInfo(result);
            return scala.runtime.BoxedUnit.UNIT;
        });
        options.addRawSWIRLCallBack((Module module) -> {
            SWIRLPrinterOptions opts = new SWIRLPrinterOptions();
            String expected = new SWIRLPrinter().print(module, opts);
            // Logging.printInfo(expected);
            return scala.runtime.BoxedUnit.UNIT;
        });
        options.addCanSWIRLCallBack((CanModule module) -> {
            SWIRLPrinterOptions opts = new SWIRLPrinterOptions();
            String result = new SWIRLPrinter().print(module, opts);
            // Logging.printInfo(result);
            return scala.runtime.BoxedUnit.UNIT;
        });
        ModuleGroup group = TestDriver.run(silFile, options);
        SWIRLPrinterOptions opts = new SWIRLPrinterOptions().genLocationMap(true).printLineNumber(true);
        String result = new SWIRLPrinter().print(group, opts);
        Logging.printInfo(result);
        TaintAnalysisOptions analysisOptions =
                new TaintAnalysisOptions(AnalysisType.Forward$.MODULE$);
        TaintAnalysis analysis = new TaintAnalysis(group, spec, analysisOptions);
        TaintAnalysis.TaintAnalysisResults results = analysis.run();
        Logging.printInfo(results.toString());
    }

    @Test
    void testSPDSSwift() throws URISyntaxException {
        File swiftFile = new File(Objects.requireNonNull(getClass().getClassLoader()
                .getResource("playground/spds/playground.swift")).toURI());
        Logging.printInfo("(SPDS Playground) testSPDSSwift: Testing " + swiftFile.getName());
        File silFile = TestUtils.dumpSILForSwiftFile(swiftFile);
        TestDriver.TestDriverOptions options = new TestDriver.TestDriverOptions();
        options.addSILCallBack((SILModule module) -> {
            SILPrinterOptions opts = new SILPrinterOptions();
            String result = new SILPrinter().print(module, opts);
            // Logging.printInfo(result);
            return scala.runtime.BoxedUnit.UNIT;
        });
        options.addRawSWIRLCallBack((Module module) -> {
            SWIRLPrinterOptions opts = new SWIRLPrinterOptions();
            String expected = new SWIRLPrinter().print(module, opts);
            // Logging.printInfo(expected);
            return scala.runtime.BoxedUnit.UNIT;
        });
        options.addCanSWIRLCallBack((CanModule module) -> {
            SWIRLPrinterOptions opts = new SWIRLPrinterOptions();
            String result = new SWIRLPrinter().print(module, opts);
            // Logging.printInfo(result);
            return scala.runtime.BoxedUnit.UNIT;
        });
        ModuleGroup group = TestDriver.run(silFile, options);
        SWIRLPrinterOptions opts = new SWIRLPrinterOptions().genLocationMap(true).printLineNumber(true);
        String result = new SWIRLPrinter().print(group, opts);
        Logging.printInfo(result);
        TaintAnalysisOptions analysisOptions =
                new TaintAnalysisOptions(AnalysisType.Forward$.MODULE$);
        TaintAnalysis analysis = new TaintAnalysis(group, spec, analysisOptions);
        TaintAnalysis.TaintAnalysisResults results = analysis.run();
        Logging.printInfo(results.toString());
    }

    @Test
    void testSPDSSWIRL() throws URISyntaxException, Error {
        File swirlFile = new File(Objects.requireNonNull(getClass().getClassLoader()
                .getResource("playground/spds/playground.swirl")).toURI());
        Logging.printInfo("(SPDS Playground) testSPDSSWIRL: Testing " + swirlFile.getName());
        Module parsedModule = new SWIRLParser(swirlFile.toPath()).parseModule();
        CanModule canModule = new SWIRLPass().runPasses(parsedModule);
        ArrayBuffer<CanModule> modules = new ArrayBuffer<>();
        modules.append(canModule);
        modules.append(TestDriver.getModelModule());
        ModuleGroup group = ModuleGrouper.group(modules, null, null);
        SWIRLPrinterOptions opts = new SWIRLPrinterOptions().genLocationMap(true).printLineNumber(true);
        String result = new SWIRLPrinter().print(group, opts);
        Logging.printInfo(result);
        TaintAnalysisOptions analysisOptions =
                new TaintAnalysisOptions(AnalysisType.Forward$.MODULE$);
        TaintAnalysis analysis = new TaintAnalysis(group, spec, analysisOptions);
        TaintAnalysis.TaintAnalysisResults results = analysis.run();
        Logging.printInfo(results.toString());
    }
}
