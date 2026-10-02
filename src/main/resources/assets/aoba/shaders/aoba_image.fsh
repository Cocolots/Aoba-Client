#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};
layout(std140) uniform AobaShaderParams {
    vec4 Tint;
};

uniform sampler2D Sampler0;

layout(location = 0) in vec2 localUV;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 texSample = texture(Sampler0, localUV);
    vec4 color = texSample * Tint * ColorModulator;
    fragColor = vec4(color.rgb * color.a, color.a);
}
