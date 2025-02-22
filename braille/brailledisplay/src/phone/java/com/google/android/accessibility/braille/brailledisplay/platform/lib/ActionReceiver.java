/*
 * Copyright 2021 Google Inc.
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
 */

package com.google.android.accessibility.braille.brailledisplay.platform.lib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

/** A subclass of BroadcastReceiver that reduces the tedium of registering and unregistering. */
public abstract class ActionReceiver<R extends ActionReceiver<R, C>, C> extends BroadcastReceiver {

  private boolean registered;
  private C callback;

  public ActionReceiver(C callback) {
    this.callback = callback;
  }

  public void onReceive(Context context, Intent intent) {
    onReceive(callback, intent.getAction(), intent.getExtras());
  }

  protected abstract void onReceive(C callback, String action, Bundle extras);

  protected abstract String[] getActionsList();

  @SuppressWarnings("unchecked")
  public R registerSelf(Context context) {
    if (!registered) {
      IntentFilter filter = new IntentFilter();
      for (String action : getActionsList()) {
        filter.addAction(action);
      }
      context.registerReceiver(this, filter);
      registered = true;
    }
    return (R) this;
  }

  public void unregisterSelf(Context context) {
    if (registered) {
      context.unregisterReceiver(this);
      registered = false;
    }
  }
}
