# Custom Sky Image (Fabric, Minecraft 1.21.11)

Client-side mod. Press **K** (rebindable under Controls) to open the menu, then:

* **Browse...** opens your OS file picker (any file type is selectable),
* or paste a path into the box, or **drag and drop** an image onto the menu.

Sliders control brightness, opacity and rotation; you can also hide the sun, moon and stars.
The image is copied to `config/skycustom/` and restored automatically next launch.

Supported formats: PNG, JPG, GIF (first frame), BMP, TGA, PSD, HDR, PIC, PNM, plus whatever
Java ImageIO adds (e.g. TIFF, WBMP). WebP and SVG are not supported. Images larger than 4096 px are downscaled.
A 2:1 equirectangular panorama looks best; other ratios are stretched around the sky.
Applies to Overworld-style skies (not the Nether or End).

## Build
1. Easiest: generate a fresh 1.21.11 project at https://fabricmc.net/develop/template, then copy this
   project's `src/` folder over it (and use its gradle wrapper).
2. Or use this folder directly: run `gradle wrapper` once (Gradle 9.2+), then `./gradlew build`.
3. The jar is in `build/libs/skycustom-1.0.0.jar`. Put it in `mods/` with Fabric API.

## If it doesn't compile
This was written against 1.21.11 Mojang mappings without being compiled. The likeliest tweaks:
* `RenderType` / `RenderSetup` import package (IDE auto-import fixes it),
* `MoonPhase` import in the mixin,
* `NativeImage.setPixel` colour order (red/blue swapped means use `setPixelABGR`),
* `RenderSetup.withTexture(...)` signature in `SkyDomeRenderer`.
