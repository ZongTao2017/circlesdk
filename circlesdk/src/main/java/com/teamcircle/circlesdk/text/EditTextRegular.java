package com.teamcircle.circlesdk.text;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatEditText;

import com.teamcircle.circlesdk.helper.AppSocialGlobal;

public class EditTextRegular extends AppCompatEditText {
    public EditTextRegular(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (AppSocialGlobal.textFontRegular != null) {
            setTypeface(Typeface.createFromAsset(context.getAssets(),
                    AppSocialGlobal.textFontRegular));
        }
    }
}
