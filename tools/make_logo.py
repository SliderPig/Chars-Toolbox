"""Draws the store logo for Char's Toolbox: 64x64 pixel art, every pixel placed by hand, saved 8x bigger.

    python -I tools/make_logo.py        (writes store/logo.png and store/logo_64.png)

The picture says what the mod does: one box with everything in it. Char's black cat peeks out of a plum toolbox, a
wrench sticks out of the other side, and the box's latch is one of the Toolbox's own pink switches, switched on. Same
family as the Far View and Films logos: a banded night sky going pink at the horizon, hills, no noise.
"""
import os
from PIL import Image

W = H = 64
img = Image.new("RGB", (W, H))
px = img.load()

INK = (14, 8, 16)            # the cat, outlines
SKY_TOP = (40, 20, 66)
SKY_LOW = (255, 170, 214)
FAR = (156, 88, 160)
NEAR = (104, 54, 122)
GROUND = (54, 28, 66)
GROUND_LOW = (42, 21, 54)
BOX = (58, 33, 70)           # the toolbox, the colour of the switch screen's panel
BOX_DARK = (40, 22, 49)
BOX_EDGE = (24, 13, 30)
TRIM = (178, 90, 147)        # the lid's pink band
TRIM_HI = (255, 143, 200)
PINK = (255, 143, 200)       # the switch, on
KNOB = (36, 21, 43)
PALE = (255, 226, 240)
EYE = (255, 233, 150)
EAR = (255, 156, 198)
RIM = (90, 58, 94)           # a soft rim round the black cat
STEEL = (206, 196, 222)
STEEL_DARK = (132, 118, 150)


def dot(x, y, c):
    if 0 <= x < W and 0 <= y < H:
        px[x, y] = c


