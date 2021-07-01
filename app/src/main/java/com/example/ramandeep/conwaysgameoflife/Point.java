package com.example.ramandeep.conwaysgameoflife;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_DYNAMIC_DRAW;
import static android.opengl.GLES20.GL_ELEMENT_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_POINTS;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.GL_UNSIGNED_INT;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glBufferSubData;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawElements;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform4fv;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;

/**
 * Created by Ramandeep on 2017-09-07.
 */

public class Point extends RenderObject {

    private static final int POSITION_COMPONENT_COUNT = 3;
    private static final int COLOR_COMPONENT_COUNT = 4;
    private static final int STATUS_COMPONENT_COUNT = 1;

    private static final int VERTICES_PER_CELL = 1;
    private static final int FLOATS_PER_CELL_2 = VERTICES_PER_CELL * (POSITION_COMPONENT_COUNT + STATUS_COMPONENT_COUNT);

    private int[] indices;
    //length of each vertex attribute in bytes
    //x,y,z,cellStatus
    private int vertexStatusStride = BYTES_PER_FLOAT*(POSITION_COMPONENT_COUNT + STATUS_COMPONENT_COUNT);
    private int firstStatusPosition = BYTES_PER_FLOAT*(POSITION_COMPONENT_COUNT);

    private float pointSize = 1f;

    private int rows;
    private int columns;
    private int numberOfCells;

    private float width;
    private float height;

    private FloatBuffer vertexAndStatusBuffer;
    private FloatBuffer cellColorBuffer;


    private float[] vertexAndStatus;
    private int cellStatusReference;
    private int cellColorReference;

    private float[] cellColor = {0f,1f,0f,1f};
    private ConcurrentLinkedQueue<byte[]> frameQueue;

    public Point(ConcurrentLinkedQueue<byte[]> frameQueue) {
        this.frameQueue = frameQueue;
        initStatusBuffers();
    }

    private void initStatusBuffers() {
        cellColorBuffer = getNativeOrderFloatBuffer(cellColor.length);
        cellColorBuffer.put(cellColor);
        cellColorBuffer.position(0);
    }

    /*
        current structure
        ------------------
        vec3 pos;
        float cellStatus;

        xn,yn,zn,cellStatus_n,

     */
    @Override
    public void draw() {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,buffers[0]);
        glBindBuffer(GL_ARRAY_BUFFER,buffers[1]);

        glVertexAttribPointer(positionReference,POSITION_COMPONENT_COUNT,GL_FLOAT,false,vertexStatusStride,0);
        glVertexAttribPointer(cellStatusReference,STATUS_COMPONENT_COUNT,GL_FLOAT,false,vertexStatusStride,firstStatusPosition);

        glEnableVertexAttribArray(positionReference);
        glEnableVertexAttribArray(cellStatusReference);

        glUseProgram(program);

        //new frames available
        //push the oldest and then
        if(!frameQueue.isEmpty()){
            pushNewData();
        }
        glUniformMatrix4fv(mvpMatrixReference,1,false,mvpMatrixBuffer);
        glUniform1f(pointSizeReference,pointSize);
        glUniform4fv(cellColorReference,1,cellColorBuffer);

        glDrawElements(GL_POINTS,indexBuffer.capacity(),GL_UNSIGNED_INT,0);

        glDisableVertexAttribArray(positionReference);
        glDisableVertexAttribArray(cellStatusReference);
    }

    private void pushNewData() {
        //push new values to gpu
        //lets say a frame arrives while pushing data
        //then what?
        //needs to be a queue
        //
        //i want to push the entire byte array at once
        byte[] data = frameQueue.poll();//remove the oldest frame
        //Log.i("GLThread", Arrays.toString(data));
        //replace the status attribute in the vertexAndStatus array
        int stride = POSITION_COMPONENT_COUNT;//jump over this many elements
        for(int i = 0;i < data.length;i++,stride += POSITION_COMPONENT_COUNT + 1){
            vertexAndStatusBuffer.put(stride,data[i]);
        }
        //correct placement
//        vertexAndStatusBuffer.put(3,1f);
//        vertexAndStatusBuffer.put(7,1f);
        glBufferSubData(GL_ARRAY_BUFFER,0,vertexAndStatusBuffer.capacity()*BYTES_PER_FLOAT,vertexAndStatusBuffer);
    }

    @Deprecated
    private void changeCellStatusAt(int location,int bufferByteSize,FloatBuffer cellBuffer){
        int offset = POSITION_COMPONENT_COUNT + (location - 1)*FLOATS_PER_CELL_2;//VERTICES_PER_CELL*(POSITION_COMPONENT_COUNT + STATUS_COMPONENT_COUNT);
        int byteOffset = BYTES_PER_FLOAT*offset;
        //target,offset into attribute vector,size in bytes, cellBuffer to send to gpu
        glBufferSubData(GL_ARRAY_BUFFER,byteOffset,bufferByteSize,cellBuffer);
        //whole buffer or small amount of data
        //i can replace the whole buffer
    }

    public void setAttributeAndVBO(){
        //get the render thread looper
        buffers = new int[2];
        glGenBuffers(2,buffers,0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,buffers[0]);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER,indexBuffer.capacity() * BYTES_PER_INT,indexBuffer,GL_STATIC_DRAW);

        //vertex and color or vertex and status
        glBindBuffer(GL_ARRAY_BUFFER,buffers[1]);
        glBufferData(GL_ARRAY_BUFFER,vertexAndStatusBuffer.capacity() * BYTES_PER_FLOAT,vertexAndStatusBuffer,GL_DYNAMIC_DRAW);

        positionReference = glGetAttribLocation(program,"vPosition");
        cellStatusReference = glGetAttribLocation(program,"CellStatus");
        pointSizeReference = glGetUniformLocation(program,"pointSize");
        mvpMatrixReference = glGetUniformLocation(program,"mvpMatrix");
        cellColorReference = glGetUniformLocation(program,"liveCellColor");
    }

    public void setPointSize(float pointSize) {
        this.pointSize = pointSize;
    }

    public void generatePointGrid() {
        int gridVertexPointer = 0;
        int i = 1;
        float x = -width/2f + pointSize/2f;
        float y = -height/2f + pointSize/2f;
        while(i <= rows){
            int j = 1;
            while(j <= columns){
                    gridVertexPointer = insertVertex(vertexAndStatus,gridVertexPointer,x,y,0f);
                    vertexAndStatus[gridVertexPointer] = 0f;
                    gridVertexPointer++;
                x += pointSize;
                j++;
            }
            x = -width/2f + pointSize/2f;
            y += pointSize;
            i++;
        }
        vertexAndStatusBuffer = getNativeOrderFloatBuffer(vertexAndStatus.length);
        vertexAndStatusBuffer.put(vertexAndStatus);
        vertexAndStatusBuffer.position(0);

        initIndexBuffer(indices);
    }

    public void setPointGridDimensions(int width, int height, int rows, int columns) {
        this.rows = rows;
        this.columns = columns;

        this.width = width;
        this.height = height;

        numberOfCells = rows*columns;
        vertexAndStatus = new float[numberOfCells*FLOATS_PER_CELL_2];
        indices = new int[numberOfCells];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }
    }

    public void setCellColor(float[] cellColor) {
        this.cellColor = cellColor;
        cellColorBuffer.clear();
        cellColorBuffer.put(cellColor);
        cellColorBuffer.position(0);
    }
}
