package org.petero.droidfish;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class SeekBarPreference extends Preference 
                               implements OnSeekBarChangeListener {
    private final static int maxValue = 1000;
    private final static int DEFAULT_STRENGTH = 1000;
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

        final SeekBar bar = new SeekBar(getContext());
        bar.setMax(maxValue);
        bar.setProgress(currVal);
        bar.setOnSeekBarChangeListener(this);
        lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                                           LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.RIGHT;
        bar.setLayoutParams(lp);

        LinearLayout layout = new LinearLayout(getContext());
        layout.setPadding(25, 5, 25, 5);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(row1);
        layout.addView(bar);
        layout.setId(android.R.id.widget_frame);

        currValBox.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog dialog = new Dialog(getContext());
                dialog.setContentView(R.layout.select_strength);
                dialog.setTitle(R.string.select_strength);
                final EditText strengthView = (EditText)dialog.findViewById(R.id.selstrength_number);
                Button ok = (Button)dialog.findViewById(R.id.selstrength_ok);
                Button cancel = (Button)dialog.findViewById(R.id.selstrength_cancel);
                strengthView.setText(currValBox.getText().toString().replaceAll("%", ""));
                final Runnable selectStrength = new Runnable() {
                    public void run() {
                        try {
                            String txt = strengthView.getText().toString();
                            int strength = (int)(Double.parseDouble(txt) * 10 + 0.5);
                            if (strength < 0) strength = 0;
                            if (strength > maxValue) strength = maxValue;
                            dialog.cancel();
                            onProgressChanged(bar, strength, false);
                        } catch (NumberFormatException nfe) {
                        }
                    }
                };
                strengthView.setOnKeyListener(new OnKeyListener() {
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                            selectStrength.run();
                            return true;
                        }
                        return false;
                    }
                });
                ok.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        selectStrength.run();
                    }
                });
                cancel.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        dialog.cancel();
                    }
                });

                dialog.show();
            }
        });

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
