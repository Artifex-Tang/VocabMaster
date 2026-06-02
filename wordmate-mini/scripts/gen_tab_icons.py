"""
Generate polished TabBar icons for wordmate-mini.
Modern rounded line-art style, 81x81 RGBA PNG.
"""
import os
import math
from PIL import Image, ImageDraw

SIZE = 81
NORMAL = (156, 163, 175, 255)   # #9CA3AF
ACTIVE = (24, 144, 255, 255)    # #1890FF
BG = (0, 0, 0, 0)

OUT = os.path.join(os.path.dirname(__file__), '..', 'static', 'tab-icons')


def new():
    return Image.new('RGBA', (SIZE, SIZE), BG)


def circle_point(cx, cy, r, angle_deg):
    a = math.radians(angle_deg)
    return (cx + r * math.cos(a), cy - r * math.sin(a))


# ── Study: Open book with pages ──────────────────────────────────
def draw_study(draw, c):
    W = 3
    # Book outline - two pages spread open
    # Left page
    draw.rounded_rectangle([10, 18, 39, 64], radius=3, outline=c, width=W)
    # Right page
    draw.rounded_rectangle([42, 18, 71, 64], radius=3, outline=c, width=W)
    # Spine curve
    draw.arc([36, 16, 45, 66], 270, 90, fill=c, width=W)
    # Text lines - left page
    for y in [28, 35, 42, 49]:
        draw.rounded_rectangle([15, y, 34, y + 2], radius=1, fill=c)
    # Text lines - right page
    for y in [28, 35, 42, 49]:
        draw.rounded_rectangle([47, y, 66, y + 2], radius=1, fill=c)
    # Bookmark ribbon on right page
    draw.polygon([(62, 18), (67, 18), (67, 30), (64.5, 27), (62, 30)], fill=c)


# ── Test: Checklist with checkmarks ──────────────────────────────
def draw_test(draw, c):
    W = 3
    # Clipboard body
    draw.rounded_rectangle([15, 20, 66, 68], radius=5, outline=c, width=W)
    # Clip at top
    draw.rounded_rectangle([30, 12, 51, 24], radius=4, outline=c, width=W)
    # Clip inner
    draw.rounded_rectangle([35, 16, 46, 22], radius=2, fill=c)
    # Three rows with checkmarks
    rows = [(32, 32), (32, 44), (32, 56)]
    for rx, ry in rows:
        # Checkbox
        draw.rounded_rectangle([rx, ry, rx + 8, ry + 8], radius=2, outline=c, width=2)
        # Checkmark inside (only first two)
    # Checkmark 1
    x, y = 33, 36
    draw.line([(x, y), (x + 2, y + 3), (x + 6, y - 3)], fill=c, width=2)
    # Checkmark 2
    x, y = 33, 48
    draw.line([(x, y), (x + 2, y + 3), (x + 6, y - 3)], fill=c, width=2)
    # Answer lines
    draw.rounded_rectangle([44, 34, 62, 36], radius=1, fill=c)
    draw.rounded_rectangle([44, 46, 60, 48], radius=1, fill=c)
    draw.rounded_rectangle([44, 58, 58, 60], radius=1, fill=c)


# ── Stats: Trending up chart ────────────────────────────────────
def draw_stats(draw, c):
    W = 3
    # Axis lines
    draw.line([(14, 66), (14, 14)], fill=c, width=W)
    draw.line([(14, 66), (68, 66)], fill=c, width=W)
    # Arrow on Y axis
    draw.polygon([(14, 12), (10, 18), (18, 18)], fill=c)
    # Arrow on X axis
    draw.polygon([(70, 66), (64, 62), (64, 70)], fill=c)
    # Bars
    bar_data = [
        (22, 50, 10),  # x, height, width
        (35, 40, 10),
        (48, 32, 10),
        (61, 22, 10),
    ]
    for bx, bh, bw in bar_data:
        draw.rounded_rectangle([bx, 66 - bh, bx + bw, 66], radius=2, fill=c)
    # Trend line (dotted feel - small circles)
    trend = [(27, 48), (40, 38), (53, 30), (66, 20)]
    for i in range(len(trend) - 1):
        draw.line([trend[i], trend[i + 1]], fill=c, width=2)
    for tx, ty in trend:
        draw.ellipse([tx - 3, ty - 3, tx + 3, ty + 3], fill=(255, 255, 255, 255), outline=c, width=2)


# ── Mine: Person avatar ─────────────────────────────────────────
def draw_mine(draw, c):
    W = 3
    # Head - circle
    draw.ellipse([27, 10, 54, 37], outline=c, width=W)
    # Body - rounded torso shape using arcs
    draw.arc([16, 40, 65, 80], 200, 340, fill=c, width=W)
    # Shoulders curve
    draw.arc([18, 28, 64, 52], 25, 155, fill=c, width=W)
    # Small badge/shield on body (achievement indicator)
    draw.rounded_rectangle([50, 46, 66, 62], radius=3, outline=c, width=2)
    # Star in badge
    cx, cy, r = 58, 54, 4
    pts = []
    for i in range(5):
        a1 = 90 + i * 72
        a2 = 90 + i * 72 + 36
        pts.append(circle_point(cx, cy, r, a1))
        pts.append(circle_point(cx, cy, r * 0.4, a2))
    draw.polygon(pts, fill=c)


ICONS = {
    'study': draw_study,
    'test': draw_test,
    'stats': draw_stats,
    'mine': draw_mine,
}


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, draw_fn in ICONS.items():
        for active in (False, True):
            color = ACTIVE if active else NORMAL
            suffix = '-active' if active else ''
            img = new()
            d = ImageDraw.Draw(img)
            draw_fn(d, color)
            fname = f'{name}{suffix}.png'
            img.save(os.path.join(OUT, fname))
            print(f'  {fname}')
    print(f'Done -> {os.path.abspath(OUT)}')


if __name__ == '__main__':
    main()
