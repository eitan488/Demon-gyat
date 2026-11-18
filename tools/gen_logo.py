import json, os, sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter
OUT = sys.argv[1]
SPR = os.path.join(OUT, "assets/demon/textures/gui/sprites")
FONT = "/usr/share/fonts/opentype/inter/InterDisplay-Black.otf"
if not os.path.exists(FONT):
    FONT = "/usr/share/fonts/opentype/inter/Inter-ExtraBold.otf"
SUB = "/usr/share/fonts/opentype/inter/Inter-SemiBold.otf"
meta = {"gui": {"scaling": {"type": "stretch"}}}
info = {}
def render(text, font_path, cap_px, pad, name, spacing=0, glow=True):
    f = ImageFont.truetype(font_path, int(cap_px * 1.38))
    # measure cap height via 'H'
    hb = f.getbbox("H")
    cap = hb[3] - hb[1]
    # total width with letter spacing
    widths = [f.getlength(c) for c in text]
    W = int(sum(widths) + spacing * (len(text) - 1)) + pad * 2
    H = cap + pad * 2
    img = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(img)
    x = pad
    for c, w in zip(text, widths):
        d.text((x, pad - hb[1]), c, font=f, fill=255)
        x += w + spacing
    bbox = img.getbbox()
    # trim horizontally but keep padding
    l, r = max(0, bbox[0] - pad), min(W, bbox[2] + pad)
    img = img.crop((l, 0, r, H))
    out = Image.new("RGBA", img.size, (255, 255, 255, 0)); out.putalpha(img)
    out.save(os.path.join(SPR, name + ".png"))
    json.dump(meta, open(os.path.join(SPR, name + ".png.mcmeta"), "w"))
    if glow:
        g = img.filter(ImageFilter.GaussianBlur(pad / 2.2))
        g = g.point(lambda v: min(255, int(v * 1.8)))
        go = Image.new("RGBA", g.size, (255, 255, 255, 0)); go.putalpha(g)
        go.save(os.path.join(SPR, name + "_glow.png"))
        json.dump(meta, open(os.path.join(SPR, name + "_glow.png.mcmeta"), "w"))
    # aspect = width/height ; padFrac = pad/height (so code can place the cap box exactly)
    info[name] = (round(img.size[0] / img.size[1], 5), round(pad / img.size[1], 5))

for c in "DEMON":
    render(c, FONT, 160, 40, "logo_" + c.lower())
render("CLIENT", SUB, 60, 16, "logo_client", spacing=26)
render("DEMON", FONT, 48, 12, "logo_small", spacing=2)
print(json.dumps(info))
