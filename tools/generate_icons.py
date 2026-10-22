#!/usr/bin/env python3
"""
Generates the Work Planner launcher and in-app brand assets from logo.png.

The supplied logo is a calendar/leaf mark above the "Work Planner" wordmark.
We extract the mark, make its outer background transparent, and emit:

  * drawable-nodpi/ic_logo_mark.png            (in-app brand mark)
  * drawable-nodpi/ic_launcher_foreground.png  (adaptive icon foreground)
  * drawable-nodpi/ic_launcher_monochrome.png  (Android 13 themed icon)
  * drawable-*/ic_notification.png             (status-bar small icon)
  * mipmap-*/ic_launcher*.png                  (legacy launcher icons)

Requires Pillow. Run from anywhere:

    python3 tools/generate_icons.py
"""
from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "logo.png"
RES = ROOT / "app" / "src" / "main" / "res"

# The mark occupies the upper square of the supplied 1264x1264 lockup.
MARK_CROP = (392, 228, 872, 708)
BACKGROUND = (233, 233, 222, 255)
DENSITIES = (("mdpi", 1), ("hdpi", 1.5), ("xhdpi", 2), ("xxhdpi", 3), ("xxxhdpi", 4))


def extract_mark() -> Image.Image:
    """Cut the calendar mark out of the lockup and remove its outer cream box."""
    crop = Image.open(SOURCE).convert("RGB").crop(MARK_CROP).copy()
    fill = (255, 0, 255)
    for corner in (
        (0, 0),
        (crop.width - 1, 0),
        (0, crop.height - 1),
        (crop.width - 1, crop.height - 1),
    ):
        ImageDraw.floodfill(crop, corner, fill, thresh=8)

    rgba = crop.convert("RGBA")
    pixels = rgba.load()
    for y in range(rgba.height):
        for x in range(rgba.width):
            if pixels[x, y][:3] == fill:
                pixels[x, y] = (0, 0, 0, 0)
    return rgba.crop(rgba.getbbox())


def fit_square(
    image: Image.Image,
    size: int,
    fill_ratio: float = 0.92,
    background: tuple[int, int, int, int] = (0, 0, 0, 0),
) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), background)
    target = int(size * fill_ratio)
    scale = min(target / image.width, target / image.height)
    resized = image.resize(
        (max(1, round(image.width * scale)), max(1, round(image.height * scale))),
        Image.Resampling.LANCZOS,
    )
    canvas.alpha_composite(
        resized,
        ((size - resized.width) // 2, (size - resized.height) // 2),
    )
    return canvas


def monochrome(image: Image.Image, size: int, fill_ratio: float) -> Image.Image:
    """White silhouette of all non-cream pixels, for status/themed icons."""
    source = fit_square(image, size, fill_ratio)
    result = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    source_pixels = source.load()
    result_pixels = result.load()
    for y in range(size):
        for x in range(size):
            r, g, b, a = source_pixels[x, y]
            if a > 24 and max(r, g, b) < 215:
                result_pixels[x, y] = (255, 255, 255, 255)
    return result


def legacy_icon(mark: Image.Image, size: int, circular: bool) -> Image.Image:
    """Legacy launcher icon: cream plate plus the calendar mark."""
    scale = 4
    canvas_size = size * scale
    plate = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(plate)
    if circular:
        draw.ellipse(
            (0, 0, canvas_size - 1, canvas_size - 1),
            fill=BACKGROUND,
        )
        fill_ratio = 0.72
    else:
        draw.rounded_rectangle(
            (0, 0, canvas_size - 1, canvas_size - 1),
            radius=round(canvas_size * 0.22),
            fill=BACKGROUND,
        )
        fill_ratio = 0.80
    plate.alpha_composite(fit_square(mark, canvas_size, fill_ratio))
    return plate.resize((size, size), Image.Resampling.LANCZOS)


def main() -> None:
    mark = extract_mark()

    (RES / "drawable-nodpi").mkdir(parents=True, exist_ok=True)
    fit_square(mark, 512, 0.98).save(RES / "drawable-nodpi" / "ic_logo_mark.png", optimize=True)
    fit_square(mark, 432, 0.68).save(
        RES / "drawable-nodpi" / "ic_launcher_foreground.png", optimize=True
    )
    monochrome(mark, 432, 0.68).save(
        RES / "drawable-nodpi" / "ic_launcher_monochrome.png", optimize=True
    )

    for density, factor in DENSITIES:
        notification_size = round(24 * factor)
        folder = RES / f"drawable-{density}"
        folder.mkdir(parents=True, exist_ok=True)
        monochrome(mark, notification_size, 0.88).save(
            folder / "ic_notification.png", optimize=True
        )

        icon_size = round(48 * factor)
        mipmap = RES / f"mipmap-{density}"
        mipmap.mkdir(parents=True, exist_ok=True)
        legacy_icon(mark, icon_size, circular=False).save(
            mipmap / "ic_launcher.png", optimize=True
        )
        legacy_icon(mark, icon_size, circular=True).save(
            mipmap / "ic_launcher_round.png", optimize=True
        )

    print("Generated logo, launcher, and notification assets.")


if __name__ == "__main__":
    main()
