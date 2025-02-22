package com.google.android.accessibility.braille.brailledisplay;

import androidx.annotation.Nullable;
import com.google.android.accessibility.braille.common.TalkBackSpeaker;
import com.google.android.accessibility.braille.interfaces.TalkBackForBrailleDisplay;
import com.google.android.accessibility.utils.output.FeedbackItem;
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions;
import com.google.android.accessibility.utils.output.SpeechController.UtteranceCompleteRunnable;

/**
 * Speaker provides announce abilities based on TalkBack.
 *
 * <p>TODO: combine with braille keyboard TalkBackSpeaker together.
 */
public class BrailleDisplayTalkBackSpeaker implements TalkBackSpeaker {
  private static final String TAG = "BrailleDisplayTalkBackSpeaker";

  private static BrailleDisplayTalkBackSpeaker instance;
  @Nullable private TalkBackForBrailleDisplay talkBackForBrailleDisplay;

  /** Get the static singleton instance, creating it if necessary. */
  public static BrailleDisplayTalkBackSpeaker getInstance() {
    if (instance == null) {
      instance = new BrailleDisplayTalkBackSpeaker();
    }
    return instance;
  }

  public void initialize(TalkBackForBrailleDisplay talkBackForBrailleDisplay) {
    this.talkBackForBrailleDisplay = talkBackForBrailleDisplay;
  }

  @Override
  public void speak(
      CharSequence text,
      int delayMs,
      int queueMode,
      UtteranceCompleteRunnable utteranceCompleteRunnable) {
    if (talkBackForBrailleDisplay == null) {
      BrailleDisplayLog.e(TAG, "Instance does not init correctly.");
      return;
    }
    SpeakOptions speakOptions =
        SpeakOptions.create()
            .setQueueMode(queueMode)
            .setFlags(
                FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_AUDIO_PLAYBACK_ACTIVE
                    | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_MICROPHONE_ACTIVE
                    | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_SSB_ACTIVE)
            .setCompletedAction(utteranceCompleteRunnable);
    talkBackForBrailleDisplay.speak(text, delayMs, speakOptions);
  }
}
