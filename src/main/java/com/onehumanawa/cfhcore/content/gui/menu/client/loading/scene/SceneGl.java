package com.onehumanawa.cfhcore.content.gui.menu.client.loading.scene;

import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.system.MemoryUtil;

final class SceneGl {
   static final int OPAQUE = 0;
   static final int BLEND = 1;
   static final int ADD = 2;
   static final int MIRRORED = 3;
   private static final int SHADOW_SIDE = 2048;
   private static final String VERTEX = "#version 150\nin vec3 aPos;\nin vec4 aNormal;\nin vec2 aUv;\nin vec4 aColor;\nin vec4 aExtra;\nuniform mat4 uViewProj;\nuniform mat4 uLightViewProj;\nout vec3 vWorld;\nout vec3 vNormal;\ncentroid out vec2 vUv;\nout vec4 vColor;\nflat out vec3 vExtra;\nout vec4 vShadow;\nvoid main() {\n    vWorld = aPos;\n    vNormal = aNormal.xyz;\n    vUv = aUv;\n    vColor = aColor;\n    vExtra = aExtra.xyz;\n    vShadow = uLightViewProj * vec4(aPos + aNormal.xyz * 0.035, 1.0);\n    gl_Position = uViewProj * vec4(aPos, 1.0);\n}\n";
   private static final String FRAGMENT = "#version 150\nin vec3 vWorld;\nin vec3 vNormal;\ncentroid in vec2 vUv;\nin vec4 vColor;\nflat in vec3 vExtra;\nin vec4 vShadow;\n\nuniform sampler2D uAtlas;\nuniform sampler2DShadow uShadowMap;\nuniform float uShadowOn;\nuniform float uShadowTexel;\nuniform vec3 uEye;\nuniform vec3 uKeyDir;\nuniform vec3 uKeyColor;\nuniform vec3 uFillDir;\nuniform vec3 uFillColor;\nuniform vec3 uAmbient;\nuniform vec3 uRimColor;\nuniform float uWrap;\nuniform vec4 uLamp[3];\nuniform vec3 uLampColor[3];\nuniform vec3 uBackground;\nuniform vec3 uFloorLit;\nuniform vec4 uPool;\nuniform vec4 uStage;\nuniform float uTime;\n\nout vec4 fragColor;\n\nfloat hash(vec2 p) {\n    vec3 q = fract(vec3(p.xyx) * 0.1031);\n    q += dot(q, q.yzx + 33.33);\n    return fract((q.x + q.y) * q.z);\n}\n\nfloat noise(vec2 p) {\n    vec2 i = floor(p), f = fract(p);\n    f = f * f * (3.0 - 2.0 * f);\n    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);\n}\n\n// How much of the key light gets here: 25 looks at the shadow map round the place, each one smoothed by the\n// driver, so the edge of a shadow is a soft ramp.\nfloat shadow() {\n    if (uShadowOn < 0.5) return 1.0;\n    vec3 p = vShadow.xyz / vShadow.w * 0.5 + 0.5;\n    if (p.x <= 0.0 || p.x >= 1.0 || p.y <= 0.0 || p.y >= 1.0 || p.z >= 1.0) return 1.0;\n    float s = 0.0;\n    for (int x = -2; x <= 2; x++) {\n        for (int y = -2; y <= 2; y++) {\n            s += texture(uShadowMap, vec3(p.xy + vec2(x, y) * uShadowTexel * 1.6, p.z - 0.0012));\n        }\n    }\n    return s / 25.0;\n}\n\nvec3 lamps(vec3 n, float turned) {\n    vec3 sum = vec3(0.0);\n    for (int i = 0; i < 3; i++) {\n        vec3 d = uLamp[i].xyz - vWorld;\n        float far = length(d);\n        float a = clamp(1.0 - far / uLamp[i].w, 0.0, 1.0);\n        float facing = mix(1.0, clamp(dot(n, d / max(far, 0.001)) * 0.5 + 0.5, 0.0, 1.0), turned);\n        sum += uLampColor[i] * a * a * facing;\n    }\n    return sum;\n}\n\n// Lava as the game paints it, texel by texel, but made here: two layers of noise that drift through each other,\n// cut into a few steps of colour.\nvec3 lava(vec2 texel) {\n    vec2 c = floor(texel);\n    float a = noise(c * 0.23 + vec2(0.0, uTime * 0.42));\n    float b = noise(c * 0.51 + vec2(uTime * 0.21, -uTime * 0.17));\n    float n = floor((a * 0.62 + b * 0.38) * 7.0) / 6.0;\n    vec3 dark = vec3(0.66, 0.14, 0.02), mid = vec3(0.98, 0.43, 0.05), hot = vec3(1.0, 0.83, 0.33);\n    return n < 0.5 ? mix(dark, mid, n * 2.0) : mix(mid, hot, (n - 0.5) * 2.0);\n}\n\nvoid main() {\n    int kind = int(vExtra.y * 255.0 / 32.0 + 0.5);\n    float grain = (hash(gl_FragCoord.xy) - 0.5) / 255.0;\n\n    if (kind == 2) {\n        // A glow: strongest in the middle of its quad, nothing at its edge. Added to what is there.\n        float r = length(vUv * 2.0 - 1.0);\n        float a = clamp(1.0 - r, 0.0, 1.0);\n        fragColor = vec4(vColor.rgb * a * a * vColor.a, 1.0);\n        return;\n    }\n    if (kind == 1) {\n        vec3 c = lava(vUv) * vColor.rgb;\n        fragColor = vec4(c + grain, 1.0);\n        return;\n    }\n\n    vec3 n = normalize(vNormal);\n    float lit = shadow();\n\n    if (kind == 3) {\n        // The floor: the colour of the background, lifted where the light pools on it. Slabs of a block with\n        // a seam between them show in the light and are gone in the dark, so the floor has no edge.\n        vec2 q = (vWorld.xz - uPool.xy) / uPool.zw;\n        float pool = exp(-dot(q, q) * 1.5);\n        vec2 cell = floor(vWorld.xz);\n        vec2 within = fract(vWorld.xz);\n        float tone = hash(cell) * 0.16 - 0.08 + hash(floor(vWorld.xz * 16.0)) * 0.06 - 0.03;\n        float seam = (within.x < 0.0625 || within.y < 0.0625) ? -0.22 : 0.0;\n        vec3 slab = uFloorLit * (1.0 + tone + seam);\n        // What stands on the floor takes light from it all round its foot.\n        float off = length(max(abs(vWorld.xz - uStage.xy) - uStage.zw, 0.0));\n        float foot = 1.0 - 0.62 * exp(-off * 2.2);\n        vec3 c = mix(uBackground, slab, pool * (0.22 + 0.78 * lit) * foot);\n        c += slab * lamps(n, 0.0) * 1.6;\n        fragColor = vec4(c + grain, 1.0);\n        return;\n    }\n\n    vec4 tex = texture(uAtlas, vUv);\n    if (tex.a < 0.02) discard;\n    vec3 base = tex.rgb * vColor.rgb;\n    if (kind == 4) {\n        fragColor = vec4(base, tex.a * vColor.a);\n        return;\n    }\n\n    float key = clamp((dot(n, -uKeyDir) + uWrap) / (1.0 + uWrap), 0.0, 1.0);\n    key = key * key * (3.0 - 2.0 * key);\n    key *= mix(0.16, 1.0, lit);\n    float fill = clamp(dot(n, uFillDir) * 0.5 + 0.5, 0.0, 1.0);\n    vec3 light = uAmbient + uKeyColor * key + uFillColor * fill + lamps(n, 1.0);\n\n    vec3 toEye = normalize(uEye - vWorld);\n    float edge = 1.0 - clamp(dot(n, toEye), 0.0, 1.0);\n    float rim = edge * edge * edge;\n    vec3 c = base * light + uRimColor * rim * (0.35 + 0.65 * key);\n\n    vec3 halfway = normalize(toEye - uKeyDir);\n    float shine = pow(max(dot(n, halfway), 0.0), 28.0) * vExtra.z * lit;\n    c += uKeyColor * shine;\n\n    c = mix(c, base * 1.12, vExtra.x);\n    fragColor = vec4(c + grain, tex.a * vColor.a);\n}\n";
   private static final String DEPTH_VERTEX = "#version 150\nin vec3 aPos;\nuniform mat4 uLightViewProj;\nvoid main() {\n    gl_Position = uLightViewProj * vec4(aPos, 1.0);\n}\n";
   private static final String DEPTH_FRAGMENT = "#version 150\nout vec4 fragColor;\nvoid main() {\n    fragColor = vec4(1.0);\n}\n";
   private int program;
   private int depthProgram;
   private int vao;
   private int vbo;
   private int atlas;
   private ByteBuffer frame = MemoryUtil.memAlloc(1048576);
   private int shadowFbo;
   private int shadowMap;
   private int manyFbo;
   private int manyColour;
   private int manyDepth;
   private int flatFbo;
   private int flatColour;
   private int width;
   private int height;
   private int samples;
   private int uViewProj;
   private int uLightViewProj;
   private int uAtlas;
   private int uShadowMap;
   private int uShadowOn;
   private int uShadowTexel;
   private int uEye;
   private int uKeyDir;
   private int uKeyColor;
   private int uFillDir;
   private int uFillColor;
   private int uAmbient;
   private int uRimColor;
   private int uWrap;
   private int uLamp;
   private int uLampColor;
   private int uBackground;
   private int uFloorLit;
   private int uPool;
   private int uStage;
   private int uTime;
   private int uDepthLight;

