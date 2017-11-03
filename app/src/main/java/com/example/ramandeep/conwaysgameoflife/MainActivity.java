package com.example.ramandeep.conwaysgameoflife;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;

import static com.example.ramandeep.conwaysgameoflife.HelperMethods.getConwayObjects;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private final int[] conwayResourceIDs = {
            R.raw.conway_objects_a,R.raw.conway_objects_b, //0 , 1
            R.raw.conway_objects_c,R.raw.conway_objects_d, //2 , 3
            R.raw.conway_objects_e,R.raw.conway_objects_f, //4 , 5
            R.raw.conway_objects_g,R.raw.conway_objects_h, //6 , 7
            R.raw.conway_objects_i,R.raw.conway_objects_j, //8 , 9
            R.raw.conway_objects_k,R.raw.conway_objects_l, //10, 11
            R.raw.conway_objects_m,R.raw.conway_objects_n, //12, 13
            R.raw.conway_objects_o,R.raw.conway_objects_p, //14, 15
            R.raw.conway_objects_q,R.raw.conway_objects_r, //16, 17
            R.raw.conway_objects_s,R.raw.conway_objects_t, //18, 19
            R.raw.conway_objects_u,R.raw.conway_objects_v, //20, 21
            R.raw.conway_objects_w,R.raw.conway_objects_x, //22, 23
            R.raw.conway_objects_y,R.raw.conway_objects_z};//24, 25

    private ConwayGLSurfaceView conwayGLSurfaceView;
    private ArrayList<ConwayObject> conwayObjects;

    private SharedPreferences.OnSharedPreferenceChangeListener sharedPrefListener;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar)findViewById(R.id.mainToolbar);
        setSupportActionBar(toolbar);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                switch(s){
                    case "grid_visible":
                        conwayGLSurfaceView.gridVisible(sharedPreferences.getBoolean(s,true));
                        break;
                    case "frame_delay":
                        conwayGLSurfaceView.setFrameDelay(sharedPreferences.getInt(s,SeekBarPreference.FRAME_DELAY_MIN));
                        break;
                    case "cell_color":
                        conwayGLSurfaceView.setCellColor(sharedPreferences.getInt(s,ColorPickerPreference.COLOR_GREEN));
                        break;
                    case "conway_objects":
                        int loc = Integer.parseInt(sharedPreferences.getString(s,"0"));
                        conwayObjects.clear();
                        getConwayObjects(conwayObjects,conwayResourceIDs[loc],getApplicationContext());
                        conwayGLSurfaceView.setConwayObjectList(conwayObjects);
                        break;
                }
            }
        };

        conwayGLSurfaceView = (ConwayGLSurfaceView)findViewById(R.id.conwayGLSurfaceView);
        conwayObjects = new ArrayList<>();
        getConwayObjects(conwayObjects,conwayResourceIDs[Integer.parseInt(sharedPref.getString("conway_objects","0"))],this);
        conwayGLSurfaceView.setConwayObjectList(conwayObjects);


        Button startButton = (Button)findViewById(R.id.startButton);
        Button stopButton = (Button)findViewById(R.id.stopButton);
        Button nextButton = (Button)findViewById(R.id.nextButton);

        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        nextButton.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.settings_button){
            // Intent for the activity to open when user selects the notification
            Intent intent = new Intent(this, SettingsActivity.class);//always use this method
            startActivity(intent);
            return true;
        }
        return false;
    }

    @Override
    protected void onResume(){
        super.onResume();
        conwayGLSurfaceView.onResume();
        sharedPref.unregisterOnSharedPreferenceChangeListener(sharedPrefListener);
    }

    @Override
    protected void onPause(){
        super.onPause();
        conwayGLSurfaceView.onPause();
        sharedPref.registerOnSharedPreferenceChangeListener(sharedPrefListener);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        sharedPref.unregisterOnSharedPreferenceChangeListener(sharedPrefListener);
        conwayGLSurfaceView.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.startButton:
                conwayGLSurfaceView.onClickStart();
                break;
            case R.id.stopButton:
                conwayGLSurfaceView.onClickStop();
                break;
            case R.id.nextButton:
                conwayGLSurfaceView.onClickNext();
                break;
        }
    }
}
