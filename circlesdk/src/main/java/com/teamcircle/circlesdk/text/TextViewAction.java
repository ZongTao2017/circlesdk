package com.teamcircle.circlesdk.text;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import com.teamcircle.circlesdk.helper.AppSocialGlobal;

public class TextViewAction extends AppCompatTextView {
    public TextViewAction(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    public TextViewAction(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        if (AppSocialGlobal.textFontAction != null) {
            setTypeface(Typeface.createFromAsset(context.getAssets(),
                    AppSocialGlobal.textFontAction));
        } else if (AppSocialGlobal.textFontRegular != null) {
            setTypeface(Typeface.createFromAsset(context.getAssets(),
                    AppSocialGlobal.textFontRegular));
        }
    }
}