   private void init() {
      this.program = Gfx.link("#version 150\nin vec3 aPos;\nin vec4 aNormal;\nin vec2 aUv;\nin vec4 aColor;\nin vec4 aExtra;\nuniform mat4 uViewProj;\nuniform mat4 uLightViewProj;\nout vec3 vWorld;\nout vec3 vNormal;\ncentroid out vec2 vUv;\nout vec4 vColor;\nflat out vec3 vExtra;\nout vec4 vShadow;\nvoid main() {\n    vWorld = aPos;\n    vNormal = aNormal.xyz;\n    vUv = aUv;\n    vColor = aColor;\n    vExtra = aExtra.xyz;\n    vShadow = uLightViewProj * vec4(aPos + aNormal.xyz * 0.035, 1.0);\n    gl_Position = uViewProj * vec4(aPos, 1.0);\n}\n", "#version 150\nin vec3 vWorld;\nin vec3 vNormal;\ncentroid in vec2 vUv;\nin vec4 vColor;\nflat in vec3 vExtra;\nin vec4 vShadow;\n\nuniform sampler2D uAtlas;\nuniform sampler2DShadow uShadowMap;\nuniform float uShadowOn;\nuniform float uShadowTexel;\nuniform vec3 uEye;\nuniform vec3 uKeyDir;\nuniform vec3 uKeyColor;\nuniform vec3 uFillDir;\nuniform vec3 uFillColor;\nuniform vec3 uAmbient;\nuniform vec3 uRimColor;\nuniform float uWrap;\nuniform vec4 uLamp[3];\nuniform vec3 uLampColor[3];\nuniform vec3 uBackground;\nuniform vec3 uFloorLit;\nuniform vec4 uPool;\nuniform vec4 uStage;\nuniform float uTime;\n\nout vec4 fragColor;\n\nfloat hash(vec2 p) {\n    vec3 q = fract(vec3(p.xyx) * 0.1031);\n    q += dot(q, q.yzx + 33.33);\n    return fract((q.x + q.y) * q.z);\n}\n\nfloat noise(vec2 p) {\n    vec2 i = floor(p), f = fract(p);\n    f = f * f * (3.0 - 2.0 * f);\n    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);\n}\n\n// How much of the key light gets here: 25 looks at the shadow map round the place, each one smoothed by the\n// driver, so the edge of a shadow is a soft ramp.\nfloat shadow() {\n    if (uShadowOn < 0.5) return 1.0;\n    vec3 p = vShadow.xyz / vShadow.w * 0.5 + 0.5;\n    if (p.x <= 0.0 || p.x >= 1.0 || p.y <= 0.0 || p.y >= 1.0 || p.z >= 1.0) return 1.0;\n    float s = 0.0;\n    for (int x = -2; x <= 2; x++) {\n        for (int y = -2; y <= 2; y++) {\n            s += texture(uShadowMap, vec3(p.xy + vec2(x, y) * uShadowTexel * 1.6, p.z - 0.0012));\n        }\n    }\n    return s / 25.0;\n}\n\nvec3 lamps(vec3 n, float turned) {\n    vec3 sum = vec3(0.0);\n    for (int i = 0; i < 3; i++) {\n        vec3 d = uLamp[i].xyz - vWorld;\n        float far = length(d);\n        float a = clamp(1.0 - far / uLamp[i].w, 0.0, 1.0);\n        float facing = mix(1.0, clamp(dot(n, d / max(far, 0.001)) * 0.5 + 0.5, 0.0, 1.0), turned);\n        sum += uLampColor[i] * a * a * facing;\n    }\n    return sum;\n}\n\n// Lava as the game paints it, texel by texel, but made here: two layers of noise that drift through each other,\n// cut into a few steps of colour.\nvec3 lava(vec2 texel) {\n    vec2 c = floor(texel);\n    float a = noise(c * 0.23 + vec2(0.0, uTime * 0.42));\n    float b = noise(c * 0.51 + vec2(uTime * 0.21, -uTime * 0.17));\n    float n = floor((a * 0.62 + b * 0.38) * 7.0) / 6.0;\n    vec3 dark = vec3(0.66, 0.14, 0.02), mid = vec3(0.98, 0.43, 0.05), hot = vec3(1.0, 0.83, 0.33);\n    return n < 0.5 ? mix(dark, mid, n * 2.0) : mix(mid, hot, (n - 0.5) * 2.0);\n}\n\nvoid main() {\n    int kind = int(vExtra.y * 255.0 / 32.0 + 0.5);\n    float grain = (hash(gl_FragCoord.xy) - 0.5) / 255.0;\n\n    if (kind == 2) {\n        // A glow: strongest in the middle of its quad, nothing at its edge. Added to what is there.\n        float r = length(vUv * 2.0 - 1.0);\n        float a = clamp(1.0 - r, 0.0, 1.0);\n        fragColor = vec4(vColor.rgb * a * a * vColor.a, 1.0);\n        return;\n    }\n    if (kind == 1) {\n        vec3 c = lava(vUv) * vColor.rgb;\n        fragColor = vec4(c + grain, 1.0);\n        return;\n    }\n\n    vec3 n = normalize(vNormal);\n    float lit = shadow();\n\n    if (kind == 3) {\n        // The floor: the colour of the background, lifted where the light pools on it. Slabs of a block with\n        // a seam between them show in the light and are gone in the dark, so the floor has no edge.\n        vec2 q = (vWorld.xz - uPool.xy) / uPool.zw;\n        float pool = exp(-dot(q, q) * 1.5);\n        vec2 cell = floor(vWorld.xz);\n        vec2 within = fract(vWorld.xz);\n        float tone = hash(cell) * 0.16 - 0.08 + hash(floor(vWorld.xz * 16.0)) * 0.06 - 0.03;\n        float seam = (within.x < 0.0625 || within.y < 0.0625) ? -0.22 : 0.0;\n        vec3 slab = uFloorLit * (1.0 + tone + seam);\n        // What stands on the floor takes light from it all round its foot.\n        float off = length(max(abs(vWorld.xz - uStage.xy) - uStage.zw, 0.0));\n        float foot = 1.0 - 0.62 * exp(-off * 2.2);\n        vec3 c = mix(uBackground, slab, pool * (0.22 + 0.78 * lit) * foot);\n        c += slab * lamps(n, 0.0) * 1.6;\n        fragColor = vec4(c + grain, 1.0);\n        return;\n    }\n\n    vec4 tex = texture(uAtlas, vUv);\n    if (tex.a < 0.02) discard;\n    vec3 base = tex.rgb * vColor.rgb;\n    if (kind == 4) {\n        fragColor = vec4(base, tex.a * vColor.a);\n        return;\n    }\n\n    float key = clamp((dot(n, -uKeyDir) + uWrap) / (1.0 + uWrap), 0.0, 1.0);\n    key = key * key * (3.0 - 2.0 * key);\n    key *= mix(0.16, 1.0, lit);\n    float fill = clamp(dot(n, uFillDir) * 0.5 + 0.5, 0.0, 1.0);\n    vec3 light = uAmbient + uKeyColor * key + uFillColor * fill + lamps(n, 1.0);\n\n    vec3 toEye = normalize(uEye - vWorld);\n    float edge = 1.0 - clamp(dot(n, toEye), 0.0, 1.0);\n    float rim = edge * edge * edge;\n    vec3 c = base * light + uRimColor * rim * (0.35 + 0.65 * key);\n\n    vec3 halfway = normalize(toEye - uKeyDir);\n    float shine = pow(max(dot(n, halfway), 0.0), 28.0) * vExtra.z * lit;\n    c += uKeyColor * shine;\n\n    c = mix(c, base * 1.12, vExtra.x);\n    fragColor = vec4(c + grain, tex.a * vColor.a);\n}\n", "aPos", "aNormal", "aUv", "aColor", "aExtra");
      this.depthProgram = Gfx.link("#version 150\nin vec3 aPos;\nuniform mat4 uLightViewProj;\nvoid main() {\n    gl_Position = uLightViewProj * vec4(aPos, 1.0);\n}\n", "#version 150\nout vec4 fragColor;\nvoid main() {\n    fragColor = vec4(1.0);\n}\n", "aPos");
      this.uViewProj = GL32C.glGetUniformLocation(this.program, "uViewProj");
      this.uLightViewProj = GL32C.glGetUniformLocation(this.program, "uLightViewProj");
      this.uAtlas = GL32C.glGetUniformLocation(this.program, "uAtlas");
      this.uShadowMap = GL32C.glGetUniformLocation(this.program, "uShadowMap");
      this.uShadowOn = GL32C.glGetUniformLocation(this.program, "uShadowOn");
      this.uShadowTexel = GL32C.glGetUniformLocation(this.program, "uShadowTexel");
      this.uEye = GL32C.glGetUniformLocation(this.program, "uEye");
      this.uKeyDir = GL32C.glGetUniformLocation(this.program, "uKeyDir");
      this.uKeyColor = GL32C.glGetUniformLocation(this.program, "uKeyColor");
      this.uFillDir = GL32C.glGetUniformLocation(this.program, "uFillDir");
      this.uFillColor = GL32C.glGetUniformLocation(this.program, "uFillColor");
      this.uAmbient = GL32C.glGetUniformLocation(this.program, "uAmbient");
      this.uRimColor = GL32C.glGetUniformLocation(this.program, "uRimColor");
      this.uWrap = GL32C.glGetUniformLocation(this.program, "uWrap");
      this.uLamp = GL32C.glGetUniformLocation(this.program, "uLamp");
      this.uLampColor = GL32C.glGetUniformLocation(this.program, "uLampColor");
      this.uBackground = GL32C.glGetUniformLocation(this.program, "uBackground");
      this.uFloorLit = GL32C.glGetUniformLocation(this.program, "uFloorLit");
      this.uPool = GL32C.glGetUniformLocation(this.program, "uPool");
      this.uStage = GL32C.glGetUniformLocation(this.program, "uStage");
      this.uTime = GL32C.glGetUniformLocation(this.program, "uTime");
      this.uDepthLight = GL32C.glGetUniformLocation(this.depthProgram, "uLightViewProj");
      this.vao = GL32C.glGenVertexArrays();
      this.vbo = GL32C.glGenBuffers();
      GL32C.glBindVertexArray(this.vao);
      GL32C.glBindBuffer(34962, this.vbo);
      GL32C.glEnableVertexAttribArray(0);
      GL32C.glVertexAttribPointer(0, 3, 5126, false, 32, 0L);
      GL32C.glEnableVertexAttribArray(1);
      GL32C.glVertexAttribPointer(1, 4, 5120, true, 32, 12L);
      GL32C.glEnableVertexAttribArray(2);
      GL32C.glVertexAttribPointer(2, 2, 5126, false, 32, 16L);
      GL32C.glEnableVertexAttribArray(3);
      GL32C.glVertexAttribPointer(3, 4, 5121, true, 32, 24L);
      GL32C.glEnableVertexAttribArray(4);
      GL32C.glVertexAttribPointer(4, 4, 5121, true, 32, 28L);
      this.atlas = GL32C.glGenTextures();
      GL32C.glBindTexture(3553, this.atlas);
      GL32C.glTexParameteri(3553, 10241, 9728);
      GL32C.glTexParameteri(3553, 10240, 9728);
      GL32C.glTexParameteri(3553, 10242, 33071);
      GL32C.glTexParameteri(3553, 10243, 33071);
      this.shadowMap = GL32C.glGenTextures();
      GL32C.glBindTexture(3553, this.shadowMap);
      GL32C.glTexImage2D(3553, 0, 33190, 2048, 2048, 0, 6402, 5126, (ByteBuffer)null);
      GL32C.glTexParameteri(3553, 10241, 9729);
      GL32C.glTexParameteri(3553, 10240, 9729);
      GL32C.glTexParameteri(3553, 10242, 33071);
      GL32C.glTexParameteri(3553, 10243, 33071);
      GL32C.glTexParameteri(3553, 34892, 34894);
      GL32C.glTexParameteri(3553, 34893, 515);
      this.shadowFbo = GL32C.glGenFramebuffers();
      GL32C.glBindFramebuffer(36160, this.shadowFbo);
      GL32C.glFramebufferTexture2D(36160, 36096, 3553, this.shadowMap, 0);
      GL32C.glDrawBuffer(0);
      GL32C.glReadBuffer(0);
      complete("shadow");
   }

