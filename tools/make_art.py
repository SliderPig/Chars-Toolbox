# Draws the Toolbox's small pictures, pixel by pixel (no random noise): the black cat, the gear, the padlock and one
# icon per part. Run from the project folder:   python tools/make_art.py
# Part icons sit on a light pink disc, so they are drawn in dark plum; the gear and padlock sit on a dark row, so pink.
import os
from PIL import Image

OUT = os.path.join('src', 'main', 'resources', 'assets', 'chartoolbox', 'textures', 'gui')
COLOURS = {
    '.': (0, 0, 0, 0),
    'k': (24, 16, 28, 255),      # the black cat
    'p': (255, 143, 200, 255),   # pink
    'y': (255, 233, 150, 255),   # the cat's eyes
    'd': (36, 21, 43, 255),      # dark plum lines
    'm': (150, 84, 134, 255),    # a softer plum for what is inside
}

CAT = [
    '................',
    '..k.........k...',
    '.kkk.......kkk..',
    '.kpkk.....kkpk..',
    '.kppkkkkkkkppk..',
    '.kkkkkkkkkkkkk..',
    '.kkkkkkkkkkkkk..',
    '.kkyykkkkkyykk..',
    '.kkyykkkkkyykk..',
    '.kkkkkkpkkkkkk..',
    '.kkkkkkkkkkkkk..',
    '..kkkkkkkkkkk...',
    '...kkkkkkkkk....',
    '....kkkkkkk.....',
    '................',
    '................',
]

GEAR = [
    '....pppp....',
    '.pp.pppp.pp.',
    '.pppppppppp.',
    '..pppppppp..',
    'pppp....pppp',
    'pppp....pppp',
    'pppp....pppp',
    'pppp....pppp',
    '..pppppppp..',
    '.pppppppppp.',
    '.pp.pppp.pp.',
    '....pppp....',
]

LOCK = [
    '............',
    '....pppp....',
    '...pp..pp...',
    '...p....p...',
    '...p....p...',
    '..pppppppp..',
    '..pppppppp..',
    '..ppp..ppp..',
    '..ppp..ppp..',
    '..pppppppp..',
    '..pppppppp..',
    '............',
]

# Fortress Boxes: the outline of a fortress with a few of its pieces inside.
FORTRESS_BOXES = [
    '................',
    '................',
    '..dddddddddddd..',
    '..d..........d..',
    '..d..mmmm....d..',
    '..d..mmmm....d..',
    '..d..mmmm....d..',
    '..d......mmm.d..',
    '..d......mmm.d..',
    '..d..........d..',
    '..d.mm.......d..',
    '..d.mm.......d..',
    '..d..........d..',
    '..dddddddddddd..',
    '................',
    '................',
]


# Map: a pin above a folded map.
MAP = [
    '................',
    '......dddd......',
    '.....dmmmmd.....',
    '.....dm..md.....',
    '.....dmmmmd.....',
    '......dmmd......',
    '.......dd.......',
    '................',
    '..dddd....dddd..',
    '..d..dddddd..d..',
    '..d...d..d...d..',
    '..d...d..d...d..',
    '..d...d..d...d..',
    '..dddddddddddd..',
    '................',
    '................',
]

# Far View: far mountains and the sun.
FAR_VIEW = [
    '................',
    '................',
    '..........mmm...',
    '.........mmmmm..',
    '..........mmm...',
    '.....d..........',
    '....ddd.........',
    '...dd.dd....d...',
    '..dd...dd..ddd..',
    '.dd.....dddd.dd.',
    '.d.......dd...d.',
    '.dddddddddddddd.',
    '................',
    '................',
    '................',
    '................',
]

# Main Menu: the little house.
MAIN_MENU = [
    '................',
    '.......dd.......',
    '......dddd......',
    '.....dd..dd.....',
    '....dd....dd....',
    '...dd......dd...',
    '..dd........dd..',
    '...d........d...',
    '...d..mmm...d...',
    '...d..mmm...d...',
    '...d..mmm...d...',
    '...d..mmm...d...',
    '...dddddddddd...',
    '................',
    '................',
    '................',
]

