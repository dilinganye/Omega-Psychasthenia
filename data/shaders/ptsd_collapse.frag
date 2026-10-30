#version 120
uniform sampler2D image;
uniform vec2 texel;
uniform vec3 lightDirection;
uniform int mode;
varying vec2 uv;
varying vec4 tint;
float density(vec2 p) {
    vec4 c = texture2D(image, p);
    return c.a * dot(c.rgb, vec3(0.299, 0.587, 0.114));
}
void main() {
    vec4 c = texture2D(image, uv);
    float h = density(uv);
    float mask = h * tint.a;
    if (mode == 1) { gl_FragColor = vec4(vec3(mask), 1.0); return; }
    if (mode == 2) { gl_FragColor = vec4(vec3(1.0 - mask * 0.8), 1.0); return; }
    if (mode == 3) { gl_FragColor = c * tint; return; }
    float dx = density(uv + vec2(texel.x,0.0)) - density(uv - vec2(texel.x,0.0));
    float dy = density(uv + vec2(0.0,texel.y)) - density(uv - vec2(0.0,texel.y));
    vec3 normal = normalize(vec3(-dx * 8.0, -dy * 8.0, 0.35));
    float light = 0.52 + 0.65 * max(0.0, dot(normal, normalize(lightDirection)));
    // A low-density luminous body sits under the noisy wave crest.
    if (mode == 4) mask = (0.16 + 0.84 * h) * tint.a;
    gl_FragColor = vec4(tint.rgb * light, mask);
}
