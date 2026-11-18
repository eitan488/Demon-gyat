import json, math, os, sys
from PIL import Image, ImageDraw
OUT = os.path.join(sys.argv[1], "assets/demon/textures/gui/sprites")
N = 64; SS = 8; B = N * SS
meta = {"gui": {"scaling": {"type": "stretch"}}}
def canvas():
    img = Image.new("L", (B, B), 0)
    return img, ImageDraw.Draw(img)
def S(v): return v * SS
def save(img, name):
    a = img.resize((N, N), Image.LANCZOS)
    out = Image.new("RGBA", (N, N), (255, 255, 255, 0)); out.putalpha(a)
    out.save(os.path.join(OUT, "icon_" + name + ".png"))
    json.dump(meta, open(os.path.join(OUT, "icon_" + name + ".png.mcmeta"), "w"))
def ring(d, cx, cy, r, w):
    d.ellipse([S(cx - r), S(cy - r), S(cx + r), S(cy + r)], outline=255, width=S(w))
def line(d, x1, y1, x2, y2, w):
    d.line([S(x1), S(y1), S(x2), S(y2)], fill=255, width=S(w))
    for x, y in ((x1, y1), (x2, y2)):
        d.ellipse([S(x - w / 2), S(y - w / 2), S(x + w / 2), S(y + w / 2)], fill=255)
def dot(d, cx, cy, r):
    d.ellipse([S(cx - r), S(cy - r), S(cx + r), S(cy + r)], fill=255)

# crosshair (hunting)
img, d = canvas(); ring(d, 32, 32, 20, 6); dot(d, 32, 32, 5)
for a in range(4):
    ang = a * math.pi / 2
    line(d, 32 + math.cos(ang) * 22, 32 + math.sin(ang) * 22, 32 + math.cos(ang) * 30, 32 + math.sin(ang) * 30, 6)
save(img, "hunting")
# eye (render)
img, d = canvas()
pts = []
for i in range(0, 181):
    t = math.radians(i); pts.append((S(32 - 28 * math.cos(t)), S(32 - 17 * math.sin(t))))
for i in range(0, 181):
    t = math.radians(i); pts.append((S(32 + 28 * math.cos(t)), S(32 + 17 * math.sin(t))))
d.polygon(pts, outline=255, width=S(6)); dot(d, 32, 32, 9)
save(img, "render")
# gear (client / settings)
def gear(d):
    teeth = 8
    for k in range(teeth):
        ang = k * 2 * math.pi / teeth
        cx, cy = 32 + math.cos(ang) * 24, 32 + math.sin(ang) * 24
        poly = []
        for dx, dy in ((-5, -5), (5, -5), (5, 5), (-5, 5)):
            rx = dx * math.cos(ang) - dy * math.sin(ang); ry = dx * math.sin(ang) + dy * math.cos(ang)
            poly.append((S(cx + rx), S(cy + ry)))
        d.polygon(poly, fill=255)
    dot(d, 32, 32, 21)
    d.ellipse([S(32 - 9), S(32 - 9), S(32 + 9), S(32 + 9)], fill=0)
img, d = canvas(); gear(d); save(img, "client"); save(img, "settings")
# play triangle (singleplayer)
img, d = canvas(); d.polygon([(S(20), S(12)), (S(52), S(32)), (S(20), S(52))], fill=255); save(img, "play")
# globe (multiplayer)
img, d = canvas(); ring(d, 32, 32, 24, 5)
d.ellipse([S(32 - 10), S(8), S(32 + 10), S(56)], outline=255, width=S(5))
line(d, 9, 32, 55, 32, 5); line(d, 14, 20, 50, 20, 4); line(d, 14, 44, 50, 44, 4)
save(img, "multiplayer")
# sparkle (demon menu)
img, d = canvas()
pts = []
for k in range(8):
    ang = k * math.pi / 4 - math.pi / 2
    r = 28 if k % 2 == 0 else 8
    pts.append((S(32 + math.cos(ang) * r), S(32 + math.sin(ang) * r)))
d.polygon(pts, fill=255); save(img, "sparkle")
# power (quit)
img, d = canvas()
d.arc([S(10), S(12), S(54), S(56)], start=-55, end=235, fill=255, width=S(6))
line(d, 32, 6, 32, 30, 6); save(img, "power")
# chunk grid (chunk finder)
img, d = canvas()
d.rounded_rectangle([S(8), S(8), S(56), S(56)], radius=S(8), outline=255, width=S(5))
line(d, 24, 10, 24, 54, 4); line(d, 40, 10, 40, 54, 4); line(d, 10, 24, 54, 24, 4); line(d, 10, 40, 54, 40, 4)
d.rectangle([S(26), S(26), S(38), S(38)], fill=255); save(img, "chunk")
# cube (esp)
img, d = canvas()
top = [(32, 6), (56, 18), (32, 30), (8, 18)]
d.polygon([(S(x), S(y)) for x, y in top], outline=255, width=S(5))
line(d, 8, 18, 8, 44, 5); line(d, 56, 18, 56, 44, 5); line(d, 32, 30, 32, 58, 5)
line(d, 8, 44, 32, 58, 5); line(d, 56, 44, 32, 58, 5)
save(img, "cube")
# camera (freecam)
img, d = canvas()
d.rounded_rectangle([S(6), S(18), S(58), S(54)], radius=S(8), outline=255, width=S(5))
d.rounded_rectangle([S(20), S(10), S(36), S(20)], radius=S(3), fill=255)
ring(d, 32, 36, 10, 5); save(img, "camera")
# layout (hud)
img, d = canvas()
d.rounded_rectangle([S(6), S(10), S(58), S(54)], radius=S(7), outline=255, width=S(5))
d.rounded_rectangle([S(14), S(18), S(30), S(26)], radius=S(2), fill=255)
d.rounded_rectangle([S(38), S(18), S(50), S(22)], radius=S(2), fill=255)
d.rounded_rectangle([S(38), S(26), S(50), S(30)], radius=S(2), fill=255)
d.rounded_rectangle([S(14), S(40), S(36), S(46)], radius=S(2), fill=255)
save(img, "hud")
# palette (interface)
img, d = canvas()
d.ellipse([S(6), S(6), S(58), S(58)], outline=255, width=S(5))
for cx, cy in ((22, 22), (38, 18), (46, 32), (20, 38)):
    dot(d, cx, cy, 5)
d.ellipse([S(32), S(38), S(48), S(54)], fill=0)
d.ellipse([S(34), S(40), S(46), S(52)], outline=255, width=S(4))
save(img, "palette")
print("icons ok")