# Teams: two people side by side.
TEAMS = [
    '................',
    '................',
    '...ddd....mmm...',
    '..ddddd..mmmmm..',
    '..ddddd..mmmmm..',
    '...ddd....mmm...',
    '................',
    '..ddddd..mmmmm..',
    '.ddddddd.mmmmmm.',
    '.dddddddmmmmmmm.',
    '.dddddddmmmmmmm.',
    '................',
    '................',
    '................',
    '................',
    '................',
]

# Config Helper: three sliders.
CONFIG_HELPER = [
    '................',
    '................',
    '....mmm.........',
    '..ddmmmdddddddd.',
    '....mmm.........',
    '................',
    '.........mmm....',
    '..dddddddmmmddd.',
    '.........mmm....',
    '................',
    '......mmm.......',
    '..ddddmmmdddddd.',
    '......mmm.......',
    '................',
    '................',
    '................',
]

# Game Logger: a pulse line.
GAME_LOGGER = [
    '................',
    '................',
    '................',
    '.......d........',
    '.......d........',
    '......d.d.......',
    '......d.d....d..',
    '.dddd.d.d...d.dd',
    '.....d...d.d....',
    '.........d.d....',
    '..........d.....',
    '..........d.....',
    '................',
    '................',
    '................',
    '................',
]


# Bring my settings: a little suitcase.
SUITCASE = [
    '................',
    '................',
    '......dddd......',
    '.....d....d.....',
    '.....d....d.....',
    '..dddddddddddd..',
    '..dmmmmmmmmmmd..',
    '..dmmmmmmmmmmd..',
    '..dddddddddddd..',
    '..dmmmmmmmmmmd..',
    '..dmmmmmmmmmmd..',
    '..dmmmmmmmmmmd..',
    '..dddddddddddd..',
    '................',
    '................',
    '................',
]


# Key clash fixer: two keys and a little spark between them.
KEY_CLASH = [
    '................',
    '.dddddd..dddddd.',
    '.d....d..d....d.',
    '.d.mm.d..d.mm.d.',
    '.d.mm.d..d.mm.d.',
    '.d....d..d....d.',
    '.dddddd..dddddd.',
    '..dddd....dddd..',
    '................',
    '.......m........',
    '......mm........',
    '.....mmmmmm.....',
    '........mm......',
    '........m.......',
    '................',
    '................',
]


# Zoom: a magnifying glass.
ZOOM = [
    '................',
    '................',
    '....dddddd......',
    '...d......d.....',
    '..d..mm....d....',
    '..d.mm.....d....',
    '..d.m......d....',
    '..d........d....',
    '...d......d.....',
    '....dddddddd....',
    '..........ddd...',
    '...........ddd..',
    '............ddd.',
    '.............dd.',
    '................',
    '................',
]

# Little HUD: a small rounded pill with three lines on it.
LITTLE_HUD = [
    '................',
    '................',
    '................',
    '..dddddddddddd..',
    '.d............d.',
    '.d.mmmmmmmm...d.',
    '.d............d.',
    '.d.mmmmmmmmmm.d.',
    '.d............d.',
    '.d.mmmmm......d.',
    '.d............d.',
    '..dddddddddddd..',
    '................',
    '................',
    '................',
    '................',
]

# Pickup list: a gem with a little plus.
PICKUP_LIST = [
    '................',
    '...........d....',
    '..........ddd...',
    '...........d....',
    '.......dd.......',
    '......dmmd......',
    '.....dmmmmd.....',
    '....dmmmmmmd....',
    '...dmmmmmmmmd...',
    '....dmmmmmmd....',
    '.....dmmmmd.....',
    '......dmmd......',
    '.......dd.......',
    '................',
    '................',
    '................',
]

