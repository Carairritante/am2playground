"""Protect independent compatibility edits and localization during private assembly."""
import sys
import unittest
from pathlib import Path

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from assemble_independent import compatibility_overrides,merge_language


class AssemblyTests(unittest.TestCase):
    def test_collaborator_fixes_and_our_other_changes_do_not_overwrite_each_other(self):
        original={'a.class':b'original-a','b.class':b'original-b'}
        maintained={'a.class':b'original-a','b.class':b'our-fix'}
        compat={'a.class':b'collaborator-fix','b.class':b'original-b','compat/new.class':b'new'}
        changes=compatibility_overrides(original,maintained,compat)
        self.assertEqual({'a.class':b'collaborator-fix','compat/new.class':b'new'},changes)
        self.assertEqual(b'our-fix',maintained['b.class'])

    def test_overlapping_class_edits_require_review(self):
        with self.assertRaisesRegex(ValueError,'Overlapping'):
            compatibility_overrides({'a.class':b'original'},{'a.class':b'our-fix'},{'a.class':b'collaborator-fix'})

    def test_unreviewed_compatibility_resource_changes_are_rejected(self):
        with self.assertRaisesRegex(ValueError,'resource'):
            compatibility_overrides({'example.lang':b'old'},{'example.lang':b'old'},{'example.lang':b'different'})

    def test_language_merge_preserves_unrelated_text_and_has_one_value_per_key(self):
        old=b'# Existing comment\nunchanged=Keep me\npoint=old\npoint=previous patch\n'
        patch=b'point=new %s\nnewKey=New value\n'
        merged=merge_language(old,patch)
        self.assertEqual(b'# Existing comment\nunchanged=Keep me\npoint=new %s\nnewKey=New value\n',merged)
        self.assertEqual(merged,merge_language(merged,patch))


if __name__=='__main__':
    unittest.main()
