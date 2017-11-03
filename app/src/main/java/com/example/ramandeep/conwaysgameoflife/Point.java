package com.example.ramandeep.conwaysgameoflife;

import java.nio.FloatBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private int vertexStatusStride = BYTES_PER_FLOAT*(POSITION_COMPONENT_COUNT + STATUS_COMPONENT_COUNT);
    private int firstStatusPosition = BYTES_PER_FLOAT*(POSITION_COMPONENT_COUNT);

    private float pointSize = 1f;

    private int rows;
    private int columns;
    private int numberOfCells;

    private float width;
    private float height;

    private ConcurrentLinkedQueue<Object[]> liveList;
    private ConcurrentLinkedQueue<Object[]> deadList;

    private Object[] array;

    private FloatBuffer vertexAndStatusBuffer;
    private FloatBuffer liveCellBuffer;
    private FloatBuffer deadCellBuffer;
    private FloatBuffer cellColorBuffer;

    private int liveCellBufferByteSize;
    private int deadCellBufferByteSize;

    private float[] vertexAndStatus;
    private int cellStatusReference;
    private int cellColorReference;

    float[] livingCell = {1.0f};
    float[] deadCell = {0.0f};

    private boolean update = true;

    private float[] cellColor = {0f,1f,0f,1f};

    public Point(ConcurrentLinkedQueue<Object[]> liveList, ConcurrentLinkedQueue<Object[]> deadList) {
        this.liveList = liveList;
        this.deadList = deadList;
        initStatusBuffers();
    }

    private void initStatusBuffers() {
        liveCellBuffer = getNativeOrderFloatBuffer(livingCell.length);
        liveCellBuffer.put(livingCell);
        liveCellBuffer.position(0);
        liveCellBufferByteSize = liveCellBuffer.capacity() * BYTES_PER_FLOAT;


        deadCellBuffer = getNativeOrderFloatBuffer(deadCell.length);
        deadCellBuffer.put(deadCell);
        deadCellBuffer.position(0);
        deadCellBufferByteSize = deadCellBuffer.capacity() * BYTES_PER_FLOAT;

        cellColorBuffer = getNativeOrderFloatBuffer(cellColor.length);
        cellColorBuffer.put(cellColor);
        cellColorBuffer.position(0);
    }

    @Override
    public void draw() {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,buffers[0]);
        glBindBuffer(GL_ARRAY_BUFFER,buffers[1]);

        glVertexAttribPointer(positionReference,POSITION_COMPONENT_COUNT,GL_FLOAT,false,vertexStatusStride,0);
        glVertexAttribPointer(cellStatusReference,STATUS_COMPONENT_COUNT,GL_FLOAT,false,vertexStatusStride,firstStatusPosition);

        glEnableVertexAttribArray(positionReference);
        glEnableVertexAttribArray(cellStatusReference);

        glUseProgram(program);

        if(update){
            updateCells();
        }

        glUniformMatrix4fv(mvpMatrixReference,1,false,mvpMatrixBuffer);
        glUniform1f(pointSizeReference,pointSize);
        glUniform4fv(cellColorReference,1,cellColorBuffer);

        glDrawElements(GL_POINTS,indexBuffer.capacity(),GL_UNSIGNED_INT,0);

        glDisableVertexAttribArray(positionReference);
        glDisableVertexAttribArray(cellStatusReference);
    }

    private void updateCells(){
        if(!liveList.isEmpty()){
            array = liveList.poll();
            for (int i = 0; i < array.length; i++) {
                int location = (int) array[i];
                changeCellStatusAtL(numberOfCells - location);
            }
        }
        if(!deadList.isEmpty()){
            array = deadList.poll();
            for (int i = 0; i < array.length; i++) {
                int location = (int) array[i];
                changeCellStatusAtD(numberOfCells - location);
            }
        }
    }
    
    private void changeCellStatusAtL(int location){
        int offset = POSITION_COMPONENT_COUNT + (location - 1)*FLOATS_PER_CELL_2;//VERTICES_PER_CELL*(POSITION_COMPONENT_COUNT + STATUS_COMPONENT_COUNT);
        int byteOffset = BYTES_PER_FLOAT*offset;
        glBufferSubData(GL_ARRAY_BUFFER,byteOffset,liveCellBufferByteSize,liveCellBuffer);
    }
    private void changeCellStatusAtD(int location){
        int offset = POSITION_COMPONENT_COUNT + (location - 1)*FLOATS_PER_CELL_2;//VERTICES_PER_CELL*(POSITION_COMPONENT_COUNT + STATUS_COMPONENT_COUNT);
        int byteOffset = BYTES_PER_FLOAT*offset;
        glBufferSubData(GL_ARRAY_BUFFER,byteOffset,deadCellBufferByteSize,deadCellBuffer);
    }

    public void setAttributeAndVBO(){
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
        cellColorReference=  glGetUniformLocation(program,"liveCellColor");
    }

    @Deprecated
    private void changeColorAt(int row, int column) {
        if(row < 1 || row > rows){
            return;
        }if(column < 1 || column > columns){
            return;
        }
        //in the vertex and color
        //get four locations
        //from row and column
        //cell number from the beginning
        //first determine which cell out of all the cells am i altering
        int cellNumber = numberOfCells - row*columns + column;
        //alter the color of the cellnumberth cell
        //each cell has 1 vertex
        //each vertex has 2 parts a position and a color
        //each vertex is as such {x,y,z,r,g,b,a};
        //each cell is as such {x,y,z,r,g,b,a};
        int offset = POSITION_COMPONENT_COUNT + (cellNumber - 1)*VERTICES_PER_CELL*(POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT);
        int byteOffset = BYTES_PER_FLOAT*offset;

        //glBufferSubData(GL_ARRAY_BUFFER,byteOffset,livingColorBuffer.capacity() * BYTES_PER_FLOAT,livingColorBuffer);
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

    public void stopUpdating() {
        update = false;
    }

    public void startUpdating(){
        update = true;
    }

    public void setCellColor(float[] cellColor) {
        this.cellColor = cellColor;
        cellColorBuffer.clear();
        cellColorBuffer.put(cellColor);
        cellColorBuffer.position(0);
    }
}
