"""Текстуры боевой системы: катаны трёх разрядов, полумесяц удара, значок оглушения.
Запуск: python3 tools/tex_combat.py src/main/resources"""
import math, sys
from PIL import Image
A = sys.argv[1] + "/assets/onepiece_newworld/textures"
def hexc(h, a=255): return ((h >> 16) & 255, (h >> 8) & 255, h & 255, a)
def shade(c, f): return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3],)

def katana(name, blade, edge, guard, wrap, wrap2):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); px = im.load()
    # клинок по диагонали от гарды (5,10) к острию (15,0), лёгкий изгиб
    for i in range(11):
        x, y = 5 + i, 10 - i
        bend = 1 if 3 <= i <= 8 else 0
        px[x, y - bend if y - bend >= 0 else y] = hexc(blade)
        if 0 <= y + 1 - bend < 16 and x < 15:
            px[x, y + 1 - bend] = hexc(edge)
    px[15, 0] = hexc(edge)
    # гарда (цуба)
    for dx, dy in [(-1, -1), (0, -1), (1, 0), (1, 1), (0, 1), (-1, 0), (0, 0)]:
        px[5 + dx, 10 + dy] = hexc(guard) if (dx, dy) != (0, 0) else shade(hexc(guard), 0.7)
    # рукоять с обмоткой
    for i in range(1, 5):
        x, y = 5 - i, 10 + i
        px[x, y] = hexc(wrap if i % 2 else wrap2)
        if x + 1 < 16 and y < 16 and i < 4:
            px[x + 1, y] = shade(hexc(wrap), 0.75)
    px[0, 15] = hexc(guard)
    im.save(f"{A}/item/{name}.png")

katana("katana", 0xC9CED6, 0xF2F5F8, 0x5A4A32, 0x2B2B33, 0xD8D2C0)
katana("wazamono_katana", 0xB8C8DA, 0xF4FAFF, 0xC9A23A, 0x1E2A44, 0xE6E0CC)
katana("o_wazamono_katana", 0x8E95A6, 0xE9EEF6, 0x2B2B2B, 0x7A1E22, 0xD9C9A8)

# полумесяц удара 64×32, белый с прозрачностью — цвет задаётся при отрисовке
W, H = 64, 32
im = Image.new("RGBA", (W, H), (0, 0, 0, 0)); px = im.load()
for y in range(H):
    for x in range(W):
        cx = x + 0.5; cy = y + 0.5
        outer = math.hypot(cx - 32, (cy - 34) * 1.0) <= 32
        inner = math.hypot(cx - 32, (cy - 41) * 1.0) <= 33
        if outer and not inner:
            side = abs(cx - 32) / 32.0
            depth = (math.hypot(cx - 32, cy - 34) - 26) / 6.0
            core = max(0.0, 1.0 - abs(depth - 0.85) * 1.6)
            a = int(255 * max(0.0, 1.0 - side ** 2.2) * (0.45 + 0.55 * core))
            v = int(200 + 55 * core)
            px[x, y] = (v, v, v, max(0, min(255, a)))
im.save(f"{A}/entity/slash.png")

# значок оглушения 18×18: кольцо из жёлтых звёздочек
im = Image.new("RGBA", (18, 18), (0, 0, 0, 0)); px = im.load()
for k in range(5):
    ang = k * 2 * math.pi / 5 - math.pi / 2
    sx, sy = 9 + 6 * math.cos(ang), 9 + 4 * math.sin(ang)
    for dx, dy in [(0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)]:
        x, y = int(round(sx + dx)), int(round(sy + dy))
        if 0 <= x < 18 and 0 <= y < 18:
            px[x, y] = hexc(0xFFE04A) if (dx, dy) != (0, 0) else hexc(0xFFFFFF)
im.save(f"{A}/mob_effect/stunned.png")
print("ok")

# значки эффектов Королевского Хаки 18×18
def ring_icon(name, outer, inner):
    im = Image.new("RGBA", (18, 18), (0, 0, 0, 0)); px = im.load()
    for y in range(18):
        for x in range(18):
            d = math.hypot(x - 8.5, y - 8.5)
            if 6.0 <= d <= 8.2:
                px[x, y] = hexc(outer)
            elif 3.0 <= d <= 4.6:
                px[x, y] = hexc(inner)
    im.save(f"{A}/mob_effect/{name}.png")

ring_icon("knocked_out", 0x1A0508, 0xC21F3A)
ring_icon("haki_suppression", 0xC21F3A, 0x2A0A10)
print("ok2")
