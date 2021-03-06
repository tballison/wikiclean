/**
 * WikiClean: A Java Wikipedia markup to plain text converter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wikiclean;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.process.DocumentPreprocessor;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;
import org.wikiclean.WikiClean.WikiLanguage;

import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

public class DumpEnWikiToParsedSentences {
  private static final class Args {
    @Option(name = "-input", metaVar = "[path]", required = true, usage = "input path")
    String input;

    @Option(name = "-output", metaVar = "[path]", required = true, usage = "output path")
    String output;
  }

  public static void main(String[] argv) throws Exception {
    final Args args = new Args();
    CmdLineParser parser = new CmdLineParser(args, ParserProperties.defaults().withUsageWidth(100));

    try {
      parser.parseArgument(argv);
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
      System.exit(-1);
    }

    PrintWriter writer = new PrintWriter(args.output, "UTF-8");
    WikiClean cleaner = new WikiCleanBuilder()
        .withLanguage(WikiLanguage.EN).withTitle(false)
        .withFooter(false).build();

    WikipediaBz2DumpInputStream stream = new WikipediaBz2DumpInputStream(args.input);
    String page;
    while ((page = stream.readNext()) != null) {
      if ( page.contains("<ns>") && !page.contains("<ns>0</ns>")) {
        continue;
      }

      String s = cleaner.clean(page);
      if (s.startsWith("#REDIRECT")) {
        continue;
      }

      String title = cleaner.getTitle(page).replaceAll("\\n+", " ");
      int cnt = 0;
      Reader reader = new StringReader(s);
      DocumentPreprocessor dp = new DocumentPreprocessor(reader);
      for (List<HasWord> sentence : dp) {
        writer.print(String.format("%s.%04d\t%s\n", title, cnt, Sentence.listToString(sentence)));
        cnt++;
      }
    }
    writer.close();
  }
}
