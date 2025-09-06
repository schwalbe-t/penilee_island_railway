
#pragma once

#include "projection.h.glsl"

const int MAX_LIGHT_COUNT = 8;

uniform int uLightCount;
uniform mat4 uLightViewProj[MAX_LIGHT_COUNT];
uniform sampler2DArray uShadowMaps;
uniform float uShadowDepthBias;
uniform float uShadowNormalOffset;
uniform bool uOutOfBoundsLit;

bool isInShadow(vec3 worldPos, vec3 surfaceNormal) {
    vec3 surfacePos = worldPos + surfaceNormal * uShadowNormalOffset;
    bool inAnyLightCoverage = false;
    for(int i = 0; i < uLightCount; i += 1) {
        vec4 clipPos = uLightViewProj[i] * vec4(surfacePos, 1.0);
        vec3 ndcPos = clipspaceToNdc(clipPos);
        vec2 uvPos = ndcToUv(ndcPos);
        bool inLightCoverage = 0.0 <= uvPos.x && uvPos.x <= 1.0
            && 0.0 <= uvPos.y && uvPos.y <= 1.0;
        if(!inLightCoverage) { continue; }
        inAnyLightCoverage = true;
        float surfaceDepth = ndcToDepth(ndcPos);
        float lightDepth = texture(uShadowMaps, vec3(uvPos, i)).r;
        if(surfaceDepth <= lightDepth + uShadowDepthBias) {
            return false; 
        }
    }
    return inAnyLightCoverage || !uOutOfBoundsLit;
}