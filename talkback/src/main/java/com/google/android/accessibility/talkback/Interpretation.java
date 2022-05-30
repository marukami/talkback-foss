/*
 * Copyright (C) 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.accessibility.talkback;

import static com.google.android.accessibility.compositor.Compositor.EVENT_UNKNOWN;
import static com.google.android.accessibility.utils.AccessibilityNodeInfoUtils.toStringShort;
import static com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_UNKNOWN;

import android.graphics.Rect;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.compositor.Compositor;
import com.google.android.accessibility.compositor.EventInterpretation;
import com.google.android.accessibility.talkback.focusmanagement.interpreter.ScreenState;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.StringBuilderUtils;
import com.google.android.accessibility.utils.input.CursorGranularity;
import com.google.android.accessibility.utils.traversal.TraversalStrategy;
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SearchDirection;
import com.google.android.accessibility.utils.traversal.TraversalStrategyUtils;
import com.google.auto.value.AutoValue;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Data-structure classes to hold event-interpretations. Many sub-classes are expected. */
public abstract class Interpretation {
  /** Interpretation sub-type that only contains a general ID. */
  public static final class ID extends Interpretation {

    /** Possible ID values. */
    public static enum Value {
      SCROLL_CANCEL_TIMEOUT,
      CONTINUOUS_READ_CONTENT_FOCUSED,
      CONTINUOUS_READ_INTERRUPT,
      STATE_CHANGE,
      PASS_THROUGH_INTERACTION_START,
      PASS_THROUGH_INTERACTION_END,
      ACCESSIBILITY_FOCUSED,
      SUBTREE_CHANGED,
      ACCESSIBILITY_EVENT_IDLE,
    }

    public final Value value;

    public ID(Value v) {
      this.value = v;
    }

