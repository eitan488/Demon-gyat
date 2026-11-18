# Texture generators

The GUI sprites in `src/main/resources/assets/demon/textures/gui/sprites` are generated with Pillow:

```
python3 tools/gen_sprites.py src/main/resources   # shadow, glow, light sweep, fade + mod icon
python3 tools/gen_logo.py src/main/resources      # "DEMON" / "CLIENT" wordmark letters (needs the Inter font)
python3 tools/gen_icons.py src/main/resources     # vector icons for menus and modules
```

Rounded rectangles, circles and outlines are generated at runtime in exact screen pixels (see `Hd.java`); only blurry effects (shadow, glow, sweep, fade) are atlas sprites.
