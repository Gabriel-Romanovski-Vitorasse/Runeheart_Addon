"""Desenha a textura 16x16 do pergaminho de runa (assets/runeheart/textures/item/rune_scroll.png).

Uso (na raiz do repositório): python3 tools/textures/make_scroll.py
"""
from PIL import Image

T = (0, 0, 0, 0)
O = (74, 50, 26, 255)      # contorno
P = (232, 214, 170, 255)   # papel
S = (196, 172, 120, 255)   # sombra do papel
R = (176, 132, 72, 255)    # rolo
H = (214, 170, 104, 255)   # brilho do rolo
G = (64, 210, 170, 255)    # runa
L = (150, 255, 220, 255)   # brilho da runa

ROWS = [
    "................",
    "..OOOOOOOOOOOO..",
    ".ORHHHHHHHHHHRO.",
    ".ORRRRRRRRRRRRO.",
    "..OPPPPPPPPPPO..",
    "..OPPPPGPPPPSO..",
    "..OPPPGLGPPPSO..",
    "..OPPGPGPGPPSO..",
    "..OPPPPGPPPPSO..",
    "..OPPPGPGPPPSO..",
    "..OPPGPPPGPPSO..",
    "..OPPPPPPPPPSO..",
    ".ORRRRRRRRRRRRO.",
    ".ORHHHHHHHHHHRO.",
    "..OOOOOOOOOOOO..",
    "................",
]
COLORS = {".": T, "O": O, "P": P, "S": S, "R": R, "H": H, "G": G, "L": L}

img = Image.new("RGBA", (16, 16))
for y, row in enumerate(ROWS):
    for x, ch in enumerate(row):
        img.putpixel((x, y), COLORS[ch])
img.save("src/main/resources/assets/runeheart/textures/item/rune_scroll.png")
print("ok")