    @Override
    public boolean equals(Object otherObject) {
      if (otherObject == null || !(otherObject instanceof ID)) {
        return false;
      }
      ID otherId = (ID) otherObject;
      return (this.value == otherId.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public String toString() {
      return (value == null) ? "null" : value.toString();
    }
  }

  /** Interpretation sub-type for power-events. */
  public static final class Power extends Interpretation {
    public final boolean connected;
    public final int percent;

    public Power(boolean connected, int percent) {
      this.connected = connected;
      this.percent = percent;
    }

    @Override
    public boolean equals(Object otherObject) {
      @Nullable Power other = castOrNull(otherObject, Power.class);
      return (other != null)
          && (this.connected == other.connected)
          && (this.percent == other.percent);
    }

    @Override
    public int hashCode() {
      return Objects.hash(connected, percent);
    }

    @Override
    public String toString() {
      return StringBuilderUtils.joinFields(
          StringBuilderUtils.optionalTag("connected", connected),
          StringBuilderUtils.optionalInt("percent", percent, BatteryMonitor.UNKNOWN_LEVEL));
    }
  }

  /** Interpretation sub-type that only contains a compositor-event ID. */
  public static final class CompositorID extends Interpretation {

    @Compositor.Event public final int value;
    private EventInterpretation eventInterpretation;
    private AccessibilityNodeInfoCompat node;

    public CompositorID(@Compositor.Event int v) {
      this.value = v;
      this.eventInterpretation = null;
      this.node = null;
    }

    public CompositorID(
        @Compositor.Event int v,
        @Nullable EventInterpretation eventInterp,
        @Nullable AccessibilityNodeInfoCompat node) {
      this.value = v;
      this.eventInterpretation = eventInterp;
      if (node != null) {
        this.node = AccessibilityNodeInfoCompat.obtain(node);
      }
    }

    public EventInterpretation getEventInterpretation() {
      return eventInterpretation;
    }

    public AccessibilityNodeInfoCompat getNode() {
      return node;
    }

    @Override
    public boolean equals(Object otherObject) {
      if (otherObject == null || !(otherObject instanceof CompositorID)) {
        return false;
      }
      CompositorID otherId = (CompositorID) otherObject;

      if (this.value != otherId.value) {
        return false;
      }

      // Any one of this.eventInterpretation or otherId.eventInterpretation is null, but the other
      // isn't null. It returns false.
      if ((this.eventInterpretation == null) ^ (otherId.eventInterpretation == null)) {
        return false;
      }

      if ((this.eventInterpretation != null)
          && !this.eventInterpretation.toString().equals(otherId.eventInterpretation.toString())) {
        return false;
      }

      return ((this.node == null && otherId.node == null)
          || ((this.node != null) && (this.node.equals(otherId.node))));
    }

    @Override
    public int hashCode() {
      return Objects.hash(value, eventInterpretation, node);
    }

    @Override
    public String toString() {
      return StringBuilderUtils.joinFields(
          "CompositorID= {",
          StringBuilderUtils.optionalInt("value", value, EVENT_UNKNOWN),
          StringBuilderUtils.optionalSubObj("eventInterp", eventInterpretation),
          StringBuilderUtils.optionalSubObj("node", node),
          "}");
    }
  }

  /** Interpretation sub-type for key-combo events. */
  @AutoValue
  public abstract static class KeyCombo extends Interpretation {
    public abstract int id();

    public abstract @Nullable String text();

    public static KeyCombo create(int id, @Nullable String text) {
      return new AutoValue_Interpretation_KeyCombo(id, text);
    }

    @Override
    public final String toString() {
      return String.format("%d=%s", id(), text());
    }
  }

  /** Interpretation sub-type for directional-navigation. */
  @AutoValue
  public abstract static class DirectionNavigation extends Interpretation {

    @SearchDirection
    public abstract int direction();

    /** Caller does not own returned node. */
    public abstract @Nullable AccessibilityNodeInfoCompat destination();

    /** Caller retains ownership of destination, this class makes a copy. */
    public static DirectionNavigation create(
        @SearchDirection int direction, @Nullable AccessibilityNodeInfoCompat destination) {
      return new AutoValue_Interpretation_DirectionNavigation(
          direction, AccessibilityNodeInfoUtils.obtain(destination));
    }

    @Override
    public final String toString() {
      return StringBuilderUtils.joinFields(
          StringBuilderUtils.optionalInt("direction", direction(), SEARCH_FOCUS_UNKNOWN),
          ((destination() == null) ? null : "destination=" + toStringShort(destination())));
    }
  }

  /** Interpretation sub-type for voice command. */
  @AutoValue
  public abstract static class VoiceCommand extends Interpretation {

    /** Touch actions */
    public static enum Action {
      VOICE_COMMAND_UNKNOWN,
      VOICE_COMMAND_NEXT_GRANULARITY,
      VOICE_COMMAND_SELECT_ALL,
      VOICE_COMMAND_START_SELECT,
      VOICE_COMMAND_END_SELECT,
      VOICE_COMMAND_COPY,
      VOICE_COMMAND_INSERT,
      VOICE_COMMAND_CUT,
      VOICE_COMMAND_PASTE,
      VOICE_COMMAND_DELETE,
      VOICE_COMMAND_LABEL,
      VOICE_COMMAND_REPEAT_SEARCH,
      VOICE_COMMAND_FIND,
      VOICE_COMMAND_START_AT_TOP,
      VOICE_COMMAND_START_AT_NEXT,
      VOICE_COMMAND_COPY_LAST_SPOKEN_UTTERANCE,
      VOICE_COMMAND_FIRST,
      VOICE_COMMAND_LAST,
      VOICE_COMMAND_HOME,
      VOICE_COMMAND_BACK,
      VOICE_COMMAND_RECENT,
      VOICE_COMMAND_ALL_APPS,
      VOICE_COMMAND_NOTIFICATIONS,
      VOICE_COMMAND_QUICK_SETTINGS,
      VOICE_COMMAND_BRIGHTEN_SCREEN,
      VOICE_COMMAND_DIM_SCREEN,
      VOICE_COMMAND_SHOW_COMMAND_LIST,
    }

    public abstract Action command();

    /** Caller does not own returned node. */
    public abstract @Nullable AccessibilityNodeInfoCompat targetNode();

    public abstract @Nullable CursorGranularity granularity();

    public abstract @Nullable CharSequence text();

    public static VoiceCommand create(
        Action command,
        @Nullable AccessibilityNodeInfoCompat targetNode,
        @Nullable CursorGranularity granularity,
        @Nullable CharSequence text) {
      return new AutoValue_Interpretation_VoiceCommand(
          command, AccessibilityNodeInfoUtils.obtain(targetNode), granularity, text);
    }

    @Override
    public final String toString() {
      return StringBuilderUtils.joinFields(
          StringBuilderUtils.optionalField("command", command()),
          ((targetNode() == null) ? null : "targetNode=" + toStringShort(targetNode())),
          StringBuilderUtils.optionalField("granularity", granularity()),
          StringBuilderUtils.optionalText("text", text()));
    }
  }

  /** Interpretation sub-type for input-focus. */
  public static final class InputFocus extends Interpretation {

    private AccessibilityNodeInfoCompat node;

    /** Caller retains ownership of node, this class makes a copy. */
    public InputFocus(AccessibilityNodeInfoCompat node) {
      this.node = AccessibilityNodeInfoUtils.obtain(node);
    }

    /** Caller does not own returned node. */
    public AccessibilityNodeInfoCompat getNode() {
      return node;
    }

    @Override
    public boolean equals(Object otherObject) {
      if (otherObject == null || !(otherObject instanceof InputFocus)) {
        return false;
      }
      InputFocus other = (InputFocus) otherObject;
      if (this.node == other.getNode()) {
        return true;
      }
      return (this.node != null) && this.node.equals(other.getNode());
    }

    @Override
    public int hashCode() {
      return Objects.hash(node);
    }

    @Override
    public String toString() {
      return StringBuilderUtils.optionalSubObj("node", node);
    }
  }

  /** Interpretation sub-type for manual scroll. */
  @AutoValue
  public abstract static class ManualScroll extends Interpretation {

    @TraversalStrategy.SearchDirection
    public abstract int direction();

    public abstract @Nullable ScreenState screenState();

    public static ManualScroll create(int direction, @Nullable ScreenState screenState) {
      return new AutoValue_Interpretation_ManualScroll(direction, screenState);
    }

    @Override
    public final String toString() {
      return StringBuilderUtils.joinFields(
          StringBuilderUtils.optionalField(
              "direction", TraversalStrategyUtils.directionToString(direction())),
          StringBuilderUtils.optionalSubObj("screenState", screenState()));
    }
  }

  /** Interpretation sub-type for window events. */
  @AutoValue
  public abstract static class WindowChange extends Interpretation {

    public abstract boolean forceRestoreFocus();

    public abstract @Nullable ScreenState screenState();

    public static WindowChange create(
        boolean forceRestoreFocus, @Nullable ScreenState screenState) {
      return new AutoValue_Interpretation_WindowChange(forceRestoreFocus, screenState);
    }
  }

  /** Interpretation sub-type for touch-events. */
  @AutoValue
  public abstract static class Touch extends Interpretation {

    /** Touch actions */
    public static enum Action {
      TAP,
      LIFT,
      LONG_PRESS,
      TOUCH_START,
      TOUCH_NOTHING,
      TOUCH_FOCUSED_NODE,
      TOUCH_UNFOCUSED_NODE,
    }

    public abstract Touch.Action action();

    /** Caller does not own returned node. */
    public abstract @Nullable AccessibilityNodeInfoCompat target();

    /** Caller retains ownership of target, this class makes a copy. */
    public static Touch create(Touch.Action action, @Nullable AccessibilityNodeInfoCompat target) {
      return new AutoValue_Interpretation_Touch(action, AccessibilityNodeInfoUtils.obtain(target));
    }

    public static Touch create(Touch.Action action) {
      return new AutoValue_Interpretation_Touch(action, /* target= */ null);
    }

    @Override
    public final String toString() {
      return StringBuilderUtils.joinFields(
          StringBuilderUtils.optionalField("action", action()),
          ((target() == null) ? null : "target=" + toStringShort(target())));
    }
  }

  /** Interpretation sub-type for accessibility focused event. */
  @AutoValue
  public abstract static class AccessibilityFocused extends Interpretation {

    public abstract boolean needsCaption();

    public static AccessibilityFocused create(boolean needsCaption) {
      return new AutoValue_Interpretation_AccessibilityFocused(needsCaption);
    }

    @Override
    public final String toString() {
      return StringBuilderUtils.joinFields(
          StringBuilderUtils.optionalTag("needsCaption", needsCaption()));
    }
  }

  /** Interpretation sub-type for UI change event. */
  @AutoValue
  public abstract static class UiChange extends Interpretation {

    private static final UiChange WHOLE_SCREEN_UI_CHANGE =
        new AutoValue_Interpretation_UiChange(
            /* sourceBoundsInScreen= */ null, UiChangeType.WHOLE_SCREEN_UI_CHANGED);

    /** UI change types */
    public enum UiChangeType {
      UNKNOWN,
      WHOLE_SCREEN_UI_CHANGED,
      VIEW_CLICKED,
      VIEW_SCROLLED,
      WINDOW_CONTENT_CHANGED
    }

    public abstract @Nullable Rect sourceBoundsInScreen();

    public abstract UiChangeType uiChangeType();

    public static UiChange createWholeScreenUiChange() {
      return WHOLE_SCREEN_UI_CHANGE;
    }

    public static UiChange createPartialScreenUiChange(
        Rect sourceBoundsInScreen, UiChangeType uiChangeType) {
      return new AutoValue_Interpretation_UiChange(sourceBoundsInScreen, uiChangeType);
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Static utility methods

  private static @Nullable <T> T castOrNull(Object object, Class<T> clazz) {
    return (object == null || !clazz.isInstance(object)) ? null : clazz.cast(object);
  }
}
