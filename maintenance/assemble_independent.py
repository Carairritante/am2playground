"""Assemble a private playable JAR from locally supplied Playground binaries.

This repository supplies authored tools and payloads, never the original mod.
Inputs are read-only. Outputs must be outside the public source repository.
"""
from collections import OrderedDict
from pathlib import Path
import argparse
import hashlib
import json
import os
import shutil
import subprocess
import tempfile
import zipfile

from merge_compendium import compendium_replacements

ROOT=Path(__file__).resolve().parent
REPOSITORY=ROOT.parent
DISPATCHER='airburn/am2playground/preloader/PGClassTransformer.class'
PAYLOADS=(
    'airburn/am2playground/compat/thaumcraft/PGRitualPortalRewards.class',
    'airburn/am2playground/items/PGSilverOrbRewards.class',
    'airburn/am2playground/preloader/ManaRegenTransformFix.class',
    'airburn/am2playground/preloader/IndependentCompatibilityTransform.class',
    'airburn/am2playground/utils/ManaRegenEvents.class',
    'airburn/am2playground/progression/SilverProgressionRules.class',
    'airburn/am2playground/progression/SilverOculusHooks.class',
    'airburn/am2playground/progression/SilverProgression.class',
)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def read_jar(path):
    with zipfile.ZipFile(path) as jar:
        names=jar.namelist()
        if len(names)!=len(set(names)):
            raise ValueError('Duplicate entries in input JAR: '+str(path))
        return OrderedDict((name,jar.read(name)) for name in names),jar.infolist()


def language_key(line):
    clean=line.strip()
    if not clean or clean.startswith(('#','//')) or '=' not in clean:
        return None
    return clean.split('=',1)[0].strip()


def merge_language(existing,patch):
    """Last value wins by key; unrelated values/comments survive; keys occur once."""
    lines=existing.decode('utf-8-sig').splitlines()
    values=OrderedDict()
    for line in lines+patch.decode('utf-8-sig').splitlines():
        key=language_key(line)
        if key is not None:
            values[key]=line.split('=',1)[1]
    output=[]
    seen=set()
    for line in lines:
        key=language_key(line)
        if key is None:
            output.append(line)
        elif key not in seen:
            output.append(key+'='+values[key]);seen.add(key)
    for key,value in values.items():
        if key not in seen:
            output.append(key+'='+value);seen.add(key)
    result=('\n'.join(output).rstrip('\n')+'\n').encode('utf-8')
    keys=[language_key(line) for line in result.decode('utf-8').splitlines()]
    keys=[key for key in keys if key is not None]
    if len(keys)!=len(set(keys)):
        raise AssertionError('Duplicate language keys after merging')
    return result


def compatibility_overrides(original,patched,compat):
    """Preserve collaborator fixes, rejecting overlapping edits instead of picking one."""
    changes={}
    conflicts=[]
    for name in sorted(set(original)|set(compat)):
        if name.endswith('/') or original.get(name)==compat.get(name):
            continue
        if not name.endswith('.class'):
            raise ValueError('Compatibility resource also differs; review separately: '+name)
        if name not in compat:
            raise ValueError('Compatibility source removes an original class: '+name)
        inherited=original.get(name)
        maintained=patched.get(name)
        candidate=compat[name]
        if maintained is not None and maintained!=inherited and maintained!=candidate:
            conflicts.append(name)
        else:
            changes[name]=candidate
    if conflicts:
        raise ValueError('Overlapping maintenance/compatibility edits require review: '+', '.join(conflicts))
    return changes


