
in vec2 fUv;

uniform sampler2D uRenderColor;
uniform sampler2D uRenderDepth;
uniform vec2 uRenderSize;

out vec4 oColor;

void main() {
    oColor = texture(uRenderColor, fUv);
}