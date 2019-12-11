package com.teamcircle.circlesdk.text;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import com.teamcircle.circlesdk.helper.AppSocialGlobal;

public class TextViewRegular extends AppCompatTextView {
    public TextViewRegular(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    public TextViewRegular(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        if (AppSocialGlobal.textFontRegular != null) {
            setTypeface(Typeface.createFromAsset(context.getAssets(),
                    AppSocialGlobal.textFontRegular));
        }
    }
}
