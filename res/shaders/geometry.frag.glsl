
#include "common/palette.h.glsl"
#include "common/diffuse.h.glsl"
#include "common/shadows.h.glsl"

in vec2 fUv;
in vec3 fWorldPos;
in vec3 fWorldNormal;

uniform sampler2D uTexture;

out vec4 oColor;

void main() {
    vec4 texColor = texture(uTexture, fUv);
    if(texColor.a == 0.0) { discard; }
    int paletteIdx = findPaletteIndex(texColor.rgb);
    float diffuse = diffuseIntensityOf(fWorldNormal);
    if(diffuse < 0.25) {
        paletteIdx = PALETTE_AS_DARK[paletteIdx];
    }
    bool inShadow = isInShadow(fWorldPos, fWorldNormal) || diffuse < 0.0;
    if(inShadow) {
        paletteIdx = PALETTE_AS_DARK[paletteIdx];
    }
    oColor = vec4(PALETTE_COLORS[paletteIdx], 1.0);
}