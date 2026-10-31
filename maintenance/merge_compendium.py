"""Merge original maintenance text into a local AM2PG compendium without publishing its body."""
from copy import deepcopy
from pathlib import Path
import argparse
import re
from xml.etree import ElementTree as ET
import zipfile

RESOURCE_DIR = Path(__file__).resolve().parent / 'resources/assets/am2pg/docs'
GUIDE_ID = 'am2pg_advanced_silver'
TARGETS = ('Level', 'Nullify', 'CovenConnection', 'FloweringNature', 'Diabolic', 'Ethereal', 'Sacrament')
MARKERS = {
    'en_US': '!p #5Advanced Silver requirements#0 !l ',
    'pt_BR': '!p #5Requisitos Silver adicionais#0 !l ',
}
COMPENDIUM_PATTERN = re.compile(r'^assets/am2pg/docs/ArcaneCompendium_(.+)\.xml$')


def _language(language):
    return 'pt_BR' if language == 'pt_BR' else 'en_US'


def _find_target(root, identifier):
    matches = [entry for entry in root.iter() if entry.get('id') == identifier]
    if len(matches) != 1:
        raise ValueError(f'Expected one compendium entry for {identifier}; found {len(matches)}')
    return matches[0]


def merge_compendium(source_xml, language='en_US'):
    """Return UTF-8 XML; preserve inherited descriptions and append only our requirements."""
    language = _language(language)
    root = ET.fromstring(source_xml)
    if root.tag != 'compendium':
        raise ValueError('Input is not an AM2 compendium')
    patch = ET.parse(RESOURCE_DIR / f'SilverProgression_{language}.xml').getroot()
    guides = root.find('guides')
    if guides is None:
        guides = ET.SubElement(root, 'guides')
    for old in list(guides):
        if old.get('id') == GUIDE_ID:
            guides.remove(old)
    guide = patch.find('guides/guide')
    if guide is None or guide.get('unlocked') != 'true':
        raise ValueError('The new requirements guide must be unlocked from the start')
    guides.append(deepcopy(guide))

    requirements = {entry.get('id'): ' '.join(''.join(entry.itertext()).split())
                    for entry in patch.findall('requirements/entry')}
    if set(requirements) != set(TARGETS):
        raise ValueError('The requirements patch must cover all seven advanced parts')
    for identifier in TARGETS:
        entry = _find_target(root, identifier)
        desc = entry.find('desc')
        if desc is None:
            desc = ET.SubElement(entry, 'desc')
        inherited = desc.text or ''
        # Reapplying or switching locale replaces our previous appendix, not upstream text.
        for marker in MARKERS.values():
            inherited = inherited.split(marker, 1)[0]
        desc.text = inherited.rstrip() + ' ' + MARKERS[language] + requirements[identifier]

    ET.indent(root, space='  ')
    merged = ET.tostring(root, encoding='utf-8', xml_declaration=True)
    validate_merged(merged, language)
    return merged


def validate_merged(xml, language='en_US'):
    root = ET.fromstring(xml)
    guide = _find_target(root, GUIDE_ID)
    if guide.tag != 'guide' or guide.get('unlocked') != 'true':
        raise ValueError('The requirements chapter is not an unlocked Guide')
    if len(guide.findall('subitem')) != 9:
        raise ValueError('Missing initiation, consecration or one of the seven requirement topics')
    marker = MARKERS[_language(language)]
    for identifier in TARGETS:
        desc = _find_target(root, identifier).findtext('desc', '')
        if desc.count(marker) != 1:
            raise ValueError(f'Missing or duplicate requirements for {identifier}')
    return True


def compendium_replacements(jar):
    """Return changed resources for a ZipFile; Portuguese falls back to the original English body."""
    resources = {}
    english_path = 'assets/am2pg/docs/ArcaneCompendium_en_US.xml'
    if english_path not in jar.namelist():
        raise ValueError('The addon is missing its English compendium')
    for name in jar.namelist():
        match = COMPENDIUM_PATTERN.fullmatch(name)
        if match:
            resources[name] = merge_compendium(jar.read(name), match.group(1))
    portuguese_path = 'assets/am2pg/docs/ArcaneCompendium_pt_BR.xml'
    if portuguese_path not in resources:
        resources[portuguese_path] = merge_compendium(jar.read(english_path), 'pt_BR')
    return resources


def patch_jar(source, destination):
    source, destination = Path(source), Path(destination)
    if source.resolve() == destination.resolve():
        raise ValueError('Use a new output path; never rewrite the source JAR in place')
    with zipfile.ZipFile(source) as original:
        changes = compendium_replacements(original)
        existing = set(original.namelist())
        with zipfile.ZipFile(destination, 'w') as result:
            for entry in original.infolist():
                result.writestr(entry, changes.get(entry.filename, original.read(entry.filename)))
            for name, data in changes.items():
                if name not in existing:
                    result.writestr(name, data)
    with zipfile.ZipFile(source) as original, zipfile.ZipFile(destination) as result:
        for name in original.namelist():
            if name not in changes and original.read(name) != result.read(name):
                raise AssertionError('Unrelated JAR entry changed: ' + name)
    return sorted(changes)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('input_jar')
    parser.add_argument('output_jar')
    args = parser.parse_args()
    for name in patch_jar(args.input_jar, args.output_jar):
        print(name)


if __name__ == '__main__':
    main()
