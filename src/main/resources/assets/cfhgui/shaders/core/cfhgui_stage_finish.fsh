#version 150

// Slate stage: the look of a finished scene. The scene (premultiplied alpha) plus its glow, a touch more
// saturation and contrast, a warm tint, a colour laid into the shade and another into the light (split toning),
// darker corners, and a grain too fine to see that keeps dark gradients from breaking into bands. Premultiplied
// out, so a see-through scene stays see-through.

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform float Bloom;
uniform float Vignette;
uniform float Contrast;
uniform float Saturation;
uniform float Warmth;
uniform vec3 Shadows;
uniform vec3 Highlights;
uniform float Grain;
uniform float Alpha;

in vec2 texCoord0;

out vec4 fragColor;

float hash(vec2 p) {
    vec3 q = fract(vec3(p.xyx) * 0.1031);
    q += dot(q, q.yzx + 33.33);
    return fract((q.x + q.y) * q.z);
}

void main() {
    vec4 scene = texture(Sampler0, texCoord0);
    vec3 glow = texture(Sampler1, texCoord0).rgb * Bloom;
    float a = scene.a;

    vec3 c = scene.rgb;
    float grey = dot(c, vec3(0.299, 0.587, 0.114));
    c = mix(vec3(grey), c, Saturation);
    // Contrast about mid grey; the colour is premultiplied, so mid grey is too.
    c = (c - 0.5 * a) * Contrast + 0.5 * a;
    c += vec3(Warmth, Warmth * 0.3, -Warmth) * a;
    // Split toning: the shade leans one way, the light another; mid tones keep their colour.
    float luma = clamp(grey / max(a, 0.001), 0.0, 1.0);
    c += (Shadows * (1.0 - luma) * (1.0 - luma) + Highlights * luma * luma) * a;
    c = max(c, vec3(0.0)) + glow;

    vec2 d = (texCoord0 - 0.5) * vec2(1.0, 0.82);
    float corner = smoothstep(0.32, 0.92, length(d) * 1.6);
    c *= 1.0 - Vignette * corner;
    c += (hash(gl_FragCoord.xy) - 0.5) * Grain / 255.0 * a;

    float outA = max(a, clamp(max(glow.r, max(glow.g, glow.b)), 0.0, 1.0));
    fragColor = vec4(c, outA) * Alpha;
}