def input_record(path):
    return {'filename':path.name,'sha256':sha(path.read_bytes())}


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--patched',type=Path,required=True,help='Previously maintained advanced-Silver JAR.')
    parser.add_argument('--original',type=Path,required=True,help='Unmodified matching Playground release.')
    parser.add_argument('--compat-source',type=Path,help='Optional collaborator variant of the same release.')
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--manifest',type=Path,help='Default: output filename with .manifest.json suffix.')
    parser.add_argument('--java',default='java')
    parser.add_argument('--asm',type=Path,default=ROOT/'build/dependencies/asm-all-5.0.3.jar')
    parser.add_argument('--payload',type=Path,default=ROOT/'build/payload')
    parser.add_argument('--patchers',type=Path,default=ROOT/'build/patchers')
    parser.add_argument('--overwrite',action='store_true',help='Explicitly replace an existing assembled output only.')
    args=parser.parse_args()
    output=args.output.resolve()
    manifest=args.manifest.resolve() if args.manifest else output.with_suffix('.manifest.json')
    checksum=output.with_suffix('.SHA256SUMS')
    inputs=[args.patched,args.original]+([args.compat_source] if args.compat_source else [])
    if any(not path.is_file() for path in inputs):
        parser.error('Every supplied input JAR must exist.')
    for destination in [output,manifest,checksum]:
        if destination==REPOSITORY or REPOSITORY in destination.parents:
            parser.error('Private assembled outputs must stay outside the public repository.')
        if destination in [path.resolve() for path in inputs]:
            parser.error('An output cannot overwrite an input JAR.')
        if destination.exists() and not args.overwrite:
            parser.error('Output already exists; choose a new path or explicitly use --overwrite: '+str(destination))
    if not args.asm.is_file() or not (args.patchers/'PatchIndependence.class').is_file():
        parser.error('Compile the maintenance tools first (ci.py).')
    patched,infos=read_jar(args.patched)
    original,_=read_jar(args.original)
    changes={}
    compat_changes={}
    if args.compat_source:
        compat,_=read_jar(args.compat_source)
        compat_changes=compatibility_overrides(original,patched,compat)
        changes.update(compat_changes)
    if DISPATCHER not in patched or DISPATCHER not in original:
        raise ValueError('Input is not a matching Playground release with its dispatcher.')
    for name in PAYLOADS:
        path=args.payload/name
        if not path.is_file():
            raise ValueError('Missing authored payload: '+str(path))
        data=path.read_bytes()
        if data[:4]!=b'\xca\xfe\xba\xbe' or int.from_bytes(data[6:8],'big')>52:
            raise ValueError('Payload is not Java 8 class data: '+name)
        changes[name]=data
    with zipfile.ZipFile(args.patched) as jar:
        changes.update(compendium_replacements(jar))
    for language in ['en_US','pt_BR']:
        name='assets/am2pg/lang/'+language+'.lang'
        changes[name]=merge_language(patched.get(name,b''),(ROOT/'resources'/name).read_bytes())
    output.parent.mkdir(parents=True,exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='.am2pg-assembly-',dir=output.parent) as directory:
        stage=Path(directory)
        source=stage/'dispatcher-input.class';target=stage/'dispatcher-output.class'
        source.write_bytes(compat_changes.get(DISPATCHER,patched[DISPATCHER]))
        subprocess.run([args.java,'-cp',os.pathsep.join([str(args.patchers.resolve()),str(args.asm.resolve())]),
                        'PatchIndependence',str(source),str(target)],check=True)
        changes[DISPATCHER]=target.read_bytes()
        if b'airburn/am2playground/preloader/IndependentCompatibilityTransform' not in changes[DISPATCHER]:
            raise AssertionError('The dispatcher is missing the independent compatibility hook')
        assembled=stage/'assembled.jar'
        with zipfile.ZipFile(assembled,'w',zipfile.ZIP_DEFLATED) as jar:
            for info in infos:
                jar.writestr(info,changes.get(info.filename,patched[info.filename]))
            for name in sorted(set(changes)-set(patched)):
                info=zipfile.ZipInfo(name,date_time=(1980,1,1,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED
                jar.writestr(info,changes[name])
        final,_=read_jar(assembled)
        untouched=0
        for name,data in patched.items():
            if name not in changes:
                if final.get(name)!=data:
                    raise AssertionError('Unrelated maintained entry changed: '+name)
                untouched+=1
        preserved={}
        for name,data in compat_changes.items():
            if name==DISPATCHER:
                continue
            if final[name]!=data:
                raise AssertionError('Collaborator compatibility class was not preserved: '+name)
            preserved[name]=sha(data)
        for name in PAYLOADS:
            if final[name]!=(args.payload/name).read_bytes():
                raise AssertionError('Authored payload differs after assembly: '+name)
        changed=sorted(name for name in final if patched.get(name)!=final[name])
        digest=sha(assembled.read_bytes())
        record={'output':output.name,'sha256':digest,'patched':input_record(args.patched),
                'original':input_record(args.original),'compat_source':input_record(args.compat_source) if args.compat_source else None,
                'compatibility_entries':sorted(compat_changes),'compatibility_entries_preserved_exactly':preserved,
                'dispatcher_source_sha256':sha(source.read_bytes()),'dispatcher_output_sha256':sha(final[DISPATCHER]),
                'dispatcher_hook_applied':True,'authored_payloads':list(PAYLOADS),'changed_or_added':changed,
                'unrelated_entries_preserved':True,'unrelated_entry_count':untouched,
                'language_keys_unique':True,'compendium_requirements_merged':True,
                'original_inputs_not_modified':True,'playable_jar_private_only':True}
        for destination in [manifest,checksum]:
            destination.parent.mkdir(parents=True,exist_ok=True)
        shutil.copy2(assembled,output)
        manifest.write_text(json.dumps(record,indent=2),encoding='utf-8')
        checksum.write_text(digest+'  '+output.name+'\n',encoding='ascii')
    print(json.dumps({'output':output.name,'sha256':digest,'compatibility_classes':len(compat_changes),
                      'authored_payloads':len(PAYLOADS),'unrelated_entries_preserved':True,'manifest':str(manifest)},indent=2))


if __name__=='__main__':
    main()
