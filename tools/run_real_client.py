# Starts the real game the way the Modrinth app does (same Forge, in a throwaway folder: run-real/) with the Toolbox,
# lets the Toolbox check itself (client/DevShots: "[Shots] ..." lines in the log, pictures in run-real/screenshots)
# and says PASS or FAIL for what each phase should show.
#   python tools/run_real_client.py phase=first        a new profile: the welcome, all seven parts on, a switch clicked
#   python tools/run_real_client.py phase=off-all      every part switched off before the game starts
#   python tools/run_real_client.py phase=same-job     with Xaero's Minimap installed: Map must start off by itself
#   python tools/run_real_client.py phase=ways         in a flat test world: every way in, then all parts off and on again
#   python tools/run_real_client.py phase=double-old   with an old separate Fortress Boxes 1.0 in the mods folder too
#   python tools/run_real_client.py phase=double-new   with a separate Fortress Boxes 1.1 in the mods folder too
#   python tools/run_real_client.py phase=alone        Fortress Boxes 1.1 by itself, no Toolbox
#   python tools/run_real_client.py phase=suitcase-save   profile A changes a key, an option and a switch, and saves
#   python tools/run_real_client.py phase=suitcase-bring  a second profile (run-real-b) is asked, and brings them over
#   python tools/run_real_client.py phase=small        Zoom, Little HUD, Pickup list, Gear watch, Memory watch in a flat test world
#   python tools/run_real_client.py phase=cat          Cat corner: Sleepy Cat, Fullbright, Auto Eat in a test world, then a fake kick for Auto Rejoin
#   python tools/run_real_client.py phase=all          every part on together in a normal world (1.0 walk-through), frame rate on vs off
#   python tools/run_real_client.py phase=fps          the eight Toolbox-written parts switched on and off in turn: the frame rate in each
#   python tools/run_real_client.py phase=keyclash     the Key clash fixer, with Soul Release and Char's Films beside it (Char's testing instance)
# Every phase uses a test suitcase folder (suitcase-test/), never the real one in AppData.
# Build first: gradlew build in each carried mod, python tools/collect_parts.py, gradlew build here.
import glob
import json
import os
import shutil
import subprocess
import sys
import time

M = os.path.join(os.environ['APPDATA'], 'ModrinthApp')
META = os.path.join(M, 'meta')
VERSION = '1.20.1-47.4.28'
JAVA = os.path.join('C:' + os.sep, 'Program Files', 'Eclipse Adoptium', 'jdk-17.0.20.101-hotspot', 'bin', 'java.exe')
DOWNLOADS = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
RUN = os.path.abspath('run-real')
SUITCASE = os.path.abspath('suitcase-test')
TIMEOUT = 500
NL = chr(10)

# id: (version, does it have a key and a network channel?). The versions come from parts.lock.json, so a carried mod's
# new version can't leave this table behind.
_SIGNS = {'charmap': True, 'charlod': True, 'charmenu': False, 'charteams': True, 'charconfig': True, 'fortressboxes': True}
with open(os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'parts.lock.json'), encoding='utf-8') as _f:
    _LOCK = json.load(_f)
PARTS = {mod: (_LOCK[mod]['version'], signs) for mod, signs in _SIGNS.items()}


def part_line(mod, on):
    version, signs = PARTS[mod]
    word = 'true' if on else 'false'
    sign = word if signs else 'none'
    return f'{mod} loaded=true version={version} key={sign} channel={sign} started={word}'


ALL_ON = [part_line(m, True) + ' separateCopy=false note=NORMAL' for m in PARTS]
NOT_FB = [m for m in PARTS if m != 'fortressboxes']

