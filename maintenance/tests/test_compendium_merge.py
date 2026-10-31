"""Check resource merging, unlocked navigation and absence of discovery spoilers."""
from pathlib import Path
import importlib.util
import re
import sys
import tempfile
import unittest
from xml.etree import ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
import merge_compendium as merger


def original_xml():
    root = ET.Element('compendium')
    components, modifiers = ET.SubElement(root, 'components'), ET.SubElement(root, 'modifiers')
    for identifier in merger.TARGETS:
        entry = ET.SubElement(components if identifier == 'Nullify' else modifiers,
                              'component' if identifier == 'Nullify' else 'modifier', id=identifier)
        ET.SubElement(entry, 'name').text = identifier
        ET.SubElement(entry, 'desc').text = 'Inherited description: ' + identifier + '.'
    return ET.tostring(root, encoding='utf-8', xml_declaration=True)


class CompendiumMergeTests(unittest.TestCase):
    def test_languages_preserve_upstream_and_are_idempotent(self):
        for language in ['en_US', 'pt_BR', 'ru_RU']:
            merged = merger.merge_compendium(original_xml(), language)
            self.assertEqual(merged, merger.merge_compendium(merged, language))
            root = ET.fromstring(merged)
            guide = merger._find_target(root, merger.GUIDE_ID)
            self.assertEqual('true', guide.get('unlocked'))
            self.assertEqual(9, len(guide.findall('subitem')))
            for identifier in merger.TARGETS:
                self.assertIn('Inherited description: ' + identifier + '.',
                              merger._find_target(root, identifier).findtext('desc'))

    def test_jar_merge_preserves_classes_and_adds_portuguese(self):
        with tempfile.TemporaryDirectory() as directory:
            source, target = Path(directory) / 'original.jar', Path(directory) / 'patched.jar'
            with zipfile.ZipFile(source, 'w') as jar:
                jar.writestr('assets/am2pg/docs/ArcaneCompendium_en_US.xml', original_xml())
                jar.writestr('some/Unrelated.class', b'unchanged class body')
            merger.patch_jar(source, target)
            with zipfile.ZipFile(target) as jar:
                self.assertEqual(b'unchanged class body', jar.read('some/Unrelated.class'))
                text = jar.read('assets/am2pg/docs/ArcaneCompendium_pt_BR.xml').decode('utf-8')
                self.assertIn('Requisitos das Silver avançadas', text)
                self.assertIn('200.000', text)
                self.assertIn('Ender Guardian', text)
                self.assertIn('inventário', text)

    def test_only_extra_requirements_are_published(self):
        # These parts are deliberately omitted even individually: the book must not reveal recipes.
        hidden_parts = ['Mana Drain', 'Entangle', 'Life Tap', 'Buff Power', 'Dispel', 'Silence',
                        'Reflect', 'Charm', 'Summon', 'Duration', 'Grow', 'Heal', 'Healing',
                        'Regeneration', 'Ignition', 'Wither', 'Curse', 'Astral Distortion',
                        'Invisibility', 'Lunar', 'Bless']
        for path in merger.RESOURCE_DIR.glob('SilverProgression_*.xml'):
            root = ET.fromstring(path.read_bytes())
            visible = ' '.join(''.join(root.itertext()).split())
            for part in hidden_parts:
                self.assertIsNone(re.search(r'\b' + re.escape(part) + r'\b', visible, re.I),
                                  f'{path.name} reveals {part}')
            self.assertNotIn(' + ', visible)


if __name__ == '__main__':
    unittest.main()
