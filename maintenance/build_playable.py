"""Build the playable maintenance JAR using the checked base and compiled sources."""
from pathlib import Path
import hashlib
import json
import re
import shutil
import zipfile

from assemble_independent import PAYLOADS, merge_language
from merge_compendium import compendium_replacements

ROOT = Path(__file__).resolve().parent


def build():
    version = json.loads((ROOT / 'VERSION.json').read_text(encoding='utf-8'))
    name = version['playable_jar']
    if not re.fullmatch(r'AM2PlayGround-Beta-0\.12\.1-CR[1-9][0-9]*\.jar', name):
        raise ValueError('Invalid playable artifact name')
    if name != 'AM2PlayGround-Beta-0.12.1-CR' + str(version['maintenance_revision']) + '.jar':
        raise ValueError('Filename and maintenance revision disagree')
    base = ROOT / version['build_base']
    base_data = base.read_bytes()
    if hashlib.sha256(base_data).hexdigest() != version['build_base_sha256']:
        raise ValueError('Playable build base checksum mismatch')
    payload = ROOT / 'build/payload'
    dist = ROOT / 'build/playable-dist'
    dist.mkdir(parents=True, exist_ok=True)
    if any(p.name != name for p in dist.glob('*.jar')):
        raise ValueError('Remove stale playable outputs before building another revision')
    with zipfile.ZipFile(base) as jar:
        if len(jar.namelist()) != len(set(jar.namelist())) or jar.testzip():
            raise ValueError('Invalid base archive')
        original = {n: jar.read(n) for n in jar.namelist()}
        if 'FMLCorePlugin:' not in original['META-INF/MANIFEST.MF'].decode('utf-8'):
            raise ValueError('Missing Playground coremod manifest')
        if b'IndependentCompatibilityTransform' not in original['airburn/am2playground/preloader/PGClassTransformer.class']:
            raise ValueError('Missing independent dispatcher hook')
        changes = {}
        for entry in PAYLOADS:
            data = (payload / entry).read_bytes()
            if data[:4] != b'\xca\xfe\xba\xbe' or int.from_bytes(data[6:8], 'big') > 52:
                raise ValueError('Payload is not Java 8 bytecode: ' + entry)
            changes[entry] = data
        changes.update(compendium_replacements(jar))
        for language in ('en_US', 'pt_BR'):
            entry = 'assets/am2pg/lang/' + language + '.lang'
            changes[entry] = merge_language(original[entry], (ROOT / 'resources' / entry).read_bytes())
    final = dict(original)
    final.update(changes)
    changed = [n for n in sorted(final) if final[n] != original.get(n)]
    output = dist / name
    if not changed:
        # Keep the already validated archive byte-for-byte when sources produce no changes.
        shutil.copyfile(base, output)
    else:
        with zipfile.ZipFile(output, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as jar:
            for entry in sorted(final):
                info = zipfile.ZipInfo(entry, date_time=(1980, 1, 1, 0, 0, 0))
                info.compress_type = zipfile.ZIP_DEFLATED
                info.external_attr = (0o40755 if entry.endswith('/') else 0o100644) << 16
                jar.writestr(info, final[entry])
    with zipfile.ZipFile(output) as jar:
        if set(jar.namelist()) != set(final) or jar.testzip():
            raise ValueError('Playable output entries differ')
        for entry, data in final.items():
            if jar.read(entry) != data:
                raise ValueError('Playable entry differs: ' + entry)
        for entry in original:
            if entry not in changes and jar.read(entry) != original[entry]:
                raise ValueError('Unrelated base entry changed: ' + entry)
    digest = hashlib.sha256(output.read_bytes()).hexdigest()
    line = digest + '  ' + name + '\n'
    (dist / 'SHA256SUMS').write_text(line, encoding='ascii')
    record = {'playable_jar': name, 'sha256': digest,
              'compiled_payload_count': len(PAYLOADS), 'changed_entries': changed,
              'base_archive_preserved_exactly': not changed,
              'unrelated_entries_preserved': True}
    (ROOT / 'build/playable-build.json').write_text(json.dumps(record, indent=2), encoding='utf-8')
    print(json.dumps(record, indent=2))
    return record


if __name__ == '__main__':
    build()
