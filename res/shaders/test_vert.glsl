
layout(location = 0) in vec3 vPos;
layout(location = 1) in vec2 vUv;

uniform mat4 uLocalTransf;
uniform mat4 uModelTransf;
uniform mat4 uViewProj;

out vec2 fUv;

void main() {
    gl_Position = uViewProj * uModelTransf * uLocalTransf * vec4(vPos, 1.0);
    fUv = vUv;
}