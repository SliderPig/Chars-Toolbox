# Fetches the built jar of every mod the Toolbox carries into parts/, and writes the list Forge reads (metadata.json).
# The Toolbox's own build then packs parts/ into its jar under META-INF/jarjar/ (Forge's jar-in-jar).
# parts.lock.json says which mods and exactly which version of each; a mod that isn't built at that version stops the
# run, so an old jar can never slip in. Build each carried mod first, then:   python tools/collect_parts.py
import glob
import json
import os
import shutil

HERE = os.path.dirname(os.path.abspath(__file__))
TOOLBOX = os.path.dirname(HERE)


def collect(lock_path, downloads_dir, parts_dir):
    with open(lock_path, encoding='utf-8') as f:
        lock = json.load(f)

    wanted = []
    for mod_id, info in lock.items():
        libs = os.path.join(downloads_dir, info['folder'], 'build', 'libs')
        name = f"{mod_id}-{info['version']}.jar"
        jar = os.path.join(libs, name)
        if not os.path.isfile(jar):
            there = sorted(os.path.basename(p)[len(mod_id) + 1:-4] for p in glob.glob(os.path.join(libs, mod_id + '-*.jar'))
                           if not os.path.basename(p).endswith(('-api.jar', '-sources.jar')))
            raise SystemExit(f"{mod_id}: the Toolbox carries version {info['version']}, but {libs} has "
                             f"{', '.join(there) if there else 'no built jar'}. Build {info['folder']} first.")
        wanted.append((mod_id, info, jar, name))

    os.makedirs(parts_dir, exist_ok=True)
    for old in glob.glob(os.path.join(parts_dir, '*.jar')) + glob.glob(os.path.join(parts_dir, 'metadata.json')):
        os.remove(old)

    jars = []
    for mod_id, info, jar, name in wanted:
        shutil.copyfile(jar, os.path.join(parts_dir, name))
        jars.append({
            'identifier': {'group': info['group'], 'artifact': mod_id},
            'version': {'range': f"[{info['version']},)", 'artifactVersion': info['version']},
            'path': 'META-INF/jarjar/' + name,
            'isObfuscated': True,
        })
    with open(os.path.join(parts_dir, 'metadata.json'), 'w', encoding='utf-8', newline='\n') as f:
        json.dump({'jars': jars}, f, indent=2)
    return [name for _, _, _, name in wanted]


def main():
    names = collect(os.path.join(TOOLBOX, 'parts.lock.json'), os.path.dirname(TOOLBOX), os.path.join(TOOLBOX, 'parts'))
    print('parts/:', ', '.join(names))


if __name__ == '__main__':
    main()
