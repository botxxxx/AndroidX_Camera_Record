package com.askey.widget;

import android.view.View;

import com.askey.thermal.R;

public class VerticalPageTransformer implements VerticalViewPager.PageTransformer {
    @Override
    public void transformPage(View view, float position) {
        int pageWidth = view.getWidth();
        int pageHeight = view.getHeight();
        CustomTextView customTextView = view.findViewById(R.id.customTextView);
        customTextView.setTextColor(view.getResources().getColor(R.color.gray_dark));
        int width = customTextView.getText().toString().length();
        float dafTranslation = 0;
        float xPosition = pageWidth * -position + dafTranslation * (width == 2 ? 2f : width == 3 ? 1.6f : 1);
        float yPosition = position * pageHeight;
        view.setTranslationX(xPosition);
        view.setTranslationY(yPosition);
        if (position < 0) {
            view.setAlpha(1f + 0.5f * position);
            view.setScaleX(0.2f * position + 1);
        } else if (position < 1) {
            view.setAlpha(1f - 0.5f * position);
            view.setScaleX(-0.2f * position + 1);
            customTextView.setTextColor(view.getResources().getColor(R.color.orange));
        } else {
            view.setAlpha(1f - 0.5f * position);
            view.setScaleX(-0.2f * position + 1);
        }
    }
}