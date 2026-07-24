"""生成小程序 tabBar 图标（81x81 RGBA PNG，透明底线稿）。

普通态灰 #9CA3AF，选中态蓝 #1890FF（与 pages.json tabBar 配色一致）。
覆盖 wordmate-mini/src/static/tab-icons/*.png（原为 174-176B 占位空图）。
"""
import os
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "wordmate-mini", "src", "static", "tab-icons")
SIZE = 81
GRAY = (156, 163, 175, 255)   # #9CA3AF
BLUE = (24, 144, 255, 255)    # #1890FF
W = 5  # 默认线宽


def canvas(color):
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    return img, d, color


def study(color):
    """打开的书：左右两页 + 书脊"""
    img, d, c = canvas(color)
    d.rectangle([16, 22, 39, 58], outline=c, width=W)
    d.rectangle([41, 22, 64, 58], outline=c, width=W)
    d.line([40, 18, 40, 62], fill=c, width=W)
    return img


def test(color):
    """对勾"""
    img, d, c = canvas(color)
    d.line([20, 42, 34, 56], fill=c, width=7, joint="curve")
    d.line([34, 56, 61, 24], fill=c, width=7, joint="curve")
    return img


def stats(color):
    """柱状图：三根递增柱"""
    img, d, c = canvas(color)
    for x0, y0, x1, y1 in [(18, 46, 30, 62), (34, 34, 46, 62), (50, 20, 62, 62)]:
        d.rectangle([x0, y0, x1, y1], fill=c)
    d.line([14, 64, 66, 64], fill=c, width=W)  # 基线
    return img


def mine(color):
    """人像：圆头 + 肩膀弧"""
    img, d, c = canvas(color)
    d.ellipse([30, 14, 51, 35], outline=c, width=W)          # 头
    d.arc([16, 38, 65, 74], 180, 360, fill=c, width=W)        # 肩（上半弧）
    return img


GLYPHS = {"study": study, "test": test, "stats": stats, "mine": mine}

for name, fn in GLYPHS.items():
    fn(GRAY).save(os.path.join(OUT, f"{name}.png"))
    fn(BLUE).save(os.path.join(OUT, f"{name}-active.png"))
    print(f"{name}: 普通+选中 已生成")

print(f"\n输出目录：{OUT}")
