#version 150

// Slate stage: soft, stylized light for the things of a scene. Works on positions that still have to be moved by
// ModelViewMat (meshes) and on ones that already were (models posed on the CPU, where ModelViewMat is identity).

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 viewPos;
out vec3 viewNormal;
out vec3 localPos;
out vec4 vertexColor;
out vec2 texCoord0;
out float blockLight;
out float receivedLight;

void main() {
    vec4 p = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * p;
    viewPos = p.xyz;
    viewNormal = mat3(ModelViewMat) * Normal;
    // Where on its own mesh the vertex lies: what a footprint (see the fragment shader) is measured in.
    localPos = Position;
    vertexColor = Color;
    texCoord0 = UV0;
    // The block half of the light coordinates, 0..1: how much light of its own the thing has (see Emissive).
    blockLight = clamp(float(UV2.x) / 240.0, 0.0, 1.0);
    // The other half, 0..1: the light a build's own torches and lanterns throw on it (see Received). Only a build's
    // meshes write it; anything else is drawn with Received at nothing, whatever it carries there.
    receivedLight = clamp(float(UV2.y) / 240.0, 0.0, 1.0);
}
