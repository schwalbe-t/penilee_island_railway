
layout(location = 0) in vec2 vNdc;
layout(location = 1) in vec2 vUv;

out vec2 fUv;

void main() {
    gl_Position = vec4(vNdc, 0.0, 1.0);
    fUv = vUv;
}