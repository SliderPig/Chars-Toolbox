# Writes the Toolbox's switch hook (hook/ToolboxSwitch.java.txt) into a mod's source as ToolboxSwitch.java.
# Every mod the Toolbox carries has this one small class; it is the same text in all of them, only the package, the mod
# id and the list of same-job mods differ. Run from the Toolbox folder:
#   python tools/install_hook.py ../charmap/src/main/java --package com.example.charmap --id charmap --same-job xaerominimap,journeymap
#   --mixins also writes ToolboxMixins.java (name it as "plugin" in the mod's mixins.json): no mixins when off at start.
import argparse
import os

HERE = os.path.dirname(os.path.abspath(__file__))
HOOK = os.path.join(HERE, '..', 'hook')


def render(template, package, mod_id, same_job=()):
    with open(os.path.join(HOOK, template), encoding='utf-8') as f:
        text = f.read()
    quoted = ', '.join('"' + s + '"' for s in same_job)
    return text.replace('@PACKAGE@', package).replace('@ID@', mod_id).replace('@SAMEJOB@', quoted)


def install(source_root, package, mod_id, same_job=(), mixins=False):
    folder = os.path.join(source_root, *package.split('.'))
    os.makedirs(folder, exist_ok=True)
    templates = [('ToolboxSwitch.java.txt', 'ToolboxSwitch.java')]
    if mixins:
        templates.append(('ToolboxMixins.java.txt', 'ToolboxMixins.java'))
    written = []
    for template, name in templates:
        target = os.path.join(folder, name)
        with open(target, 'w', encoding='utf-8', newline='\n') as f:
            f.write(render(template, package, mod_id, same_job))
        written.append(target)
    return written


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('source_root', help="the mod's src/main/java folder")
    ap.add_argument('--package', required=True, help="the mod's own package, e.g. com.example.fortressboxes")
    ap.add_argument('--id', required=True, help="the mod id, which is also its line in parts.properties")
    ap.add_argument('--same-job', default='', help="mod ids of other mods that do the same job, comma separated")
    ap.add_argument('--mixins', action='store_true', help="also write ToolboxMixins.java, the mixin plugin")
    a = ap.parse_args()
    same = [s.strip() for s in a.same_job.split(',') if s.strip()]
    for path in install(a.source_root, a.package, a.id, same, a.mixins):
        print('wrote', path)


if __name__ == '__main__':
    main()