# Gear watch: a helmet over a wear bar.
GEAR_WATCH = [
    '................',
    '................',
    '.....dddddd.....',
    '....dmmmmmmd....',
    '...dmmmmmmmmd...',
    '...dmm....mmd...',
    '...dm......md...',
    '...dd......dd...',
    '................',
    '................',
    '..dddddddddddd..',
    '..dmmmmmm....d..',
    '..dddddddddddd..',
    '................',
    '................',
    '................',
]

# Memory watch: a memory chip.
MEMORY_WATCH = [
    '................',
    '................',
    '................',
    '...d.d.d.d.d....',
    '..dddddddddddd..',
    '..d..........d..',
    '..d.mm.mm.mm.d..',
    '..d.mm.mm.mm.d..',
    '..d..........d..',
    '..dddddddddddd..',
    '...d.d.d.d.d....',
    '................',
    '................',
    '................',
    '................',
    '................',
]


# Cat corner: Char's sleepy cat, lying down with its tail hanging.
CAT_CORNER = [
    '................',
    '................',
    '................',
    '..........d...d.',
    '..........dd.dd.',
    '..........dmddm.',
    '...ddddddddddddd',
    '..dddddddddddddd',
    '.ddddddddddddddd',
    '.ddddddddddddddd',
    '..dddddddddddddd',
    '..d.............',
    '..d.............',
    '...d............',
    '...dd...........',
    '................',
]


def save(rows, *path):
    assert all(len(r) == len(rows[0]) for r in rows), path
    img = Image.new('RGBA', (len(rows[0]), len(rows)))
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            img.putpixel((x, y), COLOURS[c])
    target = os.path.join(OUT, *path)
    os.makedirs(os.path.dirname(target), exist_ok=True)
    img.save(target)
    return img


def main():
    made = {
        'cat': save(CAT, 'cat.png'),
        'gear': save(GEAR, 'gear.png'),
        'lock': save(LOCK, 'lock.png'),
        'charmap': save(MAP, 'part', 'charmap.png'),
        'charlod': save(FAR_VIEW, 'part', 'charlod.png'),
        'charmenu': save(MAIN_MENU, 'part', 'charmenu.png'),
        'charteams': save(TEAMS, 'part', 'charteams.png'),
        'charconfig': save(CONFIG_HELPER, 'part', 'charconfig.png'),
        'fortressboxes': save(FORTRESS_BOXES, 'part', 'fortressboxes.png'),
        'suitcase': save(SUITCASE, 'part', 'suitcase.png'),
        'keyclash': save(KEY_CLASH, 'part', 'keyclash.png'),
        'zoom': save(ZOOM, 'part', 'zoom.png'),
        'littlehud': save(LITTLE_HUD, 'part', 'littlehud.png'),
        'pickuplist': save(PICKUP_LIST, 'part', 'pickuplist.png'),
        'gearwatch': save(GEAR_WATCH, 'part', 'gearwatch.png'),
        'memorywatch': save(MEMORY_WATCH, 'part', 'memorywatch.png'),
        'catcorner': save(CAT_CORNER, 'part', 'catcorner.png'),
    }
    # one big sheet to look at: every picture 12x bigger, on the colour it will sit on
    scale, pad = 12, 12
    sheet = Image.new('RGBA', (pad + sum(i.width * scale + pad for i in made.values()), pad * 2 + 16 * scale), (43, 25, 51, 255))
    x = pad
    for name, img in made.items():
        back = (43, 25, 51, 255) if name in ('gear', 'lock') else (255, 214, 234, 255)
        tile = Image.new('RGBA', img.size, back)
        tile.alpha_composite(img)
        sheet.paste(tile.resize((img.width * scale, img.height * scale), Image.NEAREST), (x, pad))
        x += img.width * scale + pad
    os.makedirs('out-check', exist_ok=True)
    sheet.save(os.path.join('out-check', 'art-preview.png'))
    print('made', ', '.join(made), '-> preview out-check/art-preview.png')


if __name__ == '__main__':
    main()