   private static void complete(String what) {
      int status = GL32C.glCheckFramebufferStatus(36160);
      if (status != 36053) {
         throw new IllegalStateException(what + " target incomplete: 0x" + Integer.toHexString(status));
      }
   }

   private void size(int w, int h) {
      if (w != this.width || h != this.height || this.manyFbo == 0) {
         this.dropTargets();
         this.width = w;
         this.height = h;
         this.samples = Math.max(1, Math.min(4, GL32C.glGetInteger(36183)));
         this.manyColour = GL32C.glGenRenderbuffers();
         GL32C.glBindRenderbuffer(36161, this.manyColour);
         GL32C.glRenderbufferStorageMultisample(36161, this.samples, 32856, w, h);
         this.manyDepth = GL32C.glGenRenderbuffers();
         GL32C.glBindRenderbuffer(36161, this.manyDepth);
         GL32C.glRenderbufferStorageMultisample(36161, this.samples, 33190, w, h);
         this.manyFbo = GL32C.glGenFramebuffers();
         GL32C.glBindFramebuffer(36160, this.manyFbo);
         GL32C.glFramebufferRenderbuffer(36160, 36064, 36161, this.manyColour);
         GL32C.glFramebufferRenderbuffer(36160, 36096, 36161, this.manyDepth);
         complete("scene");
         this.flatColour = GL32C.glGenTextures();
         GL32C.glBindTexture(3553, this.flatColour);
         GL32C.glTexImage2D(3553, 0, 32856, w, h, 0, 6408, 5121, (ByteBuffer)null);
         GL32C.glTexParameteri(3553, 10241, 9729);
         GL32C.glTexParameteri(3553, 10240, 9729);
         GL32C.glTexParameteri(3553, 10242, 33071);
         GL32C.glTexParameteri(3553, 10243, 33071);
         this.flatFbo = GL32C.glGenFramebuffers();
         GL32C.glBindFramebuffer(36160, this.flatFbo);
         GL32C.glFramebufferTexture2D(36160, 36064, 3553, this.flatColour, 0);
         complete("picture");
      }
   }

