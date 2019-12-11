package com.teamcircle.circlesdk.text;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import com.teamcircle.circlesdk.helper.AppSocialGlobal;

public class TextViewProductName  extends AppCompatTextView {

    public TextViewProductName(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TextViewProductName(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        if (AppSocialGlobal.textFontProductName != null) {
            setTypeface(Typeface.createFromAsset(context.getAssets(),
                    AppSocialGlobal.textFontProductName));
        } else if (AppSocialGlobal.textFontRegular != null) {
            setTypeface(Typeface.createFromAsset(context.getAssets(),
                    AppSocialGlobal.textFontRegular));
        }
    }
}
