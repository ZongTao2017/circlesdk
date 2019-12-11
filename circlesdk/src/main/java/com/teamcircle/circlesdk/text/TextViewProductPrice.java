package com.teamcircle.circlesdk.text;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import com.teamcircle.circlesdk.helper.AppSocialGlobal;

public class TextViewProductPrice extends AppCompatTextView {

    public TextViewProductPrice(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TextViewProductPrice(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        if (AppSocialGlobal.textFontProductPrice != null) {
            setTypeface(Typeface.createFromAsset(context.getAssets(),
                    AppSocialGlobal.textFontProductPrice));
        } else if (AppSocialGlobal.textFontRegular != null) {
            setTypeface(Typeface.createFromAsset(context.getAssets(),
                    AppSocialGlobal.textFontRegular));
        }
    }
}
