package org.petero.droidfish;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class SeekBarPreference extends Preference 
                               implements OnSeekBarChangeListener {
    private final int maxValue = 1000;
    private final int DEFAULT_STRENGTH = 1000;
    private int currVal = DEFAULT_STRENGTH;
    private TextView currValBox;

    public SeekBarPreference(Context context) {
        super(context);
    }
    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        TextView name = new TextView(getContext());
        name.setText(getTitle());
        name.setTextSize(20);
        name.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        name.setGravity(Gravity.LEFT);
        LinearLayout.LayoutParams lp =
            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                          LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.LEFT;
        lp.weight  = 1.0f;
        name.setLayoutParams(lp);

        currValBox = new TextView(getContext());
        currValBox.setTextSize(12);
        currValBox.setTypeface(Typeface.MONOSPACE, Typeface.ITALIC);
        currValBox.setPadding(2, 5, 0, 0);
        currValBox.setText(strengthString());
        lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                           LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        currValBox.setLayoutParams(lp);

        LinearLayout row1 = new LinearLayout(getContext());
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.addView(name);
        row1.addView(currValBox);

        SeekBar bar = new SeekBar(getContext());
        bar.setMax(maxValue);
        bar.setProgress(currVal);
        bar.setOnSeekBarChangeListener(this);
        lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                                           LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.RIGHT;
        bar.setLayoutParams(lp);

        LinearLayout layout = new LinearLayout(getContext());
        layout.setPadding(20, 5, 10, 5);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(row1);
        layout.addView(bar);
        layout.setId(android.R.id.widget_frame);

        return layout; 
    }

    private final String strengthString() {
        return String.format("%.1f%%", currVal*0.1);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
        if (!callChangeListener(progress)) {
            seekBar.setProgress(currVal); 
            return; 
        }
        seekBar.setProgress(progress);
        currVal = progress;
        currValBox.setText(strengthString());
        SharedPreferences.Editor editor =  getEditor();
        editor.putInt(getKey(), progress);
        editor.commit();
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        notifyChanged();
    }

    @Override 
    protected Object onGetDefaultValue(TypedArray a, int index) {
        int defVal = a.getInt(index, DEFAULT_STRENGTH);
        if (defVal > maxValue) defVal = maxValue;
        if (defVal < 0) defVal = 0;
        return defVal;
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        int val = restorePersistedValue ? getPersistedInt(DEFAULT_STRENGTH) : (Integer)defaultValue;
        if (!restorePersistedValue)
            persistInt(val);
        currVal = val;
    }
}
