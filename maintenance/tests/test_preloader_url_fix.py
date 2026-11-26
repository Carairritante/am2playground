"""Verify PGPreloader URL percent-decoding (%20 spaces) and missing-file filtering end-to-end."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PRELOADER_ENTRY = 'airburn/am2playground/preloader/PGPreloader.class'

HARNESS_JAVA = r"""
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.jar.JarFile;
import airburn.am2playground.preloader.PreloaderUrlFix;

public final class PreloaderUrlHarness {
    static final class SingleClassLoader extends ClassLoader {
        private final byte[] bytes;
        SingleClassLoader(ClassLoader parent, byte[] bytes) {
            super(parent);
            this.bytes = bytes;
        }
        Class<?> loadPreloader() {
            return defineClass("airburn.am2playground.preloader.PGPreloader", bytes, 0, bytes.length);
        }
    }

    public static void main(String[] args) throws Exception {
        byte[] patched = Files.readAllBytes(Paths.get(args[0]));
        File baseDir = new File(args[1]);
        File missingJar = new File(args[2]);
        File dirNamedJar = new File(args[3]);

        SingleClassLoader cl = new SingleClassLoader(PreloaderUrlHarness.class.getClassLoader(), patched);
        Class<?> preloader = cl.loadPreloader();

        Method mapUrl = null;
        Method filterFile = null;
        for (Method m : preloader.getDeclaredMethods()) {
            Class<?>[] params = m.getParameterTypes();
            if (params.length == 1 && params[0] == URL.class && m.getReturnType() == File.class) {
                m.setAccessible(true);
                mapUrl = m;
            } else if (params.length == 1 && params[0] == File.class && m.getReturnType() == boolean.class) {
                m.setAccessible(true);
                filterFile = m;
            }
        }
        if (mapUrl == null || filterFile == null) {
            throw new AssertionError("Expected patched PGPreloader URL mapper and File filter lambdas");
        }

        // Test every URL passed on args[4..]
        for (int i = 4; i < args.length; i++) {
            URL u = new URL(args[i]);
            File resolved = (File) mapUrl.invoke(null, u);
            if (!resolved.isFile()) {
                throw new AssertionError("Resolved file does not exist for URL " + args[i] + " -> " + resolved.getAbsolutePath());
            }
            if (!Boolean.TRUE.equals(filterFile.invoke(null, resolved))) {
                throw new AssertionError("Filter rejected valid jar " + resolved.getAbsolutePath());
            }
            try (JarFile jf = new JarFile(resolved)) {
                if (jf.getEntry("META-INF/MANIFEST.MF") == null) {
                    throw new AssertionError("Missing manifest in " + resolved.getAbsolutePath());
                }
            }
        }

        // Non-existent jar and directory ending with .jar must be filtered out
        if (Boolean.TRUE.equals(filterFile.invoke(null, missingJar))) {
            throw new AssertionError("Filter accepted non-existent jar: " + missingJar);
        }
        if (Boolean.TRUE.equals(filterFile.invoke(null, dirNamedJar))) {
            throw new AssertionError("Filter accepted directory named .jar: " + dirNamedJar);
        }

        // Direct PreloaderUrlFix edge-case check for literal '+' preservation
        String decoded = PreloaderUrlFix.decodePercent("/Forbidden%20Series(3)/mod+extra%20v1.jar");
        if (!"/Forbidden Series(3)/mod+extra v1.jar".equals(decoded)) {
            throw new AssertionError("decodePercent corrupted '+' or '%20': " + decoded);
        }
        if (!baseDir.isDirectory()) {
            throw new AssertionError("Expected baseDir to exist");
        }
        System.out.println("PRELOADER_URL_FIX_OK");
    }
}
"""


def make_min_jar(path: Path):
    path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(path, 'w', zipfile.ZIP_DEFLATED) as zf:
        zf.writestr('META-INF/MANIFEST.MF', 'Manifest-Version: 1.0\n\n')


class PreloaderUrlFixTests(unittest.TestCase):
    def test_patched_preloader_decodes_spaces_brackets_plus_utf8_and_filters_missing(self):
        patchers = ROOT / 'build/patchers'
        asm = ROOT / 'build/dependencies/asm-all-5.0.3.jar'
        payload = ROOT / 'build/payload'
        forge = ROOT / 'build/dependencies/forge-1.7.10-1614.jar'
        server = ROOT / 'build/dependencies/minecraft-server-1.7.10.jar'
        base_jar = ROOT / 'base/AM2PlayGround-Beta-0.12.1-CR1.jar'

        with tempfile.TemporaryDirectory() as tmp:
            stage = Path(tmp)
            pre_in = stage / 'PGPreloader.in.class'
            pre_out = stage / 'PGPreloader.out.class'
            with zipfile.ZipFile(base_jar) as zf:
                pre_in.write_bytes(zf.read(PRELOADER_ENTRY))

            subprocess.run(
                ['java', '-cp', os.pathsep.join([str(patchers), str(asm)]), 'PatchPreloader', str(pre_in), str(pre_out)],
                check=True,
            )
            # Re-patching an already patched class must fail cleanly
            dup = subprocess.run(
                ['java', '-cp', os.pathsep.join([str(patchers), str(asm)]), 'PatchPreloader', str(pre_out), str(stage / 'dup.class')],
                capture_output=True, text=True,
            )
            self.assertNotEqual(dup.returncode, 0)

            # Create real folder named "Forbidden Series(3)" and accented folder
            pack_dir = stage / 'Forbidden Series(3)'
            jar_basic = pack_dir / 'libraries' / 'sample-lib.jar'
            jar_brackets = pack_dir / 'mods' / 'CoFHCore-[1.7.10]3.1.4.jar'
            jar_plus = pack_dir / 'mods' / 'mod+plus 1.0.jar'
            jar_utf8 = stage / 'Série Proibida (3)' / 'accented.jar'
            dir_jar = pack_dir / 'fake_dir.jar'
            dir_jar.mkdir(parents=True, exist_ok=True)
            missing_jar = pack_dir / 'does_not_exist.jar'

            for p in (jar_basic, jar_brackets, jar_plus, jar_utf8):
                make_min_jar(p)

            # Build test URLs: standard URI (%20), raw unescaped brackets + %20, jar:file: wrapper, UTF-8 %C3%A9
            url_basic = jar_basic.as_uri()
            url_raw_brackets = jar_basic.parent.parent.as_uri() + '/mods/CoFHCore-[1.7.10]3.1.4.jar'
            url_plus = jar_plus.as_uri()
            url_utf8 = jar_utf8.as_uri()
            url_jar_wrapped = 'jar:' + url_basic + '!/'

            harness_src = stage / 'PreloaderUrlHarness.java'
            harness_src.write_text(HARNESS_JAVA, encoding='utf-8')
            subprocess.run(
                ['javac', '--release', '8', '-cp', os.pathsep.join([str(payload), str(forge)]), '-d', str(stage), str(harness_src)],
                check=True,
            )
            run = subprocess.run(
                [
                    'java',
                    '-cp', os.pathsep.join([str(stage), str(payload), str(forge), str(server)]),
                    'PreloaderUrlHarness',
                    str(pre_out),
                    str(pack_dir),
                    str(missing_jar),
                    str(dir_jar),
                    url_basic,
                    url_raw_brackets,
                    url_plus,
                    url_utf8,
                    url_jar_wrapped,
                ],
                capture_output=True,
                text=True,
            )
            self.assertEqual(run.returncode, 0, msg=f"stdout={run.stdout}\nstderr={run.stderr}")
            self.assertIn('PRELOADER_URL_FIX_OK', run.stdout)


if __name__ == '__main__':
    unittest.main()
