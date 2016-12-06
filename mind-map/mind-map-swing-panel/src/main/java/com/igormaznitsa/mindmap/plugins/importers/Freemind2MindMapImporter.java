/*
 * Copyright 2016 Igor Maznitsa.
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
package com.igormaznitsa.mindmap.plugins.importers;

import static com.igormaznitsa.mindmap.swing.panel.StandardTopicAttribute.ATTR_FILL_COLOR;
import static com.igormaznitsa.mindmap.swing.panel.StandardTopicAttribute.ATTR_TEXT_COLOR;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import com.igormaznitsa.mindmap.plugins.api.AbstractImporter;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.igormaznitsa.meta.annotation.MustNotContainNull;
import com.igormaznitsa.meta.annotation.ReturnsOriginal;
import com.igormaznitsa.meta.common.utils.Assertions;
import com.igormaznitsa.mindmap.model.ExtraNote;
import com.igormaznitsa.mindmap.model.ExtraTopic;
import com.igormaznitsa.mindmap.model.MindMap;
import com.igormaznitsa.mindmap.model.Topic;
import com.igormaznitsa.mindmap.swing.panel.DialogProvider;
import com.igormaznitsa.mindmap.swing.panel.MindMapPanel;
import com.igormaznitsa.mindmap.swing.panel.Texts;
import com.igormaznitsa.mindmap.swing.services.IconID;
import com.igormaznitsa.mindmap.swing.services.ImageIconServiceProvider;
import com.igormaznitsa.mindmap.model.logger.Logger;
import com.igormaznitsa.mindmap.model.logger.LoggerFactory;
import com.igormaznitsa.mindmap.plugins.attributes.images.ImageVisualAttributePlugin;
import com.igormaznitsa.mindmap.swing.panel.ui.AbstractCollapsableElement;
import com.igormaznitsa.mindmap.swing.panel.utils.Utils;

public class Freemind2MindMapImporter extends AbstractImporter {

  private static final Icon ICO = ImageIconServiceProvider.findInstance().getIconForId(IconID.POPUP_EXPORT_FREEMIND);

  private static final Logger LOGGER = LoggerFactory.getLogger(Freemind2MindMapImporter.class);

  private static final Set<String> TOKEN_NEEDS_NEXT_LINE = new HashSet<String>(Arrays.asList("br", "div", "p", "li"));

  private enum RichContentType {
    NODE, NOTE
  }

  private static final class RichContent {

    private final RichContentType type;
    private final String text;
    private final String[] imageUrls;

    private RichContent(@Nonnull final RichContentType type, @Nonnull final String text, @Nonnull @MustNotContainNull final List<String> foundImageUrls) {
      this.type = type;
      this.text = text;
      this.imageUrls = foundImageUrls.toArray(new String[foundImageUrls.size()]);
    }

    @Nonnull
    @MustNotContainNull
    private String[] getFoundImageURLs() {
      return this.imageUrls;
    }

    @Nonnull
    private RichContentType getType() {
      return this.type;
    }

    @Nonnull
    private String getText() {
      return this.text;
    }
  }

  @Override
  @Nullable
  public MindMap doImport(@Nonnull final MindMapPanel panel, @Nonnull final DialogProvider dialogProvider, @Nullable final Topic actionTopic, @Nonnull @MustNotContainNull final Topic[] selectedTopics) throws Exception {
    final File file = this.selectFileForExtension(panel, Texts.getString("MMDImporters.Freemind2MindMap.openDialogTitle"), "mm", "Freemind files (.MM)", Texts.getString("MMDImporters.ApproveImport"));

    if (file == null) {
      return null;
    }

    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setIgnoringComments(true);
    factory.setValidating(false);
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    
    final DocumentBuilder builder = factory.newDocumentBuilder();
    
    final String content = FileUtils.readFileToString(file, "UTF-8");
    
    final Document document = builder.parse(new ByteArrayInputStream(content.getBytes("UTF-8")));

    final Element rootElement = document.getDocumentElement();
    if (!rootElement.getTagName().equals("map")) {
      throw new IllegalArgumentException("Not Freemind file");
    }

    final Map<String, Topic> idTopicMap = new HashMap<String, Topic>();
    final Map<String, String> linksMap = new HashMap<String, String>();
    final MindMap resultedMap = new MindMap(null, true);
    resultedMap.setAttribute(MindMapPanel.ATTR_SHOW_JUMPS, "true");

    final List<Element> list = getDirectChildren(rootElement, "node");
    if (list.isEmpty()) {
      Assertions.assertNotNull(resultedMap.getRoot()).setText("Empty");
    } else {
      parseTopic(file.getParentFile(), resultedMap, null, resultedMap.getRoot(), list.get(0), idTopicMap, linksMap);
    }

    for (final Map.Entry<String, String> l : linksMap.entrySet()) {
      final Topic start = idTopicMap.get(l.getKey());
      final Topic end = idTopicMap.get(l.getValue());
      if (start != null && end != null) {
        start.setExtra(ExtraTopic.makeLinkTo(resultedMap, end));
      }
    }

    return resultedMap;
  }

  @Nonnull
  @MustNotContainNull
  public static List<Element> getDirectChildren(@Nonnull final Element element, @Nonnull final String name) {
    final NodeList found = element.getElementsByTagName(name);
    final List<Element> resultList = new ArrayList<Element>();

    for (int i = 0; i < found.getLength(); i++) {
      if (found.item(i).getParentNode().equals(element) && found.item(i) instanceof Element) {
        resultList.add((Element) found.item(i));
      }
    }

    return resultList;
  }

  @Nonnull
  private static String findArrowlinkDestination(@Nonnull final Element element) {
    final List<Element> arrows = getDirectChildren(element, "arrowlink");
    return arrows.isEmpty() ? "" : arrows.get(0).getAttribute("DESTINATION");
  }

  private void parseTopic(@Nonnull final File rootFolder, @Nonnull final MindMap map, @Nullable Topic parent, @Nullable Topic preGeneratedTopic, @Nonnull Element element, @Nonnull final Map<String, Topic> idTopicMap, @Nonnull final Map<String, String> linksMap) {

    final String text = element.getAttribute("TEXT");
    final String id = element.getAttribute("ID");
    final String position = element.getAttribute("POSITION");
    final String arrowDestination = findArrowlinkDestination(element);
    final String color = element.getAttribute("COLOR");

    final List<RichContent> foundRichContent = extractRichContent(element);

    final Topic topicToProcess;
    if (preGeneratedTopic == null) {
      topicToProcess = Assertions.assertNotNull(parent).makeChild(text, null);
      if (parent.isRoot()) {
        if ("left".equalsIgnoreCase(position)) {
          AbstractCollapsableElement.makeTopicLeftSided(topicToProcess, true);
        }
      }
    } else {
      topicToProcess = preGeneratedTopic;
    }

    if (!color.isEmpty()) {
      final Color converted = Utils.html2color(color, false);
      if (converted != null) {
        topicToProcess.setAttribute(ATTR_FILL_COLOR.getText(), Utils.color2html(converted, false));
        topicToProcess.setAttribute(ATTR_TEXT_COLOR.getText(), Utils.color2html(Utils.makeContrastColor(converted), false));
      }
    }

    topicToProcess.setText(text);

    for (final RichContent r : foundRichContent) {
      switch (r.getType()) {
        case NODE: {
          if (!r.getText().isEmpty()) {
            topicToProcess.setText(r.getText().trim());
          }
        }
        break;
        case NOTE: {
          if (!r.getText().isEmpty()) {
            topicToProcess.setExtra(new ExtraNote(r.getText().trim()));
          }
        }
        break;
      }
      processImageLinkForTopic(rootFolder, topicToProcess, r.getFoundImageURLs());
    }

    if (!id.isEmpty()) {
      idTopicMap.put(id, topicToProcess);
      if (!arrowDestination.isEmpty()) {
        linksMap.put(id, arrowDestination);
      }
    }

    for (final Element e : getDirectChildren(element, "node")) {
      parseTopic(rootFolder, map, topicToProcess, null, e, idTopicMap, linksMap);
    }
  }

  private static void processImageLinkForTopic(@Nonnull final File rootFolder, @Nonnull final Topic topic, @Nonnull @MustNotContainNull final String[] imageUrls) {
    for (final String s : imageUrls) {
      try {
        URI imageUri = URI.create(s);

        final File file;
        if (imageUri.isAbsolute()) {
          file = new File(imageUri);
        } else {
          file = new File(rootFolder.toURI().resolve(imageUri));
        }

        if (file.isFile()) {
          final Image image = ImageIO.read(file);
          final ByteArrayOutputStream bos = new ByteArrayOutputStream();
          ImageIO.write((RenderedImage) image, "png", bos); //NOI18N
          bos.close();
          final String encoded = Utils.base64encode(bos.toByteArray());
          topic.setAttribute(ImageVisualAttributePlugin.ATTR_KEY, encoded);
          break;
        }
      }
      catch (final Exception ex) {
        LOGGER.warn("Can't decode or load image for URI : " + s);
      }
    }
  }

  @Nonnull
  @ReturnsOriginal
  private static StringBuilder processHtmlElement(@Nonnull final Node node, @Nonnull final StringBuilder builder, @Nonnull @MustNotContainNull final List<String> imageURLs) {
    final NodeList list = node.getChildNodes();
    for (int i = 0; i < list.getLength(); i++) {
      final Node n = list.item(i);
      switch (n.getNodeType()) {
        case Node.TEXT_NODE: {
          builder.append(n.getTextContent());
        }
        break;
        case Node.ELEMENT_NODE: {
          final String tag = n.getNodeName();
          if ("img".equals(tag)) {
            final String source = ((Element) n).getAttribute("src");
            if (!source.isEmpty()) {
              imageURLs.add(source);
            }
          }

          if (TOKEN_NEEDS_NEXT_LINE.contains(tag)) {
            builder.append('\n');
          }
          processHtmlElement(n, builder, imageURLs);
        }
        break;
      }
    }
    return builder;
  }

  @Nonnull
  @ReturnsOriginal
  private static StringBuilder extractTextFromHtmlElement(@Nonnull final Element element, @Nonnull final StringBuilder buffer, @Nonnull @MustNotContainNull final List<String> imageURLs) {
    final List<Element> html = getDirectChildren(element, "html");
    if (!html.isEmpty()) {
      final List<Element> body = getDirectChildren(html.get(0), "body");
      if (!body.isEmpty()) {
        processHtmlElement(body.get(0), buffer, imageURLs);
      }
    }
    return buffer;
  }

  @Nonnull
  @MustNotContainNull
  private static List<RichContent> extractRichContent(@Nonnull final Element richContentElement) {
    final List<Element> richContents = getDirectChildren(richContentElement, "richcontent");

    final List<RichContent> result = new ArrayList<RichContent>();

    final List<String> foundImageUrls = new ArrayList<String>();

    for (final Element e : richContents) {
      final String textType = e.getAttribute("TYPE");
      try {
        foundImageUrls.clear();
        final RichContentType type = RichContentType.valueOf(textType);
        final String text = extractTextFromHtmlElement(e, new StringBuilder(), foundImageUrls).toString().replace("\r", "");
        result.add(new RichContent(type, text, foundImageUrls));
      }
      catch (IllegalArgumentException ex) {
        LOGGER.warn("Unknown node type : " + textType);
      }
    }

    return result;
  }

  @Override
  @Nullable
  public String getMnemonic() {
    return "freemind";
  }

  @Override
  @Nonnull
  public String getName(@Nonnull final MindMapPanel panel, @Nullable final Topic actionTopic, @Nonnull @MustNotContainNull final Topic[] selectedTopics) {
    return Texts.getString("MMDImporters.Freemind2MindMap.Name");
  }

  @Override
  @Nonnull
  public String getReference(@Nonnull final MindMapPanel panel, @Nullable final Topic actionTopic, @Nonnull @MustNotContainNull final Topic[] selectedTopics) {
    return Texts.getString("MMDImporters.Freemind2MindMap.Reference");
  }

  @Override
  @Nonnull
  public Icon getIcon(@Nonnull final MindMapPanel panel, @Nullable final Topic actionTopic, @Nonnull @MustNotContainNull final Topic[] selectedTopics) {
    return ICO;
  }

  @Override
  public int getOrder() {
    return 3;
  }
}
