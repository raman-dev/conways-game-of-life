package com.example.ramandeep.conwaysgameoflife;

import android.content.Context;
import android.renderscript.Int2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by Ramandeep on 2017-09-11.
 */

public class HelperMethods {
    public static int getDimension(int resourceId,Context context) {
        BufferedReader br = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(resourceId)));
        String line;
        int result = 0;
        try {
            line = br.readLine();
            String[] array = line.split("\\s|\n");
            for (int i = 0; i < array.length; i++) {
                result = Integer.parseInt(array[i]);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void getConwayInputFromRaw(ArrayList<Integer> conway_inputs, Context context, int resourceId) {
        BufferedReader br = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(resourceId)));
        String line = null;
        try {
            line = br.readLine();
            while (line != null) {
                String[] array = line.split("\\s|\n");
                for (int i = 0; i < array.length; i++) {
                    conway_inputs.add(Integer.parseInt(array[i]));
                }
                line = br.readLine();
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int fillConwayInput1D(Context context,int resourceId, byte[] input,int rows){
        ArrayList<Integer> conway_inputs = new ArrayList<Integer>();
        getConwayInputFromRaw(conway_inputs, context, resourceId);
        int dimensions = conway_inputs.remove(0);
        for (int i = 0; i < conway_inputs.size(); i++) {
            input[i] = conway_inputs.get(i).byteValue();
        }
        return dimensions;
    }


    //reads conwayObjects from a resource and returns them in a list of byte arrays and dimensions in a list of int2's
    public static void getConwayObjects(ArrayList<ConwayObject> conwayObjects, int resourceId, Context context){
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(resourceId)));
        String line = null;
        line = getLine(bufferedReader);
        while(line!=null){
            if(line.contains("=")){//
                //new conway object available to be read
                Int2 dimensions = new Int2();
                getDimensions(line,dimensions);
                int total = dimensions.x*dimensions.y;
                byte[] data = new byte[total];
                int j = 0;
                int k = 0;
                while(j < dimensions.x) {
                    String[] a = getLine(bufferedReader).split("");
                    int l = 1;
                    while(l < a.length){
                        data[k] = Integer.valueOf(a[l]).byteValue();
                        l++;
                        k++;
                    }
                    j++;
                }
                conwayObjects.add(new ConwayObject(data,dimensions));
            }
            line = getLine(bufferedReader);
        }
        try {
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void getDimensions(String line,Int2 dimensions) {
        int xIndex = line.indexOf('x');
        dimensions.y = Integer.parseInt(line.substring(xIndex +1));
        dimensions.x = Integer.parseInt(line.substring(line.indexOf('=')+1,xIndex));
    }

    private static String getLine(BufferedReader bufferedReader) {
        try {
            return bufferedReader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
