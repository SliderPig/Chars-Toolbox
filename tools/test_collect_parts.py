# python -m unittest tools/test_collect_parts.py   (from the Toolbox folder)
import json
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import collect_parts  # noqa: E402

LOCK = {'fortressboxes': {'folder': 'fortressboxes', 'version': '1.1', 'group': 'com.example.fortressboxes'}}


class CollectPartsTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.downloads = os.path.join(self.tmp.name, 'Downloads')
        self.parts = os.path.join(self.tmp.name, 'toolbox', 'parts')
        self.lock = os.path.join(self.tmp.name, 'parts.lock.json')
        with open(self.lock, 'w', encoding='utf-8') as f:
            json.dump(LOCK, f)

    def tearDown(self):
        self.tmp.cleanup()

    def built(self, name, text='jar'):
        libs = os.path.join(self.downloads, 'fortressboxes', 'build', 'libs')
        os.makedirs(libs, exist_ok=True)
        with open(os.path.join(libs, name), 'w', encoding='utf-8') as f:
            f.write(text)

    def test_copies_the_locked_version(self):
        self.built('fortressboxes-1.1.jar', 'the real one')
        collect_parts.collect(self.lock, self.downloads, self.parts)
        with open(os.path.join(self.parts, 'fortressboxes-1.1.jar'), encoding='utf-8') as f:
            self.assertEqual('the real one', f.read())
        with open(os.path.join(self.parts, 'metadata.json'), encoding='utf-8') as f:
            meta = json.load(f)
        self.assertEqual({'jars': [{
            'identifier': {'group': 'com.example.fortressboxes', 'artifact': 'fortressboxes'},
            'version': {'range': '[1.1,)', 'artifactVersion': '1.1'},
            'path': 'META-INF/jarjar/fortressboxes-1.1.jar',
            'isObfuscated': True,
        }]}, meta)

    def test_refuses_a_missing_or_wrong_version(self):
        self.built('fortressboxes-1.0.jar')
        with self.assertRaises(SystemExit) as stop:
            collect_parts.collect(self.lock, self.downloads, self.parts)
        message = str(stop.exception)
        self.assertIn('fortressboxes', message)
        self.assertIn('1.1', message)
        self.assertIn('1.0', message)

    def test_clears_old_jars_first(self):
        os.makedirs(self.parts)
        with open(os.path.join(self.parts, 'fortressboxes-1.0.jar'), 'w', encoding='utf-8') as f:
            f.write('stale')
        self.built('fortressboxes-1.1.jar')
        collect_parts.collect(self.lock, self.downloads, self.parts)
        self.assertEqual(['fortressboxes-1.1.jar', 'metadata.json'], sorted(os.listdir(self.parts)))

    def test_ignores_api_and_sources_jars(self):
        self.built('fortressboxes-1.1.jar', 'the real one')
        self.built('fortressboxes-1.1-api.jar', 'api')
        self.built('fortressboxes-1.1-sources.jar', 'sources')
        collect_parts.collect(self.lock, self.downloads, self.parts)
        self.assertEqual(['fortressboxes-1.1.jar', 'metadata.json'], sorted(os.listdir(self.parts)))


if __name__ == '__main__':
    unittest.main()
