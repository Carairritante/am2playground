"""Check actual build inputs, including propagation of changed compiled payloads."""
import contextlib
import io
import json
from pathlib import Path
import shutil
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import build_playable


class PlayableBuildTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        source = build_playable.ROOT
        shutil.copy2(source / 'VERSION.json', self.root / 'VERSION.json')
        for folder in ('base', 'resources', 'build/payload'):
            shutil.copytree(source / folder, self.root / folder)
        self.version = json.loads((self.root / 'VERSION.json').read_text())

    def build(self):
        with patch.object(build_playable, 'ROOT', self.root), contextlib.redirect_stdout(io.StringIO()):
            return build_playable.build()

    def test_unchanged_sources_preserve_the_validated_archive_exactly(self):
        result = self.build()
        self.assertTrue(result['base_archive_preserved_exactly'])
        self.assertEqual((self.root / self.version['build_base']).read_bytes(),
                         (self.root / 'build/playable-dist' / self.version['playable_jar']).read_bytes())

    def test_changed_compiled_payload_is_in_the_playable_mod(self):
        name = 'airburn/am2playground/progression/SilverOculusHooks.class'
        payload = self.root / 'build/payload' / name
        data = payload.read_bytes()
        changed = data.replace(b'Cannot display Playground Silver points', b'Cannot display Playground Silver Points', 1)
        self.assertNotEqual(data, changed)
        payload.write_bytes(changed)
        result = self.build()
        self.assertEqual([name], result['changed_entries'])
        with zipfile.ZipFile(self.root / 'build/playable-dist' / self.version['playable_jar']) as jar:
            self.assertEqual(changed, jar.read(name))

    def test_damaged_base_is_rejected_before_delivery(self):
        base = self.root / self.version['build_base']
        base.write_bytes(base.read_bytes() + b'corruption')
        with self.assertRaisesRegex(ValueError, 'checksum mismatch'):
            self.build()
        self.assertFalse((self.root / 'build/playable-dist').exists())


if __name__ == '__main__':
    unittest.main()