PHASES = {
    'first': ['firstRun=true screen=ToolboxScreen', 'title=CharTitleScreen menuMixins=true'] + ALL_ON
             + ['after click on=false property=false file=fortressboxes=false note=RESTART', 'back on=true file=fortressboxes=true'],
    'off-all': ['firstRun=false screen=TitleScreen', 'title=TitleScreen menuMixins=false'] + [part_line(m, False) for m in PARTS],
    'same-job': ['firstRun=true screen=ToolboxScreen', part_line('charmap', False) + " separateCopy=false note=NORMAL brief=Xaero's Minimap is here"]
                + [part_line(m, True) for m in PARTS if m != 'charmap'],
    'ways': ['way key screen=ToolboxScreen', 'way command screen=ToolboxScreen', 'way pause button=true',
             'way pause click screen=ToolboxScreen', 'way modslist screen=ToolboxScreen', 'back on=true file=fortressboxes=true',
             'all off=true', 'all back on=true', 'notice title=Two jobs on one key showing=true'] + ALL_ON,
    'double-old': ['fortressboxes loaded=true version=1.0', 'started=null separateCopy=true note=OLD_COPY_IN_CHARGE']
                  + [part_line(m, True) for m in NOT_FB],
    'double-new': ['fortressboxes loaded=true version=1.1', 'started=true separateCopy=true note=SEPARATE_COPY']
                  + [part_line(m, True) for m in NOT_FB],
    'alone': [],
    'small': ['zoom key=key.keyboard.h', 'hud lines=', 'Facing south (+Z)', 'Day 1, 12:0', 'pickups rows=+10 Cobblestone | +7 Iron Ore',
              'gear strip=true', 'guards=true attack cancelled=true meows=2', 'zoom current=4.00', 'zoom turn=3.75',
              'zoom scrolled=5.0 selected=0', 'zoom after=1.00', "memory notice=The game's memory is nearly full", 'pause line=Memory: ',
              'settings fps=true corner=top right', 'off hud=true pickups=0 gear=false guard=false pause=true attack cancelled=false notice=false'],
    'cat': ['cat visible=true', 'onHud=false key=key.keyboard.apostrophe', 'cat onHud=true', 'gamma before=0.50', 'gamma on=15.00', 'gamma paused=0.50',
            'gamma back=15.00', 'gamma off=0.50', 'eat ate=true', 'placed x=190 y=124', 'off visible=false fullbright=false eat=false rejoin=false',
            'rejoin planned=true countdown=Joining Toolbox test server again in', 'rejoin tries=1 planned=true screen=DisconnectedScreen',
            'rejoin after pill planned=false on=false'],
    'all': ['all hud=', 'cat=true', 'all map screen=MapScreen', 'all teams screen=TeamScreen', 'all configs screen=ConfigScreen',
            'all farview screen=FarScreen', 'all B screen=', 'all zoom=4.00', 'all fps on='],
    'fps': ['fps newparts on='],
    'keyclash': ['clash key.keyboard.b=key.charmap.add_waypoint+key.fortressboxes.toggle', 'pressed fix=true', 'pressed doit=true',
                 'after fix clashes=0', 'pressed undo=true', 'fortressboxes=key.keyboard.b list=',
                 'changed fortressboxes=key.keyboard.n file=key_key.fortressboxes.toggle:key.keyboard.n', 'cleared fortressboxes=key.keyboard.unknown',
                 'put back fortressboxes=key.keyboard.b'],
    'suitcase-save': ['suitcase saved=Saved. from=run-real mode=SHARE'],
    'suitcase-bring': ['bring screen=BringScreen mode=ASK savedFrom=run-real',
                       'brought key=key.keyboard.f12 renderDistance=7 fortressboxes=false servers=1 waypoints=true mode=SHARE',
                       'after bring screen=ToolboxScreen'],
}


def allowed(rules):
    ok = not rules
    for r in rules or []:
        name = (r.get('os') or {}).get('name')
        if name in (None, 'windows') and 'features' not in r:
            ok = r['action'] == 'allow'
    return ok


def library_path(lib):
    art = (lib.get('downloads') or {}).get('artifact')
    if art and art.get('path'):
        return art['path']
    group, name, ver = lib['name'].split(':')[:3]
    return f"{group.replace('.', '/')}/{name}/{ver}/{name}-{ver}.jar"


def newest(pattern):
    found = sorted(glob.glob(pattern), key=os.path.getmtime)
    if not found:
        raise SystemExit('not built yet: ' + pattern)
    return found[-1]


