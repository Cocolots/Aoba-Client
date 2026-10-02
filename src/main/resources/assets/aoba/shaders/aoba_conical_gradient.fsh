#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};
layout(std140) uniform AobaShaderParams {
    vec4 StartColor;
    vec4 EndColor;
    float StartAngle;
    float CenterX;
    float CenterY;
};

uniform sampler2D Sampler0;

layout(location = 0) in vec2 localUV;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 startColor = StartColor;
    vec4 endColor = EndColor;
    float startAngle = radians(StartAngle);
    vec2 center = vec2(CenterX, CenterY);

    vec2 delta = localUV - center;
    float angle = atan(delta.y, delta.x) - startAngle;
    float t = fract(angle / (2.0 * 3.14159265));

    vec4 color = mix(startColor, endColor, t) * ColorModulator;
    vec4 texSample = texture(Sampler0, localUV);
    color.a *= min(texSample.r, texSample.a);
    fragColor = vec4(color.rgb * color.a, color.a);
}
