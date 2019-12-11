package com.teamcircle.circlesdk.text;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.appcompat.widget.AppCompatEditText;

import com.teamcircle.circlesdk.helper.AppSocialGlobal;

public class CustomEditText extends AppCompatEditText {
    private Callback callback;

    public CustomEditText(Context context, AttributeSet attrs) {
        super(context, attrs, android.R.attr.editTextStyle);
        if (AppSocialGlobal.textFontRegular != null) {
            setTypeface(Typeface.createFromAsset(context.getAssets(),
                    AppSocialGlobal.textFontRegular));
        }
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (this.callback != null) {
                callback.onBackPressed();
            }
        }
        return super.onKeyPreIme(keyCode, event);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public interface Callback {
        void onBackPressed();
    }
}