def set_up(phase):
    global RUN
    if phase == 'suitcase-bring':
        # a second profile, brand new each time; the suitcase is the one phase suitcase-save packed
        RUN = os.path.abspath('run-real-b')
    else:
        shutil.rmtree(SUITCASE, ignore_errors=True)
    # every phase starts as a brand-new profile: no keys, options, servers or waypoints left over from the phase before
    # (a key that an earlier phase rebound once made a later phase's key press do nothing)
    for name in ('options.txt', 'options.txt.before-toolbox', 'servers.dat', 'servers.dat.before-toolbox'):
        if os.path.exists(os.path.join(RUN, name)):
            os.remove(os.path.join(RUN, name))
    shutil.rmtree(os.path.join(RUN, 'charmap'), ignore_errors=True)
    os.makedirs(os.path.join(RUN, 'mods'), exist_ok=True)
    shutil.rmtree(os.path.join(RUN, 'screenshots'), ignore_errors=True)
    shutil.rmtree(os.path.join(RUN, 'config', 'chartoolbox'), ignore_errors=True)
    shutil.rmtree(os.path.join(RUN, 'logs'), ignore_errors=True)
    shutil.rmtree(os.path.join(RUN, 'saves', 'toolboxshots'), ignore_errors=True)
    for name in ('keyclash.properties', 'settings.properties'):
        if os.path.exists(os.path.join(RUN, 'config', 'chartoolbox', name)):
            os.remove(os.path.join(RUN, 'config', 'chartoolbox', name))
    # a brand new game folder opens the first-launch Narrator screen and waits for a click for ever
    options_file = os.path.join(RUN, 'options.txt')
    old = []
    if os.path.exists(options_file):
        with open(options_file, encoding='utf-8') as f:
            old = [line for line in f.read().splitlines() if not line.startswith(('onboardAccessibility:', 'pauseOnLostFocus:', 'tutorialStep:'))]
    with open(options_file, 'w', encoding='utf-8') as f:
        f.write(NL.join(old + ['onboardAccessibility:false', 'pauseOnLostFocus:false', 'tutorialStep:none']) + NL)

    for jar in glob.glob(os.path.join(RUN, 'mods', '*.jar')):
        os.remove(jar)
    new_fb = os.path.join(DOWNLOADS, 'fortressboxes', 'build', 'libs', 'fortressboxes-1.1.jar')
    if phase != 'alone':
        shutil.copy(newest(os.path.join('build', 'libs', 'chartoolbox-*.jar')), os.path.join(RUN, 'mods', 'chartoolbox-test.jar'))
    if phase == 'double-old':
        shutil.copy(os.path.join(DOWNLOADS, '{C} fortressboxes-1.0.jar'), os.path.join(RUN, 'mods', 'fortressboxes-1.0.jar'))
    if phase in ('double-new', 'alone'):
        shutil.copy(new_fb, os.path.join(RUN, 'mods', 'fortressboxes-1.1.jar'))
    if phase == 'keyclash':
        # the other mods of Char's testing instance that have keys of their own
        for name in ('{C} soulrelease-1.0.jar', '{C} charfilms-0.3.jar'):
            shutil.copy(os.path.join(DOWNLOADS, name), os.path.join(RUN, 'mods', name.replace('{C} ', '')))
    if phase == 'same-job':
        xaero = sorted(glob.glob(os.path.join(M, 'profiles', 'Powers', 'mods', 'xaerominimap-*.jar')))
        if not xaero:
            raise SystemExit("same-job needs Xaero's Minimap in the Powers profile's mods folder")
        shutil.copy(xaero[-1], os.path.join(RUN, 'mods', os.path.basename(xaero[-1])))
    if phase in ('off-all', 'alone'):
        os.makedirs(os.path.join(RUN, 'config', 'chartoolbox'))
        with open(os.path.join(RUN, 'config', 'chartoolbox', 'parts.properties'), 'w', encoding='utf-8') as f:
            f.write(''.join(m + '=false' + NL for m in PARTS))
    print('mods:', sorted(os.listdir(os.path.join(RUN, 'mods'))))


