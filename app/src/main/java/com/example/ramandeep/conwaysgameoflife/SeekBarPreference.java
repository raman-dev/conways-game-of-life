package com.example.ramandeep.conwaysgameoflife;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Created by Ramandeep on 2017-10-04.
 */

public class SeekBarPreference extends Preference implements SeekBar.OnSeekBarChangeListener{

    public static final int MAX_VALUE = 15;
    public static final int MIN_VALUE = 0;
    public static final int FRAME_DELAY_MIN = 25;

    private TextView updateTextView;
    private int mProgress = MIN_VALUE;
    private int scale = 5;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.seekbar_pref_layout);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue){
        System.out.println("called onSetInitialValue!");
        if(restorePersistedValue){
            System.out.println("    restorePersistedValue");
            mProgress = getUnscaledFrameDelay(getPersistedInt(MIN_VALUE));
        }else{
            System.out.println("    defaultValue");
            mProgress = (int)defaultValue;
            persistInt(getScaledFrameDelay(mProgress));
        }
    }

    @Override
    public void onBindView(View view){
        super.onBindView(view);
        SeekBar seekBar = view.findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setMax(MAX_VALUE);
        seekBar.setProgress(mProgress);
        updateTextView = view.findViewById(R.id.seekBarValueLabel);
        updateDisplay(mProgress*scale + FRAME_DELAY_MIN);
    }

    private void updateDisplay(int progress) {
        updateTextView.setText(""+progress + "ms");
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(fromUser){
            mProgress = progress;
            persistInt(getScaledFrameDelay(progress));
            updateDisplay(getScaledFrameDelay(progress));
        }
    }

    private int getScaledFrameDelay(int val){
        return val*scale + FRAME_DELAY_MIN;
    }

    private int getUnscaledFrameDelay(int scaled){
        return (scaled - FRAME_DELAY_MIN)/scale;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    protected Parcelable onSaveInstanceState(){
        System.out.println("called onSaveInstanceState!");
        final Parcelable superState = super.onSaveInstanceState();
        // Check whether this Preference is persistent (continually saved)
        if (isPersistent()) {
            // No need to save instance state since it's persistent,
            // use superclass state
            System.out.println("    isPersistent()");
            return superState;
        }

        // Create instance of custom BaseSavedState
        final SavedState myState = new SavedState(superState);
        // Set the state's value with the class member that holds current
        // setting value
        myState.value = mProgress;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state){
        System.out.println("called onRestoreInstanceState!");
        // Check whether we saved the state in onSaveInstanceState
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save the state, so call superclass
            super.onRestoreInstanceState(state);
            return;
        }
        // Cast state to custom BaseSavedState and pass to superclass
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());

        // Set this Preference's widget to reflect the restored state
        mProgress = myState.value;
    }

    private static class SavedState extends BaseSavedState {
        // Member that holds the setting's value
        // Change this data type to match the type saved by your Preference
        int value;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            // Get the current preference's value
            value = source.readInt();  // Change this to read the appropriate data type
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            // Write the preference's value
            dest.writeInt(value);  // Change this to write the appropriate data type
        }

        // Standard creator object using an instance of this class
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {

                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

}
