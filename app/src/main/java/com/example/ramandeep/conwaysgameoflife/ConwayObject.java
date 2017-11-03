package com.example.ramandeep.conwaysgameoflife;

import android.renderscript.Int2;

/**
 * Created by Ramandeep on 2017-09-22.
 */

public class ConwayObject {
    public byte[] data;
    public Int2 dimensions;
    public ConwayObject(byte[] data,Int2 dimensions){
        this.data = data;
        this.dimensions = dimensions;
    }
}