def command(phase):
    d = json.load(open(os.path.join(META, 'versions', VERSION, VERSION + '.json'), encoding='utf-8'))
    cp, seen = [], set()
    for lib in d['libraries']:
        if lib.get('include_in_classpath') is False or not allowed(lib.get('rules')) or 'natives' in lib['name'].split(':')[-1] and 'windows' not in lib['name']:
            continue
        path = os.path.join(META, 'libraries', library_path(lib))
        if os.path.exists(path) and path not in seen:
            seen.add(path)
            cp.append(path)
    cp.append(os.path.join(META, 'versions', VERSION, VERSION + '.jar'))
    natives = os.path.join(META, 'natives', VERSION)
    values = {'natives_directory': natives, 'launcher_name': 'chartoolbox-test', 'launcher_version': '1', 'classpath': os.pathsep.join(cp), 'classpath_separator': os.pathsep,
              'library_directory': os.path.join(META, 'libraries'), 'version_name': VERSION, 'auth_player_name': 'Dev', 'game_directory': RUN, 'assets_root': os.path.join(META, 'assets'),
              'assets_index_name': d['assets'], 'auth_uuid': '00000000000000000000000000000abc', 'auth_access_token': '0', 'clientid': '0', 'auth_xuid': '0', 'user_type': 'legacy', 'version_type': 'release'}

    def fill(text):
        for key, value in values.items():
            text = text.replace('${' + key + '}', value)
        return text

    def flat(args):
        out = []
        for a in args:
            if isinstance(a, str):
                out.append(fill(a))
            elif allowed(a.get('rules')):
                v = a['value']
                out.extend(fill(x) for x in ([v] if isinstance(v, str) else v))
        return out

    return [JAVA, '-Xmx2500M', '-Dchartoolbox.shots=' + phase, '-Dchartoolbox.suitcase=' + SUITCASE] + flat(d['arguments']['jvm']) + [d['mainClass']] + flat(d['arguments']['game']) + ['--width', '1600', '--height', '900']


def read_log():
    path = os.path.join(RUN, 'logs', 'latest.log')
    if not os.path.exists(path):
        return ''
    with open(path, encoding='utf-8', errors='replace') as f:
        return f.read()


def run(phase):
    with open(os.path.join(RUN, 'launch.log'), 'w', encoding='utf-8', errors='replace') as log:
        game = subprocess.Popen(command(phase), cwd=RUN, stdout=log, stderr=subprocess.STDOUT)
        started, quiet_since, size = time.time(), None, -1
        while game.poll() is None:
            time.sleep(2)
            if time.time() - started > TIMEOUT:
                print('timed out after', TIMEOUT, 's')
                game.kill()
                break
            if phase != 'alone':
                continue
            # nothing quits the game in this phase: once it has loaded and its log has been quiet for 20 s, stop it
            text = read_log()
            if 'Sound engine started' not in text:
                continue
            if len(text) != size:
                size, quiet_since = len(text), time.time()
            elif time.time() - quiet_since > 20:
                print('the game reached the title screen; stopping it')
                game.terminate()
                break
        game.wait()
        print('exit', game.returncode)


def judge(phase):
    text = read_log()
    shots = [line[line.index('[Shots]'):] for line in text.splitlines() if '[Shots]' in line]
    for line in shots:
        print(' ', line)
    joined = NL.join(shots)
    ok = True
    for want in PHASES[phase]:
        hit = want in joined
        ok &= hit
        print('PASS' if hit else 'FAIL', '|', want)
    if phase == 'alone':
        loaded = 'Sound engine started' in text
        bad = [line for line in text.splitlines() if 'fortressboxes' in line.lower() and ('ERROR' in line or 'Exception' in line)]
        ok = loaded and not bad
        print('PASS' if loaded else 'FAIL', '| the game loaded with Fortress Boxes 1.1 alone')
        print('PASS' if not bad else 'FAIL', '| no error names fortressboxes', bad[:3])
    elif phase != 'alone' and '[Shots] done' not in joined:
        ok = False
        print('FAIL | the self-check never finished (see run-real/launch.log and run-real/logs/latest.log)')
    print('screenshots:', sorted(os.path.basename(p) for p in glob.glob(os.path.join(RUN, 'screenshots', '*.png'))))
    print('RESULT', phase, 'PASS' if ok else 'FAIL')
    return ok


def main():
    phase = 'first'
    for arg in sys.argv[1:]:
        if arg.startswith('phase='):
            phase = arg.split('=', 1)[1]
    if phase not in PHASES:
        raise SystemExit('phase must be one of ' + ', '.join(PHASES))
    set_up(phase)
    run(phase)
    sys.exit(0 if judge(phase) else 1)


if __name__ == '__main__':
    main()
