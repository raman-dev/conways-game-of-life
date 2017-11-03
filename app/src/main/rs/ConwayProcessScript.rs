#pragma version(1)
#pragma rs java_package_name(com.example.ramandeep.conwaysgameex5)

#include "rs_debug.rsh"

const int8_t LIVING = 1;
const int8_t DEAD = 0;

int rows;
int columns;
int totalCells;

rs_allocation inAlloc;

static int getTopIndex(int x){
    int nx = x - columns;
    if(nx < 0){
       nx = totalCells - (columns - x);
    }
    return nx;
}

static int getLeftIndex(int x){
    int nx = x - 1;
    //means a left edge
    if(x % columns == 0){
       //then
       nx = x + columns - 1;
    }
    return nx;
}

static int getRightIndex(int x){
    int nx = x + 1;
    //means a right edge
    if(nx % columns == 0){
       //then
       nx = x - columns + 1;
    }
    return nx;
}

static int getBottomIndex(int x){
    //if at the bottom row then wrap to the top row
	int nx = x + columns;
	if(nx >= totalCells){
	    nx = columns - (totalCells - x);
	}
	return nx;
}

//faster nextGen2 calculation
int8_t RS_KERNEL nextGen2(int8_t in, int x){

    int leftIndex = getLeftIndex(x);
    int rightIndex = getRightIndex(x);
    int topIndex = getTopIndex(x);
    int bottomIndex = getBottomIndex(x);

    int topLeftIndex = getLeftIndex(topIndex);
    int topRightIndex = getRightIndex(topIndex);
    int bottomLeftIndex = getLeftIndex(bottomIndex);
    int bottomRightIndex = getRightIndex(bottomIndex);

    int8_t left = *(int8_t*)rsGetElementAt(inAlloc,leftIndex);
    int8_t right = *(int8_t*)rsGetElementAt(inAlloc,rightIndex);
    int8_t top = *(int8_t*)rsGetElementAt(inAlloc,topIndex);
    int8_t bottom = *(int8_t*)rsGetElementAt(inAlloc,bottomIndex);

    int8_t topLeft = *(int8_t*)rsGetElementAt(inAlloc,topLeftIndex);
    int8_t topRight = *(int8_t*)rsGetElementAt(inAlloc,topRightIndex);
    int8_t bottomLeft = *(int8_t*)rsGetElementAt(inAlloc,bottomLeftIndex);
    int8_t bottomRight = *(int8_t*)rsGetElementAt(inAlloc,bottomRightIndex);


    int8_t sum = left + right + top + bottom + topLeft + topRight + bottomRight + bottomLeft;

    if(in == LIVING){
        if(sum == 2 || sum == 3){
            return LIVING;
        }
        else{
         return DEAD;
        }
    }
    else{
        if(sum == 3){
            return LIVING;
        }else{
            return DEAD;
        }
    }
}

//return the locations of living cells
int RS_KERNEL getLivingCellCoords(int8_t in,int x){
    //we only want living cell locations
    if(in == LIVING){
        return x;
    }
    return -1;
}