#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};
layout(std140) uniform AobaShaderParams {
    vec4 InnerColor;
    vec4 OuterColor;
    float CenterX;
    float CenterY;
    float Size;
};

uniform sampler2D Sampler0;

layout(location = 0) in vec2 localUV;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 innerColor = InnerColor;
    vec4 outerColor = OuterColor;
    vec2 center = vec2(CenterX, CenterY);
    float size = Size;

    vec2 delta = abs(localUV - center);
    float dist = (delta.x + delta.y) / size;
    float t = clamp(dist, 0.0, 1.0);

    vec4 color = mix(innerColor, outerColor, t) * ColorModulator;
    vec4 texSample = texture(Sampler0, localUV);
    color.a *= min(texSample.r, texSample.a);
    fragColor = vec4(color.rgb * color.a, color.a);
}
