# Texture generators

The GUI sprites in `src/main/resources/assets/demon/textures/gui/sprites` are generated with Pillow:

```
python3 tools/gen_sprites.py src/main/resources   # rounded rects, shadows, glows, circles + mod icon
python3 tools/gen_logo.py src/main/resources      # "DEMON" / "CLIENT" wordmark letters (needs the Inter font)
python3 tools/gen_icons.py src/main/resources     # vector icons for menus and modules
```

Note: `gen_sprites.py` writes nine-slice metadata without `stretch_inner`; the committed `.mcmeta` files add `"stretch_inner": true` so the centre is stretched instead of tiled.
