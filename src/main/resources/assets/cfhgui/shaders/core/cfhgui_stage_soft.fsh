#version 150

// Slate stage: soft, stylized light. A warm key light that wraps round the form instead of cutting it in half, a cool
// fill from the other side, a line of light along the edges that turn away, an ambient floor so nothing goes black,
// and up to eight lamps: coloured points of light that fall off to nothing at their reach. What shines by itself
// (Emissive, with the block light the vertices carry) is as bright as that light says, whatever lights it; water and
// glass (Gloss) mirror the air at a slant. All vectors are in view space.

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform vec3 KeyPos;
uniform vec3 KeyColor;
uniform vec3 FillDir;
uniform vec3 FillColor;
uniform vec3 Ambient;
uniform vec3 RimColor;
uniform float RimStrength;
uniform float Wrap;
uniform float Muted;
// A cast: the thing as if made of one stone. Its texture's own colours go and the stone's (rgb) take their place, as
// much as a says, before any light falls on it: it keeps its form and the drawing of its texture as a faint relief, and
// is lit like everything round it. 0 = as it is.
uniform vec4 Cast;
uniform float Emissive;
// Water and glass: how much the surface mirrors the colour of the air where it is seen at a slant, and catches the key
// light; 0 for everything else.
uniform float Gloss;
// A lamp to a column: x, y, z and reach; r, g, b and nothing. A reach of 0 is a lamp that is out.
uniform mat4 LampPosA;
uniform mat4 LampPosB;
uniform mat4 LampColA;
uniform mat4 LampColB;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
// A footprint, in the units of the mesh drawn: its middle (x, z) and one over its half sizes (x, z).
uniform vec4 Footprint;
// Where on the way out from the footprint's middle (0..1) the fade into the air begins, and the power of its outline
// (2 = an ellipse, higher = a rectangle with rounder and rounder corners). 0 = no footprint.
uniform vec2 FootprintFade;
// 0 = textures as they are; 1 = painted in the manner of Slate's chests: every texture flattened into its own colours,
// its grain gone, what is left of its drawing in three tones (light, body, shade), every block face in a darker frame.
uniform float Stylize;
// What is see-through is not drawn: under this alpha (the game's own layers: 0.1, and 0.5 for the mipmapped cutout
// one, whose small copies blur the edge). BaseLevel 1 reads the texture at its full size only, as the game's plain
// cutout layer does (grass, flowers): read from the small copies, their clear pixels, black, are mixed into them.
uniform float AlphaCutoff;
uniform float BaseLevel;
// The colour of the light a build's own torches and lanterns throw (what its meshes carry as receivedLight), at full
// strength; nothing (the default) for everything else.
uniform vec3 Received;

in vec3 viewPos;
in vec3 viewNormal;
in vec3 localPos;
in vec4 vertexColor;
in vec2 texCoord0;
in float blockLight;
in float receivedLight;

out vec4 fragColor;

// An even spread of thresholds over every 4 x 4 block of pixels (a Bayer matrix), 0..1: the 2 x 2 pattern
// (0 2 / 3 1) of the pixel within its 2 x 2 block, a quarter as much of it again for the block within its 4 x 4.
float threshold(vec2 pixel) {
    vec2 fine = mod(floor(pixel), 2.0), coarse = mod(floor(pixel * 0.5), 2.0);
    float a = 2.0 * abs(fine.x - fine.y) + fine.y, b = 2.0 * abs(coarse.x - coarse.y) + coarse.y;
    return (4.0 * a + b + 0.5) / 16.0;
}

