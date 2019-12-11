package com.teamcircle.circlesdk.text;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import com.teamcircle.circlesdk.helper.AppSocialGlobal;

public class TextViewBold extends AppCompatTextView {
    public TextViewBold(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    public TextViewBold(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        if (AppSocialGlobal.textFontBold != null) {
            setTypeface(Typeface.createFromAsset(context.getAssets(),
                    AppSocialGlobal.textFontBold));
        } else if (AppSocialGlobal.textFontRegular != null) {
            setTypeface(Typeface.createFromAsset(context.getAssets(),
                    AppSocialGlobal.textFontRegular), Typeface.BOLD);
        } else {
            setTypeface(getTypeface(), Typeface.BOLD);
        }
    }
}
