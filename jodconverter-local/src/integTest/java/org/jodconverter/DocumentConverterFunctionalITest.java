/*
 * Copyright 2004 - 2012 Mirko Nasato and contributors
 *           2016 - 2020 Simon Braconnier and contributors
 *
 * This file is part of JODConverter - Java OpenDocument Converter.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jodconverter;

import static org.jodconverter.ResourceUtil.documentFile;

import java.io.File;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(LocalOfficeManagerExtension.class)
public class DocumentConverterFunctionalITest {

  private static final int MAX_THREADS = 10;

  @Test
  public void htmlWithImageConversion(@TempDir File testFolder, DocumentConverter converter) {

    final File source = documentFile("index.html");
    final File target = new File(testFolder, "index.pdf");

    // Convert the file to PDF
    converter.convert(source).to(target);
  }

  @Test
  public void testHtmlConversion(@TempDir File testFolder, DocumentConverter converter) {

    final File source = documentFile("test.html");
    final File target = new File(testFolder, "test.pdf");

    // Convert the file to PDF
    converter.convert(source).to(target);
  }

  //  /**
  //   * Test the conversion of all the supported documents format.
  //   *
  //   * @throws Exception if an error occurs.
  //   */
  //  @Test
  //  public void runAllPossibleConversions() throws Exception {
  //
  //    final Thread[] threads = new Thread[MAX_THREADS];
  //    int threadCount = 0;
  //
  //    final AtomicReference<Exception> exception = new AtomicReference<>();
  //    for (final File sourceFile :
  //        Objects.requireNonNull(
  //            new File("src/integTest/resources/documents")
  //                .listFiles((dir, name) -> name.startsWith("test.")))) {
  //
  //      // Convert the file to all supported formats in a separated thread
  //      final Runnable runnable =
  //          () -> {
  //            try {
  //              convertFileToAllSupportedFormats(sourceFile, testFolder);
  //            } catch (Exception ex) {
  //              exception.set(ex);
  //            }
  //          };
  //
  //      // final Runner r = new Runner (source, target, RefreshFilter.CHAIN, converter);
  //      threads[threadCount] = new Thread(runnable);
  //      threads[threadCount++].start();
  //
  //      if (threadCount == MAX_THREADS) {
  //        for (int j = 0; j < threadCount; j++) {
  //          threads[j].join();
  //        }
  //        threadCount = 0;
  //        if (exception.get() != null) {
  //          throw exception.get();
  //        }
  //      }
  //
  //      // convertFileToAllSupportedFormats(sourceFile, testFolder);
  //    }
  //
  //    // Wait for remaining threads.
  //    for (int j = 0; j < threadCount; j++) {
  //      threads[j].join();
  //    }
  //    if (exception.get() != null) {
  //      throw exception.get();
  //    }
  //  }

  /** Test the conversion of all the supported documents format. */
  @Test
  public void runAllPossibleConversionsSingleThread(
      @TempDir File testFolder, DocumentConverter converter) {

    for (final File sourceFile :
        Objects.requireNonNull(
            new File("src/integTest/resources/documents")
                .listFiles((dir, name) -> name.startsWith("test.")))) {
      ConvertUtil.convertFileToAllSupportedFormats(sourceFile, testFolder, converter);
    }
  }
}
