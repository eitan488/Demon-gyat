import json, math, os, sys
from PIL import Image, ImageDraw, ImageFilter

OUT = sys.argv[1]
SPR = os.path.join(OUT, "assets/demon/textures/gui/sprites")
os.makedirs(SPR, exist_ok=True)
SS = 4  # supersampling for anti-aliasing

def save(img, name, meta=None):
    img.save(os.path.join(SPR, name + ".png"))
    if meta:
        with open(os.path.join(SPR, name + ".png.mcmeta"), "w") as f:
            json.dump(meta, f)

def rounded(size, radius, outline=0):
    big = Image.new("L", (size * SS, size * SS), 0)
    d = ImageDraw.Draw(big)
    if outline:
        d.rounded_rectangle([0, 0, size * SS - 1, size * SS - 1], radius * SS, fill=255)
        o = outline * SS
        d.rounded_rectangle([o, o, size * SS - 1 - o, size * SS - 1 - o], max(0, (radius - outline)) * SS, fill=0)
    else:
        d.rounded_rectangle([0, 0, size * SS - 1, size * SS - 1], radius * SS, fill=255)
    a = big.resize((size, size), Image.LANCZOS)
    img = Image.new("RGBA", (size, size), (255, 255, 255, 0))
    img.putalpha(a)
    return img

DENS = 4  # texels per GUI pixel
for name, logical, border in [("round_s", 8, 3), ("round_m", 16, 6), ("round_l", 28, 12)]:
    tex = logical * DENS
    save(rounded(tex, border * DENS), name,
         {"gui": {"scaling": {"type": "nine_slice", "width": logical, "height": logical, "border": border}}})
    save(rounded(tex, border * DENS, outline=DENS), name + "_outline",
         {"gui": {"scaling": {"type": "nine_slice", "width": logical, "height": logical, "border": border}}})

# soft shadow / glow: a rounded rect blurred, padded by `pad` logical px each side
logical, pad = 64, 16
tex = logical * DENS
base = Image.new("L", (tex, tex), 0)
d = ImageDraw.Draw(base)
p = pad * DENS
d.rounded_rectangle([p, p, tex - p - 1, tex - p - 1], 8 * DENS, fill=255)
base = base.filter(ImageFilter.GaussianBlur(6 * DENS))
img = Image.new("RGBA", (tex, tex), (255, 255, 255, 0)); img.putalpha(base)
save(img, "shadow", {"gui": {"scaling": {"type": "nine_slice", "width": logical, "height": logical, "border": 24}}})

# circle
c = 64
big = Image.new("L", (c * SS, c * SS), 0)
ImageDraw.Draw(big).ellipse([0, 0, c * SS - 1, c * SS - 1], fill=255)
img = Image.new("RGBA", (c, c), (255, 255, 255, 0)); img.putalpha(big.resize((c, c), Image.LANCZOS))
save(img, "circle", {"gui": {"scaling": {"type": "stretch"}}})

# ring
big = Image.new("L", (c * SS, c * SS), 0)
dd = ImageDraw.Draw(big)
dd.ellipse([0, 0, c * SS - 1, c * SS - 1], fill=255)
w = 5 * SS
dd.ellipse([w, w, c * SS - 1 - w, c * SS - 1 - w], fill=0)
img = Image.new("RGBA", (c, c), (255, 255, 255, 0)); img.putalpha(big.resize((c, c), Image.LANCZOS))
save(img, "ring", {"gui": {"scaling": {"type": "stretch"}}})

# soft radial glow (particles / bloom blobs)
g = 128
glow = Image.new("L", (g, g), 0)
px = glow.load()
for y in range(g):
    for x in range(g):
        r = math.hypot(x - g / 2 + 0.5, y - g / 2 + 0.5) / (g / 2)
        v = max(0.0, 1.0 - r)
        px[x, y] = int(255 * (v ** 2.2))
img = Image.new("RGBA", (g, g), (255, 255, 255, 0)); img.putalpha(glow)
save(img, "glow", {"gui": {"scaling": {"type": "stretch"}}})

# horizontal fade (for light sweeps / scanlines): alpha 0 -> 255 -> 0
fw = 128
fade = Image.new("L", (fw, 4), 0)
px = fade.load()
for x in range(fw):
    t = x / (fw - 1)
    v = math.sin(t * math.pi) ** 2
    for y in range(4):
        px[x, y] = int(255 * v)
img = Image.new("RGBA", (fw, 4), (255, 255, 255, 0)); img.putalpha(fade)
save(img, "sweep", {"gui": {"scaling": {"type": "stretch"}}})

# vertical gradient white->transparent (top to bottom) for fades
vg = Image.new("L", (4, 128), 0)
px = vg.load()
for y in range(128):
    for x in range(4):
        px[x, y] = int(255 * (1 - y / 127))
img = Image.new("RGBA", (4, 128), (255, 255, 255, 0)); img.putalpha(vg)
save(img, "fade_v", {"gui": {"scaling": {"type": "stretch"}}})

# ---------- mod icon ----------
S = 512
icon = Image.new("RGBA", (S, S), (0, 0, 0, 0))
grad = Image.new("RGBA", (S, S))
gp = grad.load()
c1, c2 = (124, 58, 237), (236, 72, 153)  # violet -> pink
for y in range(S):
    for x in range(S):
        t = (x + y) / (2 * S - 2)
        gp[x, y] = tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(3)) + (255,)
mask = Image.new("L", (S, S), 0)
ImageDraw.Draw(mask).rounded_rectangle([0, 0, S - 1, S - 1], 110, fill=255)
bg = Image.new("RGBA", (S, S), (14, 10, 24, 255))
icon.paste(bg, (0, 0), mask)
# inner gradient border
inner = Image.new("L", (S, S), 0)
di = ImageDraw.Draw(inner)
di.rounded_rectangle([0, 0, S - 1, S - 1], 110, fill=255)
di.rounded_rectangle([18, 18, S - 19, S - 19], 94, fill=0)
icon.paste(grad, (0, 0), inner)
# demon horns + D glyph
shape = Image.new("L", (S, S), 0)
ds = ImageDraw.Draw(shape)
# horns
ds.polygon([(150, 215), (95, 70), (205, 175)], fill=255)
ds.polygon([(362, 215), (417, 70), (307, 175)], fill=255)
# D letter
ds.rounded_rectangle([150, 170, 380, 420], 115, fill=255)
ds.rectangle([150, 170, 250, 420], fill=255)
ds.rounded_rectangle([215, 230, 320, 360], 60, fill=0)
ds.rectangle([215, 230, 250, 360], fill=0)
glowm = shape.filter(ImageFilter.GaussianBlur(22))
gl = Image.new("RGBA", (S, S), (236, 72, 153, 0)); gl.putalpha(glowm.point(lambda v: int(v * 0.8)))
icon = Image.alpha_composite(icon, gl)
icon.paste(grad, (0, 0), shape)
icon = icon.resize((128, 128), Image.LANCZOS)
os.makedirs(os.path.join(OUT, "assets/demon"), exist_ok=True)
icon.save(os.path.join(OUT, "assets/demon/icon.png"))
print("ok")
