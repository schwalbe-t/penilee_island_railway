
#pragma once

vec3 clipspaceToNdc(vec4 clipspacePos) {
    return clipspacePos.xyz / clipspacePos.w;
}

vec2 ndcToUv(vec3 ndcPos) {
    return ndcPos.xy * 0.5 + 0.5;
}

float ndcToDepth(vec3 ndcPos) {
    return ndcPos.z * 0.5 + 0.5;
}