def rect(x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            dot(x, y, c)


def art(x0, y0, rows, colours):
    for dy, row in enumerate(rows):
        for dx, ch in enumerate(row):
            if ch in colours:
                dot(x0 + dx, y0 + dy, colours[ch])


# ---- sky: flat bands, dark at the top, pink at the horizon
TOP, HORIZON, BANDS = 0, 44, 9
for y in range(0, 50):
    t = int(min(1.0, (y - TOP) / (HORIZON - TOP)) * BANDS) / BANDS
    c = tuple(round(a + (b - a) * t) for a, b in zip(SKY_TOP, SKY_LOW))
    for x in range(W):
        px[x, y] = c

# ---- stars and two little sparkles
for x, y in [(4, 5), (13, 11), (22, 4), (33, 8), (41, 3), (52, 7), (60, 13), (7, 17), (29, 15), (46, 12), (57, 22), (2, 24)]:
    dot(x, y, (232, 196, 232))
for x, y in [(37, 11), (10, 9)]:
    dot(x, y, PALE)
    for ox, oy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
        dot(x + ox, y + oy, (214, 170, 220))

# ---- far and near hills, stepped, then the ground
far = [40, 40, 39, 39, 38, 38, 38, 39, 39, 40, 40, 41, 41, 41, 40, 40, 39, 38, 38, 37, 37, 36, 36, 36, 37, 37, 38, 38,
       39, 39, 40, 40, 40, 41, 41, 41, 41, 40, 40, 39, 39, 38, 38, 37, 37, 37, 38, 38, 39, 39, 40, 40, 40, 39, 39, 38,
       38, 38, 39, 39, 40, 40, 41, 41]
near = [44, 44, 44, 45, 45, 45, 45, 44, 44, 43, 43, 43, 44, 44, 45, 45, 46, 46, 46, 46, 45, 45, 44, 44, 44, 43, 43,
        43, 44, 44, 45, 45, 46, 46, 46, 46, 46, 45, 45, 44, 44, 43, 43, 43, 44, 44, 45, 45, 46, 46, 46, 45, 45, 44,
        44, 43, 43, 43, 44, 44, 45, 45, 46, 46]
for x in range(W):
    rect(x, far[x], x, 63, FAR)
    rect(x, near[x], x, 63, NEAR)
    rect(x, 49, x, 63, GROUND)
    rect(x, 59, x, 63, GROUND_LOW)

# ---- the box's shadow on the ground
rect(9, 57, 56, 57, BOX_EDGE)
rect(11, 58, 54, 58, GROUND_LOW)

# ---- the wrench, sticking out of the box on the right (drawn first: the lid goes over its foot). Its head is a round
# ring with the jaw's gap facing up and to the right; its shaft runs down and left into the box.
art(46, 8, [
    "...ssss....",
    "..ssssss.s.",
    ".ssssss..ss",
    ".sssss..sss",
    "sssss..ssss",
    "sssss.sssss",
    "sssssssssss",
    "dsssssssssd",
    ".dssssssssd",
    "..ddsssssd.",
    "....dsssd..",
], {"s": STEEL, "d": STEEL_DARK})
for i in range(16):                                                    # the shaft: three pixels wide, shaded below, down into the box
    x, y = 49 - i, 18 + i
    dot(x - 1, y, STEEL)
    dot(x, y, STEEL)
    dot(x + 1, y, STEEL)
    dot(x - 1, y + 1, STEEL_DARK)

# ---- the handle on the lid
art(23, 26, [
    "....eeeeeeeeee....",
    "..eeEEEEEEEEEEee..",
    ".eEE..........EEe.",
    ".eE............Ee.",
    ".eE............Ee.",
    ".eE............Ee.",
    ".eE............Ee.",
    ".eE............Ee.",
], {"e": BOX_EDGE, "E": TRIM})

# ---- the toolbox: a plum body with a pink lid band and dark edges
rect(8, 34, 56, 56, BOX_EDGE)
rect(9, 35, 55, 55, BOX)
rect(9, 35, 55, 38, TRIM)
rect(9, 35, 55, 35, TRIM_HI)
rect(9, 39, 55, 39, BOX_EDGE)
rect(9, 52, 55, 55, BOX_DARK)
for x in (9, 55):                                                        # rounded-looking corners
    dot(x, 35, BOX_EDGE)
    dot(x, 55, BOX_EDGE)
rect(12, 42, 13, 49, BOX_DARK)                                           # two little screws on the front
rect(51, 42, 52, 49, BOX_DARK)

# ---- the latch is one of the Toolbox's own switches, switched on: a pink pill with the dark knob at the right
art(23, 42, [
    "...pppppppppppp...",
    ".pppppppppppKKKKp.",
    "ppppppppppppKKKKKp",
    "ppppppppppppKKKKKp",
    "ppppppppppppKKKKKp",
    ".pppppppppppKKKKp.",
    "...pppppppppppp...",
], {"p": PINK, "K": KNOB})
dot(26, 44, PALE)                                                        # a shine on the switch
dot(27, 44, PALE)

# ---- Char's black cat, peeking over the lid with its paws on the edge (a soft rim so it shows on the dark box)
cat = [
    ".#............#.",
    ".##..........##.",
    ".#p#........#p#.",
    ".#pp########pp#.",
    "################",
    "###ee######ee###",
    "###ek######ek###",
    "#######nn#######",
    ".##############.",
    "..############..",
    "..##........##..",
]
cx, cy = 10, 25
for dy, row in enumerate(cat):
    for dx, ch in enumerate(row):
        if ch == ".":
            continue
        for ox, oy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = dx + ox, dy + oy
            inside = 0 <= ny < len(cat) and 0 <= nx < len(row) and cat[ny][nx] != "."
            if not inside:
                dot(cx + nx, cy + ny, RIM)
art(cx, cy, cat, {"#": INK, "p": EAR, "e": EYE, "k": INK, "n": EAR})
# the front paws hang over the lid's pink band, with a tiny pink toe line
for px0 in (cx + 2, cx + 12):
    rect(px0 - 1, 35, px0 + 2, 37, INK)
    rect(px0 - 1, 38, px0 + 2, 38, RIM)
    dot(px0, 37, EAR)
    dot(px0 + 1, 37, EAR)

here = os.path.dirname(os.path.abspath(__file__))
out = os.path.join(here, "..", "store")
os.makedirs(out, exist_ok=True)
img.save(os.path.join(out, "logo_64.png"))
img.resize((512, 512), Image.NEAREST).save(os.path.join(out, "logo.png"))
print("saved", os.path.abspath(os.path.join(out, "logo.png")))
