
in vec2 fUv;

uniform sampler2D uTexture;

void main() {
    vec4 texColor = texture(uTexture, fUv);
    if(texColor.a == 0) { discard; }
}
