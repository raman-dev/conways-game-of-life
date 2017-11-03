package com.example.ramandeep.conwaysgameoflife;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Ramandeep on 2017-10-06.
 */

public class CustomListPreference extends ListPreference {
    public CustomListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        System.out.println("called constructor!");
    }

    @Override
    public void onBindView(View view){
        super.onBindView(view);
        CharSequence entry = getEntry();
        String title = "Conway Object File: "+entry;
        setTitle(title);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        CharSequence entry = getEntry();
        String title = "Conway Object File: "+entry;
        setTitle(title);
        return super.onCreateView(parent);
    }
}
