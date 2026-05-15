/*
 * Copyright (C) 2026 BlissLabs
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

package org.blisslabs.inputctl.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import org.blisslabs.inputctl.R;

public class AboutPreference extends Preference {

    public AboutPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.layout_about_preference);
        setSelectable(false);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        
        TextView textView = (TextView) holder.findViewById(R.id.about_text);
        if (textView != null) {
            String content = getContext().getString(R.string.about_inputctl_what) + "\n\n" +
                             getContext().getString(R.string.about_inputctl_who)  + "\n\n" +
                             getContext().getString(R.string.about_inputctl_copyright);

            textView.setText(content);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }
}
