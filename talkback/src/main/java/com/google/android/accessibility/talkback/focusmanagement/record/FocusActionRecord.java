/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.google.android.accessibility.talkback.focusmanagement.record;

import static com.google.android.accessibility.utils.AccessibilityNodeInfoUtils.toStringShort;

import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.FocusFinder;
import java.util.Objects;

/**
 * A record of TalkBack performing {@link AccessibilityNodeInfoCompat#ACTION_ACCESSIBILITY_FOCUS}
 * action.
 */
public class FocusActionRecord {
  /**
   * Time when the accessibility focus action is performed. Initialized with
   * SystemClock.uptimeMillis().
   */
  private final long actionTime;
  /** Node being accessibility focused. */
  private final AccessibilityNodeInfoCompat focusedNode;
  /** Describes how to find focused node from root node. */
  private final @NonNull NodePathDescription nodePathDescription;
  /** Extra information about source focus action. */
  private final FocusActionInfo extraInfo;

  /**
   * Constructs a FocusActionRecord.
   *
   * @param focusedNode Node being accessibility focused.
   * @param extraInfo Extra information defined by action performer.
   * @param actionTime Time when focus action happens. Got by {@link SystemClock#uptimeMillis()}.
   */
  public FocusActionRecord(
      @NonNull AccessibilityNodeInfoCompat focusedNode,
      FocusActionInfo extraInfo,
      long actionTime) {
    this.focusedNode = AccessibilityNodeInfoUtils.obtain(focusedNode);
    nodePathDescription = NodePathDescription.obtain(focusedNode);
    this.extraInfo = extraInfo;
    this.actionTime = actionTime;
  }

  /** Constructs FocusActionRecord. Used internally by {@link #copy(FocusActionRecord)}. */
  private FocusActionRecord(
      AccessibilityNodeInfoCompat focusedNode,
      @NonNull NodePathDescription nodePathDescription,
      FocusActionInfo extraInfo,
      long actionTime) {
    this.focusedNode = AccessibilityNodeInfoUtils.obtain(focusedNode);
    this.nodePathDescription = new NodePathDescription(nodePathDescription);
    this.extraInfo = extraInfo;
    this.actionTime = actionTime;
  }

  /** Returns an instance of the focused node. */
  public AccessibilityNodeInfoCompat getFocusedNode() {
    return AccessibilityNodeInfoUtils.obtain(focusedNode);
  }

  /** Returns reference to node-path. */
  public NodePathDescription getNodePathDescription() {
    return nodePathDescription;
  }

  /** Returns extra information of the focus action. */
  public FocusActionInfo getExtraInfo() {
    return extraInfo;
  }

  /**
   * Returns the time when the accessibility focus action happens, which is initialized with {@link
   * SystemClock#uptimeMillis()}.
   */
  public long getActionTime() {
    return actionTime;
  }

  /** Returns a copied instance of another FocusActionRecord. */
  @Nullable
  public static FocusActionRecord copy(FocusActionRecord record) {
    if (record == null) {
      return null;
    }
    return new FocusActionRecord(
        record.focusedNode, record.nodePathDescription, record.extraInfo, record.actionTime);
  }

  /**
   * Returns the last focused node in {@code window} if it's still valid on screen, otherwise
   * returns focusable node with the same position.
   */
  @Nullable
  public static AccessibilityNodeInfoCompat getFocusableNodeFromFocusRecord(
      @Nullable AccessibilityNodeInfoCompat root,
      @NonNull FocusFinder focusFinder,
      @NonNull FocusActionRecord focusActionRecord) {
    @NonNull AccessibilityNodeInfoCompat lastFocusedNode = focusActionRecord.getFocusedNode();
    if (lastFocusedNode.refresh() && AccessibilityNodeInfoUtils.shouldFocusNode(lastFocusedNode)) {
      return lastFocusedNode;
    }

    if (root == null) {
      return null;
    }

    @Nullable
    NodePathDescription nodePath = focusActionRecord.getNodePathDescription(); // Not owner
    @Nullable
    AccessibilityNodeInfoCompat nodeAtSamePosition =
        (nodePath == null) ? null : nodePath.findNodeToRefocus(root, focusFinder);
    if ((nodeAtSamePosition != null)
        && AccessibilityNodeInfoUtils.shouldFocusNode(nodeAtSamePosition)) {
      AccessibilityNodeInfoCompat returnNode = nodeAtSamePosition;
      nodeAtSamePosition = null;
      return returnNode;
    }

    return null;
  }

  @Override
  public int hashCode() {
    return Objects.hash(actionTime, focusedNode, nodePathDescription, extraInfo);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof FocusActionRecord)) {
      return false;
    }
    FocusActionRecord otherRecord = (FocusActionRecord) other;
    return (focusedNode.equals(otherRecord.focusedNode))
        && (nodePathDescription.equals(otherRecord.nodePathDescription))
        && (extraInfo.equals(otherRecord.extraInfo))
        && (actionTime == otherRecord.actionTime);
  }

  public boolean focusedNodeEquals(AccessibilityNodeInfoCompat targetNode) {
    if (focusedNode == null || targetNode == null) {
      return false;
    }
    return (focusedNode == targetNode) || focusedNode.equals(targetNode);
  }

  @Override
  public String toString() {
    return "FocusActionRecord: \n    "
        + "node="
        + toStringShort(focusedNode)
        + "\n    "
        + "time="
        + actionTime
        + "\n    "
        + "extraInfo="
        + extraInfo.toString();
  }
}
