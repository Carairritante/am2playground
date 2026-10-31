import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Repair the generated regeneration hooks after the original transformer runs. */
public final class PatchManaRegen implements Opcodes {
 public static void main(String[] args)throws Exception {
  ClassNode c=new ClassNode();new ClassReader(Files.readAllBytes(Paths.get(args[0]))).accept(c,0);
  String helper="airburn/am2playground/preloader/ManaRegenTransformFix";
  int patched=0;
  for(MethodNode m:(List<MethodNode>)c.methods) if(m.name.equals("manaBurnoutTick")) {
   for(AbstractInsnNode i:m.instructions.toArray()) {
    if(i instanceof MethodInsnNode && ((MethodInsnNode)i).owner.equals(helper))throw new IllegalStateException("Already patched");
    if(i.getOpcode()==IRETURN && i.getPrevious().getOpcode()==ICONST_1) {
     m.instructions.remove(i.getPrevious());
     InsnList hook=new InsnList();hook.add(new VarInsnNode(ALOAD,0));
     hook.add(new MethodInsnNode(INVOKESTATIC,helper,"repair","(Lorg/objectweb/asm/tree/MethodNode;)Z",false));
     m.instructions.insertBefore(i,hook);patched++;
    }
   }
  }
  if(patched!=1)throw new IllegalStateException("Unexpected transformer layout: "+patched);
  ClassWriter out=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(out);
  Files.write(Paths.get(args[1]),out.toByteArray());
 }
}