   private void dropTargets() {
      if (this.manyFbo != 0) {
         GL32C.glDeleteFramebuffers(this.manyFbo);
         GL32C.glDeleteFramebuffers(this.flatFbo);
         GL32C.glDeleteRenderbuffers(this.manyColour);
         GL32C.glDeleteRenderbuffers(this.manyDepth);
         GL32C.glDeleteTextures(this.flatColour);
         this.manyFbo = 0;
      }
   }

   void delete() {
      if (this.frame != null) {
         MemoryUtil.memFree(this.frame);
      }

      this.frame = null;
      if (this.program != 0) {
         this.dropTargets();
         GL32C.glDeleteProgram(this.program);
         GL32C.glDeleteProgram(this.depthProgram);
         GL32C.glDeleteVertexArrays(this.vao);
         GL32C.glDeleteBuffers(this.vbo);
         GL32C.glDeleteTextures(this.atlas);
         GL32C.glDeleteTextures(this.shadowMap);
         GL32C.glDeleteFramebuffers(this.shadowFbo);
         this.program = 0;
      }
   }

   private void send(Sheet sheet) {
      int[] argb = sheet.pixels();
      ByteBuffer bytes = MemoryUtil.memAlloc(argb.length * 4);

      try {
         for(int c : argb) {
            bytes.put((byte)(c >>> 16)).put((byte)(c >>> 8)).put((byte)c).put((byte)(c >>> 24));
         }

         bytes.flip();
         GL32C.glBindTexture(3553, this.atlas);
         GlSave.plainUnpack();
         GL32C.glTexImage2D(3553, 0, 32856, 1024, 1024, 0, 6408, 5121, bytes);
      } finally {
         MemoryUtil.memFree(bytes);
      }

   }

