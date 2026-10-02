#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};
layout(std140) uniform AobaShaderParams {
    float Time;
    vec2 Resolution;
    vec4 StartColor;
    vec4 EndColor;
    float Radius;
    float Quality;
    float Angle;
};

uniform sampler2D Sampler0; // game framebuffer

layout(location = 0) in vec2 localUV;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 startColor = StartColor;
    vec4 endColor = EndColor;
    float radius = Radius;
    float quality = max(Quality, 1.0);
    float angle = radians(Angle);

    vec2 texSize = vec2(textureSize(Sampler0, 0));
    vec2 screenUV = gl_FragCoord.xy / Resolution;

    vec3 color = vec3(0.0);
    float total = 0.0;

    for (float x = -radius; x <= radius; x += quality) {
        for (float y = -radius; y <= radius; y += quality) {
            if (x * x + y * y > radius * radius) continue;

            vec2 offset = vec2(x, y) / texSize;
            float weight = 1.0 - length(vec2(x, y)) / radius;
            color += texture(Sampler0, screenUV + offset).rgb * weight;
            total += weight;
        }
    }

    color /= total;

    // Compute gradient tint based on local UV coordinates and angle.
    vec2 dir = vec2(cos(angle), sin(angle));
    float t = dot(localUV - 0.5, dir) + 0.5;
    t = clamp(t, 0.0, 1.0);
    vec4 tint = mix(startColor, endColor, t);

    color *= tint.rgb;
    vec4 result = vec4(color, tint.a) * ColorModulator;
    fragColor = vec4(result.rgb * result.a, result.a);
}
