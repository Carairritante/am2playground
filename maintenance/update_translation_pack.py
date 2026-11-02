"""Keep the existing PT-BR resource pack aligned with its maintained source files.

Only the Playground PT-BR XML and language entries in the ZIP are replaced.
Other resources, including the complete native AM2 translation, are preserved.
"""
from pathlib import Path
import argparse
import hashlib
import shutil
import zipfile

ROOT=Path(__file__).resolve().parent.parent
PACK_NAME='Compendium-AM2-Playground-PT-BR.zip'
XML_ENTRY='assets/am2pg/docs/ArcaneCompendium_pt_BR.xml'
LANG_ENTRY='assets/am2pg/lang/pt_BR.lang'


def language_values(text):
    values={}
    for line in text.splitlines():
        if not line.strip() or line.lstrip().startswith(('#','!')) or '=' not in line:
            continue
        key,value=line.split('=',1)
        key=key.strip()
        if key in values:
            raise ValueError('Duplicate language key: '+key)
        values[key]=value
    return values


def merge_language(original, updates):
    """Replace matching keys in place and append missing keys without duplication."""
    current=language_values(original)
    additions=language_values(updates)
    lines=[]
    for line in original.splitlines():
        if line.lstrip().startswith(('#','!')) or '=' not in line:
            lines.append(line)
            continue
        key=line.split('=',1)[0].strip()
        lines.append(key+'='+additions[key] if key in additions else line)
    missing=[key for key in additions if key not in current]
    if missing:
        if lines and lines[-1].strip():lines.append('')
        lines.append('# Mensagens da manutencao / Maintenance messages')
        lines.extend(key+'='+additions[key] for key in missing)
    result='\n'.join(lines).rstrip('\n')+'\n'
    language_values(result)
    return result


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def update_checksum(path, filename, digest):
    lines=path.read_text(encoding='utf-8').splitlines() if path.exists() else []
    output=[]
    found=False
    for line in lines:
        fields=line.split(None,1)
        if len(fields)==2 and fields[1].lstrip('*')==filename:
            if not found:output.append(digest+'  '+filename)
            found=True
        else:output.append(line)
    if not found:output.append(digest+'  '+filename)
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text('\n'.join(output).rstrip('\n')+'\n',encoding='utf-8')


def verify_pack(root=ROOT):
    """Fail when a published ZIP or language file no longer matches the sources."""
    for language in ('pt_BR','en_US'):
        values=language_values((root/'am2pg/lang'/(language+'.lang')).read_text(encoding='utf-8-sig'))
        updates=language_values((root/'maintenance/resources/assets/am2pg/lang'/(language+'.lang')).read_text(encoding='utf-8-sig'))
        for key,value in updates.items():
            if values.get(key)!=value:raise ValueError('Public language differs from maintenance: '+language+': '+key)
    pack=root/'translations'/PACK_NAME
    with zipfile.ZipFile(pack) as archive:
        names=archive.namelist()
        if len(names)!=len(set(names)):raise ValueError('Duplicate resource pack entries')
        if archive.read(XML_ENTRY)!=(root/'am2pg/docs/ArcaneCompendium_pt_BR.xml').read_bytes():
            raise ValueError('Packed Playground XML differs from the maintained source')
        if archive.read(LANG_ENTRY)!=(root/'am2pg/lang/pt_BR.lang').read_bytes():
            raise ValueError('Packed Playground language differs from the maintained source')
        language_values(archive.read(LANG_ENTRY).decode('utf-8-sig'))
    checksum=(root/'translations/SHA256SUMS').read_text(encoding='utf-8')
    if sha(pack)+'  '+PACK_NAME not in checksum.splitlines():
        raise ValueError('Resource pack SHA256SUMS is stale')
    return sha(pack)


def update_pack(root=ROOT):
    for language in ('pt_BR','en_US'):
        target=root/'am2pg/lang'/(language+'.lang')
        source=root/'maintenance/resources/assets/am2pg/lang'/(language+'.lang')
        target.write_text(merge_language(target.read_text(encoding='utf-8-sig'),
                                        source.read_text(encoding='utf-8-sig')),encoding='utf-8',newline='\n')
    pack=root/'translations'/PACK_NAME
    replacements={XML_ENTRY:(root/'am2pg/docs/ArcaneCompendium_pt_BR.xml').read_bytes(),
                  LANG_ENTRY:(root/'am2pg/lang/pt_BR.lang').read_bytes()}
    with zipfile.ZipFile(pack) as source:
        entries=source.infolist()
        original={entry.filename:source.read(entry.filename) for entry in entries}
        comment=source.comment
    if len(entries)!=len(original):raise ValueError('Duplicate resource pack entries')
    if not set(replacements).issubset(original):raise ValueError('Expected Playground resources are missing')
    if any(original[name]!=data for name,data in replacements.items()):
        temporary=pack.with_suffix('.zip.pending')
        try:
            with zipfile.ZipFile(temporary,'w') as target:
                target.comment=comment
                for entry in entries:
                    target.writestr(entry,replacements.get(entry.filename,original[entry.filename]))
            with zipfile.ZipFile(temporary) as target:
                if set(target.namelist())!=set(original):raise ValueError('Resource pack file list changed')
                for name,data in original.items():
                    if name not in replacements and target.read(name)!=data:
                        raise ValueError('Unrelated resource changed: '+name)
            temporary.replace(pack)
        finally:
            if temporary.exists():temporary.unlink()
    update_checksum(root/'translations/SHA256SUMS',PACK_NAME,sha(pack))
    return verify_pack(root)


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check',action='store_true',help='Verify source equality and checksum without changing files.')
    parser.add_argument('--copy-to',type=Path,help='Also copy the updated pack to this output ZIP.')
    parser.add_argument('--copy-checksum',type=Path,help='Update this additional checksum file for the output ZIP.')
    args=parser.parse_args()
    if args.check and (args.copy_to or args.copy_checksum):parser.error('--check does not copy or write files')
    if args.copy_checksum and not args.copy_to:parser.error('--copy-checksum requires --copy-to')
    digest=verify_pack() if args.check else update_pack()
    if args.copy_to:
        args.copy_to.parent.mkdir(parents=True,exist_ok=True)
        shutil.copy2(ROOT/'translations'/PACK_NAME,args.copy_to)
        if sha(args.copy_to)!=digest:raise ValueError('Output copy has a different checksum')
        if args.copy_checksum:update_checksum(args.copy_checksum,args.copy_to.name,digest)
    print(digest+'  '+PACK_NAME)


if __name__=='__main__':main()