   void begin(int w, int h, Sheet sheet) {
      if (this.program == 0) {
         this.init();
      }

      if (sheet.takeChanged()) {
         this.send(sheet);
      }

      this.size(w, h);
      GL32C.glBindVertexArray(this.vao);
      GL32C.glBindBuffer(34962, this.vbo);
      GL32C.glDisable(3089);
      GL32C.glDisable(2960);
      GL32C.glEnable(32925);
      GL32C.glColorMask(true, true, true, true);
      GL32C.glFrontFace(2305);
      GL32C.glCullFace(1029);
      GL32C.glBlendEquationSeparate(32774, 32774);
      GL32C.glDepthFunc(515);
   }

   void load(Mesh... meshes) {
      int bytes = 0;

      for(Mesh m : meshes) {
         bytes += m.vertices() * 32;
      }

      if (bytes > this.frame.capacity()) {
         MemoryUtil.memFree(this.frame);
         this.frame = MemoryUtil.memAlloc(Math.max(bytes, this.frame.capacity() * 2));
      }

      this.frame.clear();
      int first = 0;

      for(Mesh m : meshes) {
         m.first = first;
         this.frame.put(m.data());
         first += m.vertices();
      }

      this.frame.flip();
      GL32C.glBufferData(34962, this.frame, 35040);
   }

