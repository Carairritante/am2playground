import sys
import unittest
from pathlib import Path

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from update_translation_pack import language_values,merge_language,verify_pack


class TranslationPackTests(unittest.TestCase):
    def test_published_pack_matches_maintained_sources(self):
        self.assertEqual(64,len(verify_pack()))

    def test_language_merge_preserves_other_values_and_is_idempotent(self):
        original='# Preserve this comment\nexisting=original=with equals\nchanged=before\n'
        updates='changed=depois\nnew=AM2.5 necess\u00e1rio\n'
        merged=merge_language(original,updates)
        self.assertIn('# Preserve this comment',merged)
        self.assertEqual('original=with equals',language_values(merged)['existing'])
        self.assertEqual('depois',language_values(merged)['changed'])
        self.assertEqual('AM2.5 necess\u00e1rio',language_values(merged)['new'])
        self.assertEqual(merged,merge_language(merged,updates))

    def test_duplicate_language_keys_are_rejected(self):
        with self.assertRaisesRegex(ValueError,'Duplicate language key'):
            language_values('same=one\nsame=two\n')


if __name__=='__main__':unittest.main()
