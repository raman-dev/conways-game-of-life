package com.example.ramandeep.conwaysgameoflife;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.support.annotation.IdRes;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RadioGroup;

/**
 * Created by Ramandeep on 2017-10-04.
 */

public class ColorPickerPreference extends Preference implements RadioGroup.OnCheckedChangeListener{

    public static final int COLOR_RED = 1;
    public static final int COLOR_GREEN = 2;
    public static final int COLOR_BLUE = 3;
    public static final int COLOR_PURPLE = 4;

    private int mColor = COLOR_GREEN;
    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.color_picker_layout);
    }

    @Override
    protected void onBindView(View view){
        super.onBindView(view);
        RadioGroup radioGroup = view.findViewById(R.id.cp_RadioGroup);
        radioGroup.setOnCheckedChangeListener(this);
        checkColor(radioGroup,mColor);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        //super.onSetInitialValue(restorePersistedValue, defaultValue);
        if(restorePersistedValue){
            mColor = getPersistedInt(COLOR_GREEN);
        }else{
            mColor = COLOR_GREEN;
            persistInt(COLOR_GREEN);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, @IdRes int id) {
        switch(id){
            case R.id.redRadioButton:
                mColor = COLOR_RED;
                break;
            case R.id.greenRadioButton:
                mColor = COLOR_GREEN;
                break;
            case R.id.blueRadioButton:
                mColor = COLOR_BLUE;
                break;
            case R.id.purpleRadioButton:
                mColor = COLOR_PURPLE;
                break;
        }
        persistInt(mColor);//since color changed
    }

    private void checkColor(RadioGroup radioGroup, int mColor) {
        switch(mColor){
            case COLOR_RED:
                radioGroup.check(R.id.redRadioButton);
                break;
            case COLOR_GREEN:
                radioGroup.check(R.id.greenRadioButton);
                break;
            case COLOR_BLUE:
                radioGroup.check(R.id.blueRadioButton);
                break;
            case COLOR_PURPLE:
                radioGroup.check(R.id.purpleRadioButton);
                break;
        }
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
        myState.value = mColor;
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
        mColor = myState.value;
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
