
#pragma once

uniform vec3 uSunDirection;

float diffuseIntensityOf(vec3 normal) {
    return dot(normalize(normal), -normalize(uSunDirection));
}