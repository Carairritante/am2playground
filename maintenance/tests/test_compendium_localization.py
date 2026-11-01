import unittest,sys
from pathlib import Path
import xml.etree.ElementTree as E
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from merge_compendium import merge_compendium,MARKERS

class LocalizationTests(unittest.TestCase):
 def test_all_entries_and_technical_values(self):
  root=Path(__file__).resolve().parents[2]
  source=E.fromstring(merge_compendium((root/'am2pg/docs/ArcaneCompendium_en_US.xml').read_bytes(),'en_US'))
  target=E.parse(root/'am2pg/docs/ArcaneCompendium_pt_BR.xml').getroot()
  a=list(source.iter());b=list(target.iter());self.assertEqual(len(a),len(b))
  from collections import Counter
  import re
  for en,pt in zip(a,b):
   self.assertEqual(en.tag,pt.tag);self.assertEqual(en.attrib,pt.attrib)
   x=(en.text or '').strip();y=(pt.text or '').strip()
   if en.tag in ('name','desc'):
    if x:self.assertTrue(y)
    if en.tag=='desc' and x:
     self.assertNotEqual(x,y)
     self.assertEqual(Counter(re.findall(r'#[0-9a-zA-Z]|![a-zA-Z]',x)),Counter(re.findall(r'#[0-9a-zA-Z]|![a-zA-Z]',y)))
   else:self.assertEqual(x,y)
 def test_merger_keeps_full_portuguese_translation(self):
  root=Path(__file__).resolve().parents[2];source=(root/'am2pg/docs/ArcaneCompendium_pt_BR.xml').read_bytes()
  merged=merge_compendium(source,'pt_BR');self.assertEqual(merged,merge_compendium(merged,'pt_BR'))
  parsed=E.fromstring(merged)
  self.assertEqual('Míssil',parsed.find(".//*[@id='Missile']/name").text)
  self.assertEqual(1,parsed.find(".//*[@id='Nullify']/desc").text.count(MARKERS['pt_BR']))
  legacy=source.decode('utf-8').replace(MARKERS['pt_BR'],'!p #5Requisitos Silver adicionais#0 !l ').encode('utf-8')
  self.assertEqual(merged,merge_compendium(legacy,'pt_BR'))
if __name__=='__main__':unittest.main()