float luma(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

// A texel painted the way Slate's chests are: flat colour, no grain, the drawing in three tones, every face in a frame.
// The colour is the texture's own, averaged over the texels round this one and, more, over the whole of it (the block
// atlas keeps small copies of every sprite, its own: nothing of a neighbour bleeds in); how much lighter or darker than
// the texels round it the texel is picks one of three tones (light, body, shade), as each of the chests' planks is
// painted. A face of the block atlas gets a frame a texel wide as the chests' faces have, catching the light along its
// top edge and in shade along its foot; and the colour is a little fuller, as theirs is. What is not on an atlas (a
// skin, the chests' own paint) has no small copies to be flattened with, and is left as it is.
vec3 painted(vec4 tex, vec2 uv) {
    ivec2 size = textureSize(Sampler0, 0);
    if (size.x < 256) {
        return tex.rgb;
    }
    vec3 around = textureLod(Sampler0, uv, 2.0).rgb;
    vec3 whole = textureLod(Sampler0, uv, 4.0).rgb;
    vec3 plain = mix(around, whole, 0.6);
    float ratio = luma(tex.rgb) / max(luma(textureLod(Sampler0, uv, 1.0).rgb), 0.03);
    float tone = ratio < 0.86 ? 0.76 : (ratio > 1.12 ? 1.2 : 1.0);
    vec3 c = plain * tone;
    vec2 texel = mod(floor(uv * vec2(size)), 16.0);
    if (texel.y < 0.5) {
        c *= 0.9;
    } else if (texel.y > 14.5) {
        c *= 0.62;
    } else if (texel.x < 0.5 || texel.x > 14.5) {
        c *= 0.72;
    }
    float g = luma(c);
    return max(mix(vec3(g), c, 1.25), 0.0);
}

// One lamp: all of its light close by, none at its reach, on the smooth curve between; a face turned a little away
// still takes some, as it does from the key light.
vec3 lamp(vec4 at, vec4 color, vec3 n) {
    if (at.w <= 0.0) {
        return vec3(0.0);
    }
    vec3 d = at.xyz - viewPos;
    float far = length(d);
    float a = clamp(1.0 - far / at.w, 0.0, 1.0);
    float facing = clamp((dot(n, d / max(far, 0.001)) + 0.3) / 1.3, 0.0, 1.0);
    return color.rgb * (a * a) * facing;
}

void main() {
    vec4 tex = BaseLevel > 0.5 ? textureLod(Sampler0, texCoord0, 0.0) : texture(Sampler0, texCoord0);
    if (tex.a < AlphaCutoff) {
        discard;
    }
    if (Stylize > 0.0) {
        tex.rgb = mix(tex.rgb, painted(tex, texCoord0), Stylize);
    }
    // A footprint: towards its rim a piece of land goes over into the air, and past its rounded outline it is not there
    // at all, so it ends in the haze instead of at a straight cut. Its outer part does not only take the air's colour,
    // it goes, pixel by pixel in an even pattern that the stage's finer-than-the-screen target smooths into a fade: a
    // hill that stands up out of the haze into the sky leaves no pale wall against it.
    float gone = 0.0;
    if (FootprintFade.y > 0.0) {
        vec2 q = abs(localPos.xz - Footprint.xy) * Footprint.zw;
        float r = pow(pow(q.x, FootprintFade.y) + pow(q.y, FootprintFade.y), 1.0 / FootprintFade.y);
        gone = smoothstep(FootprintFade.x, 1.0, r);
        if (smoothstep(0.45, 0.98, gone) > threshold(gl_FragCoord.xy)) {
            discard;
        }
    }
    vec3 n = normalize(viewNormal);
    vec3 toKey = normalize(KeyPos - viewPos);
    float key = clamp((dot(n, toKey) + Wrap) / (1.0 + Wrap), 0.0, 1.0);
    key = key * key * (3.0 - 2.0 * key);
    float fill = clamp(dot(n, normalize(FillDir)) * 0.5 + 0.5, 0.0, 1.0);
    vec3 toEye = normalize(-viewPos);
    float edge = 1.0 - clamp(dot(n, toEye), 0.0, 1.0);
    float rim = edge * edge * edge * RimStrength;

    vec3 lamps = vec3(0.0);
    for (int i = 0; i < 4; i++) {
        lamps += lamp(LampPosA[i], LampColA[i], n);
        lamps += lamp(LampPosB[i], LampColB[i], n);
    }

    vec3 base = tex.rgb * vertexColor.rgb;
    if (Cast.a > 0.0) {
        // However dark or bright the texture, the stone is the stone's: of the texture only its drawing is kept, its
        // darks a little darker than its lights, so a cast of coal and one of quartz are nearly the same pale stone.
        float relief = 0.68 + 0.46 * sqrt(clamp(luma(base), 0.0, 1.0));
        base = mix(base, Cast.rgb * relief, Cast.a);
    }
    vec3 light = Ambient + KeyColor * key + FillColor * fill + lamps;
    // Light from a build's own lamps, falling off with the level as the game's light does (15 full, 8 a quarter).
    light += Received * (receivedLight / (4.0 - 3.0 * receivedLight));
    vec3 c = base * light + RimColor * rim * (0.35 + 0.65 * key);
    // Light of its own: the whole thing comes up to its own colour, and what is bright in it (the flame, the glass
    // of a lantern) goes past that, so the finish makes it glow. (A cast of a lantern is stone, and does not shine.)
    float glow = Emissive * blockLight * (1.0 - Cast.a);
    if (glow > 0.0) {
        float bright = smoothstep(0.45, 0.9, max(base.r, max(base.g, base.b)));
        c = max(c, base * (0.35 + 0.75 * glow)) + base * bright * glow * 0.55;
    }
    // (Stone is not seen through, whatever glass it was cast from.)
    float alpha = mix(tex.a * vertexColor.a, 1.0, Cast.a);
    // Gloss: seen flat on, water shows what is in it; seen at a slant it mirrors the sky (the colour of the air, which
    // a scene sets to its horizon) and hides what is under it; where the key light's reflection meets the eye it glints.
    if (Gloss > 0.0) {
        float slant = 1.0 - clamp(dot(n, toEye), 0.0, 1.0);
        float mirror = slant * slant * slant * slant * Gloss;
        vec3 sky = FogColor.a > 0.0 ? FogColor.rgb : Ambient * 1.6;
        c = mix(c, sky * 1.08, mirror);
        vec3 bounce = reflect(-toKey, n);
        float glint = pow(clamp(dot(bounce, toEye), 0.0, 1.0), 48.0) * Gloss;
        c += KeyColor * glint * 0.9;
        alpha = mix(alpha, 1.0, clamp(mirror + glint, 0.0, 1.0));
    }
    // Muted: what is there but out of reach keeps its form and loses its colour and half its light.
    float grey = dot(c, vec3(0.299, 0.587, 0.114));
    c = mix(c, vec3(grey) * vec3(0.6, 0.59, 0.57), Muted);
    // Distance: what is far goes over into the colour of the air. A stage without fog keeps start and end alike.
    float far = FogEnd > FogStart ? clamp((length(viewPos) - FogStart) / (FogEnd - FogStart), 0.0, 1.0) : 0.0;
    c = mix(c, FogColor.rgb, max(far * far * (3.0 - 2.0 * far) * FogColor.a, gone));
    fragColor = vec4(c, alpha) * ColorModulator;
}
