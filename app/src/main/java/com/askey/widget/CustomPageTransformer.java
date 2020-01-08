package com.askey.widget;

import android.view.View;

import com.askey.record.R;

public class CustomPageTransformer implements VerticalViewPager.PageTransformer {
    @Override
    public void transformPage(View view, float position) {
        int pageWidth = view.getWidth();
        int pageHeight = view.getHeight();
        float dafTranslation = 0;
        float xPosition = pageWidth * -position;
        view.setTranslationX(xPosition);
        float yPosition = position * pageHeight - dafTranslation;
        view.setTranslationY(yPosition);
        CustomTextView item_timezone = view.findViewById(R.id.customTextView);
        item_timezone.setTextColor(view.getResources().getColor(R.color.gray_dark));
        item_timezone.setTranslationX(dafTranslation * 1.5f);
        if (position < 0) {
            view.setAlpha(1f + 0.3f * position);
            item_timezone.setScaleX(0.2f * position + 1);
        } else if (position < 1) {
            view.setAlpha(1f - 0.3f * position);
            item_timezone.setScaleX(-0.2f * position + 1);
            item_timezone.setTextColor(view.getResources().getColor(R.color.green));
        } else {
            view.setAlpha(1f - 0.3f * position);
            item_timezone.setScaleX(-0.2f * position + 1);
        }
    }
}
