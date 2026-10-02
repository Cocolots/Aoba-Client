#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};

uniform sampler2D Sampler0;

layout(location = 0) in vec2 localUV;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 texSample = texture(Sampler0, localUV);
    float mask = min(texSample.r, texSample.a);
    vec4 color = vec4(1.0, 1.0, 1.0, mask);
    color *= ColorModulator;
    fragColor = vec4(color.rgb * color.a, color.a);
}
