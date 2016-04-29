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
package com.igormaznitsa.mindmap.plugins.focused;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.ImageIcon;
import com.igormaznitsa.meta.annotation.MustNotContainNull;
import com.igormaznitsa.mindmap.model.Topic;
import com.igormaznitsa.mindmap.plugins.PopUpSection;
import com.igormaznitsa.mindmap.swing.panel.DialogProvider;
import com.igormaznitsa.mindmap.swing.panel.MindMapPanel;
import com.igormaznitsa.mindmap.swing.panel.Texts;
import com.igormaznitsa.mindmap.swing.services.ImageIconID;
import com.igormaznitsa.mindmap.swing.services.ImageIconServiceProvider;
import static com.igormaznitsa.meta.common.utils.Assertions.assertNotNull;
import static com.igormaznitsa.meta.common.utils.Assertions.assertNotNull;

public class RemoveTopicPlugin extends AbstractFocusedTopicActionPlugin {

  private static final ImageIcon ICO = ImageIconServiceProvider.findInstance().getIconForId(ImageIconID.POPUP_REMOVE_TOPIC);

  @Override
  public int getOrder() {
    return 2;
  }

  @Override
  public boolean isAllowed(@Nonnull final MindMapPanel panel, @Nullable final Topic actionTopic, @Nonnull @MustNotContainNull final Topic[] selectedTopics) {
    return actionTopic != null || selectedTopics.length > 0;
  }

  @Override
  @Nullable
  protected ImageIcon getIcon(@Nonnull final MindMapPanel panel, @Nullable final Topic actionTopic, @Nonnull @MustNotContainNull final Topic[] selectedTopics) {
    return ICO;
  }

  @Override
  @Nonnull
  protected String getName(@Nonnull final MindMapPanel panel, @Nullable final Topic actionTopic, @Nonnull @MustNotContainNull final Topic[] selectedTopics) {
    return selectedTopics.length > 0 ? Texts.getString("MMDGraphEditor.makePopUp.miRemoveSelectedTopics") : Texts.getString("MMDGraphEditor.makePopUp.miRemoveTheTopic");
  }

  @Override
  protected void doActionForTopic(@Nonnull final MindMapPanel panel, @Nonnull final DialogProvider dialogProvider, @Nullable final Topic actionTopic, @Nonnull @MustNotContainNull final Topic[] selectedTopics) {
    if (panel.hasSelectedTopics()) {
      panel.deleteSelectedTopics();
    } else {
      panel.deleteTopics(assertNotNull(actionTopic));
    }
  }

  @Override
  @Nonnull
  protected PopUpSection getPopUpSection() {
    return PopUpSection.MAIN;
  }
}
