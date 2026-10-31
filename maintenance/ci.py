"""Compile independent tools; do not package API stubs or test mods."""
from pathlib import Path
import hashlib
import os
import subprocess
import sys
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parent
BUILD = ROOT / 'build'
DEPS = BUILD / 'dependencies'


def download(name, url, digest):
    path = DEPS / name
    DEPS.mkdir(parents=True, exist_ok=True)
    if not path.exists() or hashlib.sha256(path.read_bytes()).hexdigest() != digest:
        with urllib.request.urlopen(url, timeout=90) as response:
            data = response.read()
        if hashlib.sha256(data).hexdigest() != digest:
            raise RuntimeError('Dependency checksum mismatch: ' + name)
        path.write_bytes(data)
    return path


def main():
    asm = download('asm-all-5.0.3.jar',
        'https://repo.maven.apache.org/maven2/org/ow2/asm/asm-all/5.0.3/asm-all-5.0.3.jar',
        '09d44e6adddaab9fd89ea3164c439f71c755769fa8d3a940352ab3d2003b98ff')
    forge = download('forge-1.7.10-1614.jar',
        'https://maven.cleanroommc.com/net/minecraftforge/forge/1.7.10-10.13.4.1614-1.7.10/forge-1.7.10-10.13.4.1614-1.7.10-universal.jar',
        '00d1ca02192c7efb87da95552ebf247021f1e1d642f86d0a03076314def11529')
    server = download('minecraft-server-1.7.10.jar',
        'https://launcher.mojang.com/v1/objects/952438ac4e01b4d115c5fc38f891710c4941df29/server.jar',
        'c70870f00c4024d829e154f7e5f4e885b02dd87991726a3308d81f513972f3fc')
    subprocess.run([sys.executable, str(ROOT / 'compile-helper.py')], check=True)
    patchers = BUILD / 'patchers'
    subprocess.run(['javac', '--release', '8', '-cp', str(asm), '-d', str(patchers),
        str(ROOT / 'src/PatchCauldron.java'), str(ROOT / 'src/PatchPortal.java'),
        str(ROOT / 'src/PatchSilverOrb.java')], check=True)
    subprocess.run(['javac', '--release', '8', '-cp', os.pathsep.join([str(forge), str(server)]),
        '-d', str(BUILD / 'probes'), str(ROOT / 'tests/cauldronprobe/CauldronProbe.java'),
        str(ROOT / 'tests/portalprobe/PortalProbe.java')], check=True)
    dist = BUILD / 'dist'
    dist.mkdir(parents=True, exist_ok=True)
    output = dist / 'AM2PG-maintenance-tools.jar'
    with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as jar:
        jar.writestr('META-INF/MANIFEST.MF', 'Manifest-Version: 1.0\n\n')
        for directory in [patchers, BUILD / 'payload']:
            for path in sorted(directory.rglob('*.class')):
                data = path.read_bytes()
                if int.from_bytes(data[6:8], 'big') > 52:
                    raise RuntimeError('Class is not Java 8 compatible: ' + str(path))
                jar.writestr(path.relative_to(directory).as_posix(), data)
        for path in sorted((ROOT / 'resources').rglob('*.lang')):
            jar.write(path, path.relative_to(ROOT / 'resources').as_posix())
    with zipfile.ZipFile(output) as jar:
        expected = {'META-INF/MANIFEST.MF', 'PatchCauldron.class', 'PatchPortal.class', 'PatchSilverOrb.class',
            'airburn/am2playground/compat/thaumcraft/PGRitualPortalRewards.class',
            'airburn/am2playground/items/PGSilverOrbRewards.class',
            'assets/am2pg/lang/en_US.lang', 'assets/am2pg/lang/pt_BR.lang'}
        if set(jar.namelist()) != expected:
            raise RuntimeError('Unexpected classes packaged in tooling JAR')
    line = hashlib.sha256(output.read_bytes()).hexdigest() + '  ' + output.name + '\n'
    (dist / 'SHA256SUMS').write_text(line, encoding='utf-8')
    print(line, end='')
    summary = os.environ.get('GITHUB_STEP_SUMMARY')
    if summary:
        with open(summary, 'a', encoding='utf-8') as stream:
            stream.write('### Tooling build\n\nThis is a tools JAR, not an installable mod.\n\n```text\n' + line + '```\n')


if __name__ == '__main__':
    main()
