
layout(location = 0) in vec3 vPos;
layout(location = 1) in vec2 vUv;
layout(location = 2) in vec3 vNorm;

const int MAX_INSTANCE_COUNT = 128;

uniform mat4 uLocalTransf;
uniform mat4 uModelTransfs[MAX_INSTANCE_COUNT];
uniform mat4 uViewProj;

out vec2 fUv;
out vec3 fWorldPos;
out vec3 fWorldNormal;

void main() {
    fUv = vUv;
    vec4 worldPos
        = uModelTransfs[gl_InstanceID] 
        * uLocalTransf 
        * vec4(vPos, 1.0);
    gl_Position = uViewProj * worldPos;
    vec3 worldNorm 
        = mat3(uModelTransfs[gl_InstanceID])
        * mat3(uLocalTransf) 
        * vNorm;
    fUv = vUv;
    fWorldPos = worldPos.xyz;
    fWorldNormal = worldNorm;
}