   void shadows(Mesh casters, float[] lightViewProj) {
      GL32C.glBindFramebuffer(36160, this.shadowFbo);
      GL32C.glViewport(0, 0, 2048, 2048);
      GL32C.glEnable(2929);
      GL32C.glDepthMask(true);
      GL32C.glClearDepth((double)1.0F);
      GL32C.glClear(256);
      GL32C.glDisable(3042);
      GL32C.glDisable(2884);
      GL32C.glEnable(32823);
      GL32C.glPolygonOffset(1.5F, 3.0F);
      GL32C.glUseProgram(this.depthProgram);
      GL32C.glUniformMatrix4fv(this.uDepthLight, false, lightViewProj);
      GL32C.glDrawArrays(4, casters.first, casters.vertices());
      GL32C.glDisable(32823);
   }

   void picture(float[] background) {
      GL32C.glBindFramebuffer(36160, this.manyFbo);
      GL32C.glViewport(0, 0, this.width, this.height);
      GL32C.glDepthMask(true);
      GL32C.glClearColor(background[0], background[1], background[2], 1.0F);
      GL32C.glClearDepth((double)1.0F);
      GL32C.glClear(16640);
      GL32C.glUseProgram(this.program);
      GL32C.glUniform1i(this.uAtlas, 0);
      GL32C.glUniform1i(this.uShadowMap, 1);
      GL32C.glActiveTexture(33985);
      GL32C.glBindTexture(3553, this.shadowMap);
      GL32C.glActiveTexture(33984);
      GL32C.glBindTexture(3553, this.atlas);
   }

