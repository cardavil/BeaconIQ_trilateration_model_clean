package com.beaconiq.trilateration.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.beaconiq.trilateration.R;

public class HelpTipView extends LinearLayout {

    private final String helpText;
    private TextView helpTextView;
    private boolean expanded;

    public HelpTipView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HelpTipView);
        helpText = a.getString(R.styleable.HelpTipView_helpText);
        a.recycle();

        TextView toggle = new TextView(context);
        toggle.setText("(?)");
        toggle.setTextColor(ContextCompat.getColor(context, R.color.teal));
        toggle.setTextSize(13);
        toggle.setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2));
        addView(toggle);

        toggle.setOnClickListener(v -> {
            if (expanded) {
                collapse();
            } else {
                expand();
            }
            expanded = !expanded;
        });
    }

    private void expand() {
        View current = (View) getParent();
        ViewGroup container = null;
        View anchor = null;

        while (current != null && current.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) current.getParent();
            if (parent instanceof LinearLayout
                    && ((LinearLayout) parent).getOrientation() == VERTICAL) {
                container = parent;
                anchor = current;
                break;
            }
            current = (View) parent;
        }

        if (container == null) return;

        helpTextView = new TextView(getContext());
        helpTextView.setText(helpText);
        helpTextView.setTextColor(ContextCompat.getColor(getContext(), R.color.text_muted));
        helpTextView.setTextSize(12);
        helpTextView.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(8));
        helpTextView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.surface_2));

        int index = container.indexOfChild(anchor);
        container.addView(helpTextView, index + 1);
    }

    private void collapse() {
        if (helpTextView != null && helpTextView.getParent() != null) {
            ((ViewGroup) helpTextView.getParent()).removeView(helpTextView);
            helpTextView = null;
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
