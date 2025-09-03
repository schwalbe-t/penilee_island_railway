
in vec2 fUv;

uniform sampler2D uTexture;

out vec4 oColor;

void main() {
    oColor = texture(uTexture, fUv);
}