   void inFront() {
      GL32C.glDepthMask(true);
      GL32C.glClear(256);
   }

   void view(float[] viewProj, float[] eye, Light light, float[] lightViewProj, boolean shadowed) {
      GL32C.glUniformMatrix4fv(this.uViewProj, false, viewProj);
      GL32C.glUniformMatrix4fv(this.uLightViewProj, false, lightViewProj);
      GL32C.glUniform1f(this.uShadowOn, shadowed ? 1.0F : 0.0F);
      GL32C.glUniform1f(this.uShadowTexel, 4.8828125E-4F);
      GL32C.glUniform3f(this.uEye, eye[0], eye[1], eye[2]);
      GL32C.glUniform3f(this.uKeyDir, light.keyDir[0], light.keyDir[1], light.keyDir[2]);
      GL32C.glUniform3f(this.uKeyColor, light.keyColour[0], light.keyColour[1], light.keyColour[2]);
      GL32C.glUniform3f(this.uFillDir, light.fillDir[0], light.fillDir[1], light.fillDir[2]);
      GL32C.glUniform3f(this.uFillColor, light.fillColour[0], light.fillColour[1], light.fillColour[2]);
      GL32C.glUniform3f(this.uAmbient, light.ambient[0], light.ambient[1], light.ambient[2]);
      GL32C.glUniform3f(this.uRimColor, light.rim[0], light.rim[1], light.rim[2]);
      GL32C.glUniform1f(this.uWrap, light.wrap);
      GL32C.glUniform4fv(this.uLamp, light.lamp);
      GL32C.glUniform3fv(this.uLampColor, light.lampColour);
      GL32C.glUniform3f(this.uBackground, light.background[0], light.background[1], light.background[2]);
      GL32C.glUniform3f(this.uFloorLit, light.floorLit[0], light.floorLit[1], light.floorLit[2]);
      GL32C.glUniform4f(this.uPool, light.pool[0], light.pool[1], light.pool[2], light.pool[3]);
      GL32C.glUniform4f(this.uStage, light.stage[0], light.stage[1], light.stage[2], light.stage[3]);
      GL32C.glUniform1f(this.uTime, light.time);
   }

