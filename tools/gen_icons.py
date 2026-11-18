import json, math, os, sys
from PIL import Image, ImageDraw
OUT = os.path.join(sys.argv[1], "assets/demon/textures/hd")
os.makedirs(OUT, exist_ok=True)
N = 256  # output size
B = 64 * 16  # icons are designed in a 64-unit space and drawn at 16x before downsampling
meta = {"gui": {"scaling": {"type": "stretch"}}}
def canvas():
    img = Image.new("L", (B, B), 0)
    return img, ImageDraw.Draw(img)
def S(v): return v * 16
def save(img, name):
    a = img.resize((N, N), Image.LANCZOS)
    out = Image.new("RGBA", (N, N), (255, 255, 255, 0)); out.putalpha(a)
    out.save(os.path.join(OUT, "icon_" + name + ".png"))
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
# search (magnifier)
img, d = canvas(); ring(d, 27, 27, 17, 6); line(d, 40, 40, 55, 55, 7); save(img, "search")
# chevrons
img, d = canvas(); line(d, 24, 14, 42, 32, 7); line(d, 42, 32, 24, 50, 7); save(img, "chevron_right")
img, d = canvas(); line(d, 14, 24, 32, 42, 7); line(d, 32, 42, 50, 24, 7); save(img, "chevron_down")
# location pin with orbit (auto rtp)
img, d = canvas()
d.ellipse([S(18), S(6), S(46), S(34)], fill=255)
d.polygon([(S(19.5), S(25)), (S(44.5), S(25)), (S(32), S(46))], fill=255)
d.ellipse([S(26), S(14), S(38), S(26)], fill=0)
d.ellipse([S(6), S(44), S(58), S(60)], outline=255, width=S(4))
save(img, "rtp")
# timer
img, d = canvas(); ring(d, 32, 36, 22, 6); line(d, 32, 36, 32, 22, 6); line(d, 26, 6, 38, 6, 6); line(d, 50, 16, 54, 20, 5)
save(img, "timer")
# ---- v1.2 icons
def poly(d, pts, fill=255): d.polygon([(S(x), S(y)) for x, y in pts], fill=fill)
img, d = canvas(); poly(d, [(46,6),(58,6),(58,18),(26,50),(14,38)]); line(d, 10, 34, 30, 54, 6); line(d, 12, 52, 6, 58, 7); save(img, "combat")
img, d = canvas(); d.polygon([(S(32),S(6)),(S(54),S(14)),(S(52),S(34)),(S(32),S(58)),(S(12),S(34)),(S(10),S(14))], outline=255, width=S(5)); line(d, 32, 14, 32, 48, 4); save(img, "shield")
img, d = canvas(); dot(d, 22, 24, 13); dot(d, 42, 24, 13); poly(d, [(10,30),(54,30),(32,56)]); save(img, "heart")
img, d = canvas(); d.polygon([(S(32),S(4)),(S(54),S(26)),(S(32),S(60)),(S(10),S(26))], outline=255, width=S(5)); line(d, 10, 26, 54, 26, 4); line(d, 24, 26, 32, 58, 3); line(d, 40, 26, 32, 58, 3); save(img, "crystal")
img, d = canvas(); dot(d, 32, 18, 11); d.rounded_rectangle([S(12), S(34), S(52), S(60)], radius=S(12), fill=255); save(img, "player")
img, d = canvas(); line(d, 8, 22, 34, 22, 6); line(d, 4, 34, 30, 34, 6); line(d, 12, 46, 36, 46, 6); poly(d, [(38,12),(60,32),(38,52)]); save(img, "movement")
img, d = canvas(); ring(d, 32, 32, 26, 5); poly(d, [(32,10),(40,32),(32,30),(24,32)]); d.polygon([(S(32),S(54)),(S(40),S(32)),(S(32),S(34)),(S(24),S(32))], outline=255, width=S(3)); save(img, "compass")
img, d = canvas(); ring(d, 32, 32, 26, 4); ring(d, 32, 32, 15, 4); line(d, 32, 32, 50, 14, 5); dot(d, 42, 40, 4); save(img, "radar")
img, d = canvas()
for (x, y, w) in ((6,10,14),(25,10,14),(44,10,14),(6,28,14),(25,28,14),(44,28,14),(12,46,40)):
    d.rounded_rectangle([S(x), S(y), S(x+w), S(y+13)], radius=S(3), fill=255)
save(img, "keyboard")
img, d = canvas(); line(d, 14, 6, 14, 60, 6); poly(d, [(16,8),(54,16),(16,34)]); save(img, "flag")
img, d = canvas(); dot(d, 32, 28, 22); d.rectangle([S(20), S(40), S(44), S(58)], fill=255); dot(d, 23, 28, 6, ) if False else None
d.ellipse([S(17), S(22), S(29), S(34)], fill=0); d.ellipse([S(35), S(22), S(47), S(34)], fill=0); line(d, 28, 52, 28, 58, 3); line(d, 36, 52, 36, 58, 3); save(img, "skull")
img, d = canvas(); poly(d, [(38,4),(12,36),(30,36),(24,60),(52,26),(34,26)]); save(img, "bolt")
img, d = canvas(); dot(d, 32, 32, 12)
import math as _m
for k in range(8):
    a = k * _m.pi / 4; line(d, 32 + _m.cos(a) * 20, 32 + _m.sin(a) * 20, 32 + _m.cos(a) * 28, 32 + _m.sin(a) * 28, 5)
save(img, "sun")
img, d = canvas(); d.rounded_rectangle([S(14), S(22), S(50), S(42)], radius=S(6), fill=255); line(d, 24, 8, 24, 22, 6); line(d, 40, 8, 40, 22, 6); line(d, 32, 42, 32, 58, 6); save(img, "plug")
img, d = canvas(); d.rounded_rectangle([S(6), S(10), S(58), S(40)], radius=S(4), outline=255, width=S(5)); line(d, 32, 40, 32, 60, 6); line(d, 16, 22, 48, 22, 3); line(d, 16, 30, 40, 30, 3); save(img, "sign")
img, d = canvas(); d.rectangle([S(6), S(6), S(58), S(58)], outline=255, width=S(6)); d.rectangle([S(18), S(18), S(46), S(46)], outline=255, width=S(4)); dot(d, 32, 32, 6); save(img, "frame")
img, d = canvas(); d.rounded_rectangle([S(12), S(4), S(52), S(60)], radius=S(4), outline=255, width=S(6)); d.rectangle([S(22), S(14), S(42), S(50)], fill=160); save(img, "portal")
img, d = canvas(); ring(d, 32, 32, 26, 5); line(d, 32, 4, 32, 20, 4); line(d, 32, 44, 32, 60, 4); line(d, 4, 32, 20, 32, 4); line(d, 44, 32, 60, 32, 4); dot(d, 32, 32, 4); save(img, "crosshair")
img, d = canvas(); line(d, 6, 50, 22, 30, 6); line(d, 22, 30, 34, 40, 6); line(d, 34, 40, 56, 12, 6); poly(d, [(44,10),(58,8),(56,22)]); save(img, "chart")
print("icons ok")
