attribute vec3 vPosition;
attribute float CellStatus;

uniform mat4 mvpMatrix;
uniform float pointSize;
uniform vec4 liveCellColor;

varying vec4 fragColor;
const vec4 dead = vec4(0.0,0.0,0.0,1.0);

void main() {
    gl_Position = mvpMatrix*vec4(vPosition,1.0);
    gl_PointSize = pointSize;
    if(CellStatus == 1.0){
        fragColor = liveCellColor;
    }else{
        fragColor = dead;
    }
}