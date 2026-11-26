import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Fix URL percent-decoding (%20 spaces) and missing-file filtering in PGPreloader.loadJars(). */
public final class PatchPreloader implements Opcodes {
    public static void main(String[] args) throws Exception {
        ClassNode c = new ClassNode();
        new ClassReader(Files.readAllBytes(Paths.get(args[0]))).accept(c, 0);
        String helper = "airburn/am2playground/preloader/PreloaderUrlFix";
        int urlPatched = 0;
        int filterPatched = 0;

        for (MethodNode m : (List<MethodNode>) c.methods) {
            for (AbstractInsnNode i : m.instructions.toArray()) {
                if (i instanceof MethodInsnNode && ((MethodInsnNode) i).owner.equals(helper)) {
                    throw new IllegalStateException("PGPreloader URL fix already installed");
                }
            }

            if (m.desc.equals("(Ljava/net/URL;)Ljava/io/File;")) {
                boolean callsGetFile = false;
                for (AbstractInsnNode i : m.instructions.toArray()) {
                    if (i instanceof MethodInsnNode) {
                        MethodInsnNode mi = (MethodInsnNode) i;
                        if (mi.owner.equals("java/net/URL") && mi.name.equals("getFile") && mi.desc.equals("()Ljava/lang/String;")) {
                            callsGetFile = true;
                            break;
                        }
                    }
                }
                if (callsGetFile) {
                    m.instructions.clear();
                    m.tryCatchBlocks.clear();
                    m.localVariables = null;
                    m.instructions.add(new VarInsnNode(ALOAD, 0));
                    m.instructions.add(new MethodInsnNode(INVOKESTATIC, helper, "toFile", "(Ljava/net/URL;)Ljava/io/File;", false));
                    m.instructions.add(new InsnNode(ARETURN));
                    urlPatched++;
                }
            } else if (m.desc.equals("(Ljava/io/File;)Z")) {
                for (AbstractInsnNode i : m.instructions.toArray()) {
                    if (i instanceof MethodInsnNode) {
                        MethodInsnNode mi = (MethodInsnNode) i;
                        if (mi.owner.equals("java/io/File") && mi.name.equals("isDirectory") && mi.desc.equals("()Z")) {
                            AbstractInsnNode next = mi.getNext();
                            if (next instanceof JumpInsnNode && next.getOpcode() == IFNE) {
                                mi.name = "isFile";
                                ((JumpInsnNode) next).setOpcode(IFEQ);
                                filterPatched++;
                            }
                        }
                    }
                }
            }
        }

        if (urlPatched != 1) {
            throw new IllegalStateException("Expected one URL->File mapper in PGPreloader, found " + urlPatched);
        }
        if (filterPatched != 1) {
            throw new IllegalStateException("Expected one File.isDirectory filter in PGPreloader, found " + filterPatched);
        }

        ClassWriter out = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        c.accept(out);
        Files.write(Paths.get(args[1]), out.toByteArray());
    }
}
