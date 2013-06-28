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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import junit.framework.JUnit4TestAdapter;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class WikiCleanBasicTest {

  @Test
  public void testScrewyRefs() {
    String s = "Mutualism has been retrospectively characterised as ideologically situated between individualist and collectivist forms of anarchism.&lt;ref&gt;Avrich, Paul. ''Anarchist Voices: An Oral History of Anarchism in America'', Princeton University Press 1996 ISBN 0-691-04494-5, p.6&lt;br /&gt;''Blackwell Encyclopaedia of Political Thought'', Blackwell Publishing 1991 ISBN 0-631-17944-5, p. 11.&lt;/ref&gt; Proudhon first characterised his goal as a &quot;third form of society, the synthesis of communism and property.&quot;&lt;ref&gt;Pierre-Joseph Proudhon. ''What Is Property?'' Princeton, MA: Benjamin R. Tucker, 1876. p. 281.&lt;/ref&gt;";

    WikiClean cleaner = new WikiCleanBuilder().build();
    assertEquals("Mutualism has been retrospectively characterised as ideologically situated between individualist and collectivist forms of anarchism. Proudhon first characterised his goal as a &quot;third form of society, the synthesis of communism and property.&quot;",
        cleaner.removeRefs(s));
  }

  @Test
  public void testRemoveImageCaption() throws Exception {
    assertEquals("abc", WikiClean.ImageCaptionsRemover.remove("[[File: blah blah]]abc"));
    assertEquals("abc", WikiClean.ImageCaptionsRemover.remove("abc[[File: blah blah]]"));
    assertEquals("", WikiClean.ImageCaptionsRemover.remove("[[File: blah blah]]"));
    assertEquals("abcdef", WikiClean.ImageCaptionsRemover.remove("abc[[File: blah blah]]def"));
    assertEquals("abcdef", WikiClean.ImageCaptionsRemover.remove("abc[[File: [ ] [ ] [ [ ] ]]def"));
    assertEquals("abcdef", WikiClean.ImageCaptionsRemover.remove("abc[[File: blah [[nesting]] blah]]def"));
    assertEquals("abcdef", WikiClean.ImageCaptionsRemover.remove("abc[[File: blah [[nesting [[ ]] ]] blah]]def"));
    assertEquals("abcdef", WikiClean.ImageCaptionsRemover.remove("abc[[File: blah [[nesting]] [[blah]]]]def"));

    assertEquals("", WikiClean.ImageCaptionsRemover.remove("[[File: blah[[[[]]]] blah]]"));

    // Unbalanced, removes everything until the end.
    assertEquals("abc", WikiClean.ImageCaptionsRemover.remove("abc[[File: blah [[nesting blah]]def"));

    assertEquals("abcdef", WikiClean.ImageCaptionsRemover.remove("abc[[File: here]][[File: blah blah]]def"));
    assertEquals("abcdef", WikiClean.ImageCaptionsRemover.remove("abc[[File: here]]d[[File: blah blah]]ef"));
    assertEquals("", WikiClean.ImageCaptionsRemover.remove("[[File: here]][[File: blah blah]]"));
    assertEquals("abcdef", WikiClean.ImageCaptionsRemover.remove("abc[[File: [[ blah ]] here]][[File: blah blah]]def"));

    // Sprinkle in non-ASCII characters to make sure everything still works.
    assertEquals("abc政府def", WikiClean.ImageCaptionsRemover.remove("abc[[File: 政府 blah [[nesting]] blah政府]]政府def"));
    assertEquals("abc政府def", WikiClean.ImageCaptionsRemover.remove("abc[[File: blah [[nesting [[政府]] [政府[ ]x] ]] blah]]政府def"));
  }

  @Test
  public void testRemoveDoubleBraces() throws Exception {
    assertEquals("abc", WikiClean.DoubleBracesRemover.remove("{{blah blah}}abc"));
    assertEquals("abc", WikiClean.DoubleBracesRemover.remove("abc{{blah blah}}"));
    assertEquals("", WikiClean.DoubleBracesRemover.remove("{{blah blah}}"));
    assertEquals("abcdef", WikiClean.DoubleBracesRemover.remove("abc{{blah blah}}def"));
    assertEquals("abcdef", WikiClean.DoubleBracesRemover.remove("abc{{{ } { } { } }}def"));
    assertEquals("abcdef", WikiClean.DoubleBracesRemover.remove("abc{{blah {{nesting}} blah}}def"));
    assertEquals("abcdef", WikiClean.DoubleBracesRemover.remove("abc{{blah {{nesting {{ }} }} blah}}def"));
    assertEquals("abcdef", WikiClean.DoubleBracesRemover.remove("abc{{blah {{nesting}} {{blah}}}}def"));

    assertEquals("", WikiClean.DoubleBracesRemover.remove("{{blah{{{{}}}} blah}}"));

    // Unbalanced, removes everything until the end.
    assertEquals("abc", WikiClean.DoubleBracesRemover.remove("abc{{blah {{nesting blah}}def"));

    assertEquals("abcdef", WikiClean.DoubleBracesRemover.remove("abc{{here}}{{blah blah}}def"));
    assertEquals("abcdef", WikiClean.DoubleBracesRemover.remove("abc{{here}}d{{blah blah}}ef"));
    assertEquals("", WikiClean.DoubleBracesRemover.remove("{{here}}{{blah blah}}"));
    assertEquals("abcdef", WikiClean.DoubleBracesRemover.remove("abc{{{{ blah }} here}}{{blah blah}}def"));

    // Sprinkle in non-ASCII characters to make sure everything still works.
    assertEquals("abc政府def", WikiClean.DoubleBracesRemover.remove("abc{{政府 blah {{nesting}} blah政府}}政府def"));
    assertEquals("abc政府def", WikiClean.DoubleBracesRemover.remove("abc{{blah {{nesting {{政府}} [政府[ ]x] }} blah}}政府def"));
  }

  @Test
  public void testRemoveTables() throws Exception {
    assertEquals("abc", WikiClean.TableRemover.remove("{|blah blah|}abc"));
    assertEquals("abc", WikiClean.TableRemover.remove("abc{|blah blah|}"));
    assertEquals("", WikiClean.TableRemover.remove("{|blah blah|}"));
    assertEquals("abcdef", WikiClean.TableRemover.remove("abc{|blah blah|}def"));
    assertEquals("abcdef", WikiClean.TableRemover.remove("abc{|| | | | | | |}def"));
    assertEquals("abcdef", WikiClean.TableRemover.remove("abc{|blah {|nesting|} blah|}def"));
    assertEquals("abcdef", WikiClean.TableRemover.remove("abc{|blah {|nesting {| | | |} |} blah|}def"));
    assertEquals("abcdef", WikiClean.TableRemover.remove("abc{|blah {|nesting|} {|blah|}|}def"));

    assertEquals("", WikiClean.TableRemover.remove("{|blah{|{||}|} blah|}"));

    // Unbalanced, removes everything until the end.
    assertEquals("abc", WikiClean.TableRemover.remove("abc{|blah {|nesting blah|}def"));

    assertEquals("abcdef", WikiClean.TableRemover.remove("abc{|here|}{|blah blah|}def"));
    assertEquals("abcdef", WikiClean.TableRemover.remove("abc{|here|}d{|blah blah|}ef"));
    assertEquals("", WikiClean.TableRemover.remove("{|here|}{|blah blah|}"));
    assertEquals("abcdef", WikiClean.TableRemover.remove("abc{|{| blah |} here|}{|blah blah|}def"));

    // Sprinkle in non-ASCII characters to make sure everything still works.
    assertEquals("abc政府def", WikiClean.TableRemover.remove("abc{|政府 blah {|nesting|} blah政府|}政府def"));
    assertEquals("abc政府def", WikiClean.TableRemover.remove("abc{|blah {|nesting {|政府|} [政府[ ]x] |} blah|}政府def"));
  }

  @Test
  public void testBuilderOptions() throws Exception {
    String raw = FileUtils.readFileToString(new File("src/test/resources/enwiki-20120104-id12.xml"));
    WikiClean cleaner;
    String content;

    // Keep the footer.
    cleaner = new WikiCleanBuilder().withFooter(true).build();
    content = cleaner.clean(raw);

    assertTrue(content.contains("See also"));
    assertTrue(content.contains("Reference"));
    assertTrue(content.contains("Further reading"));
    assertTrue(content.contains("External links"));

    assertEquals(true, cleaner.getWithFooter());
    assertEquals(false, cleaner.getWithTitle());

    // Explicitly not keep the footer.
    cleaner = new WikiCleanBuilder().withFooter(false).build();
    content = cleaner.clean(raw);

    assertFalse(content.contains("See also"));
    assertFalse(content.contains("Reference"));
    assertFalse(content.contains("Further reading"));
    assertFalse(content.contains("External links"));

    assertEquals(false, cleaner.getWithFooter());
    assertEquals(false, cleaner.getWithTitle());

    // Print the title.
    cleaner = new WikiCleanBuilder().withTitle(true).build();
    content = cleaner.clean(raw);

    assertTrue(content.contains("Anarchism\n\nAnarchism is generally"));

    assertEquals(false, cleaner.getWithFooter());
    assertEquals(true, cleaner.getWithTitle());

    // Explicitly not print the title.
    cleaner = new WikiCleanBuilder().withTitle(false).build();
    content = cleaner.clean(raw);

    assertFalse(content.contains("Anarchism\n\nAnarchism is generally"));

    assertEquals(false, cleaner.getWithFooter());
    assertEquals(false, cleaner.getWithTitle());

    // Keep the footer and title.
    cleaner = new WikiCleanBuilder().withTitle(true).withFooter(true).build();
    content = cleaner.clean(raw);

    assertTrue(content.contains("See also"));
    assertTrue(content.contains("Reference"));
    assertTrue(content.contains("Further reading"));
    assertTrue(content.contains("External links"));
    assertTrue(content.contains("Anarchism\n\nAnarchism is generally"));

    assertEquals(true, cleaner.getWithFooter());
    assertEquals(true, cleaner.getWithTitle());

    // Should be same as the default.
    cleaner = new WikiCleanBuilder().withTitle(false).withFooter(false).build();
    content = cleaner.clean(raw);

    assertFalse(content.contains("See also"));
    assertFalse(content.contains("Reference"));
    assertFalse(content.contains("Further reading"));
    assertFalse(content.contains("External links"));
    assertFalse(content.contains("Anarchism\n\nAnarchism is generally"));

    assertEquals(false, cleaner.getWithFooter());
    assertEquals(false, cleaner.getWithTitle());
  }

  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(WikiCleanBasicTest.class);
  }
}