   void camera(float[] viewProj) {
      GL32C.glUniformMatrix4fv(this.uViewProj, false, viewProj);
   }

   void draw(Mesh mesh, int how) {
      if (mesh.vertices() != 0) {
         GL32C.glEnable(2929);
         switch (how) {
            case 0:
               GL32C.glDepthMask(true);
               GL32C.glDisable(3042);
               GL32C.glEnable(2884);
               break;
            case 1:
               GL32C.glDepthMask(false);
               GL32C.glEnable(3042);
               GL32C.glBlendFuncSeparate(770, 771, 1, 771);
               GL32C.glDisable(2884);
               break;
            case 2:
            default:
               GL32C.glDepthMask(false);
               GL32C.glEnable(3042);
               GL32C.glBlendFuncSeparate(1, 1, 0, 1);
               GL32C.glDisable(2884);
               break;
            case 3:
               GL32C.glDepthMask(true);
               GL32C.glDisable(3042);
               GL32C.glDisable(2884);
         }

         GL32C.glDrawArrays(4, mesh.first, mesh.vertices());
      }
   }

   int finish() {
      GL32C.glBindFramebuffer(36008, this.manyFbo);
      GL32C.glBindFramebuffer(36009, this.flatFbo);
      GL32C.glBlitFramebuffer(0, 0, this.width, this.height, 0, 0, this.width, this.height, 16384, 9728);
      return this.flatColour;
   }

   int atlas() {
      return this.atlas;
   }

   static final class Light {
      final float[] keyDir = new float[]{0.64F, -0.72F, -0.16F};
      final float[] keyColour = new float[]{1.02F, 0.94F, 0.8F};
      final float[] fillDir = new float[]{0.35F, 0.25F, 0.9F};
      final float[] fillColour = new float[]{0.2F, 0.24F, 0.32F};
      final float[] ambient = new float[]{0.27F, 0.27F, 0.3F};
      final float[] rim = new float[]{0.5F, 0.56F, 0.66F};
      float wrap = 0.45F;
      final float[] lamp = new float[12];
      final float[] lampColour = new float[9];
      final float[] background = new float[3];
      final float[] floorLit = new float[3];
      final float[] pool = new float[]{0.0F, 0.0F, 9.0F, 4.0F};
      final float[] stage = new float[]{0.0F, 0.0F, 0.0F, 0.0F};
      float time;
   }
}
