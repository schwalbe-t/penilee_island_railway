
#pragma once

const float E = 2.71828182845904523536028747135266250;
const float PI = 3.14159265358979323846264338327950288;
const float TAU = 6.28318530717958647692528676655900576;

#define RGB_255(r, g, b) (vec3((r), (g), (b)) / 255.0)
#define RGBA_255(r, g, b, a) (vec4((r), (g), (b), (a)) / 255.0)