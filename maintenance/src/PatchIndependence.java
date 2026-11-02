import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Append the independent compatibility pass to Playground's existing transformer. */
public final class PatchIndependence implements Opcodes {
 public static void main(String[] args)throws Exception {
  ClassNode c=new ClassNode();new ClassReader(Files.readAllBytes(Paths.get(args[0]))).accept(c,0);
  String helper="airburn/am2playground/preloader/IndependentCompatibilityTransform";int count=0;
  for(MethodNode m:(List<MethodNode>)c.methods)if(m.name.equals("transform")&&m.desc.equals("(Ljava/lang/String;Ljava/lang/String;[B)[B")) {
   for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof MethodInsnNode&&((MethodInsnNode)i).owner.equals(helper))throw new IllegalStateException("Independent compatibility already installed");
   for(AbstractInsnNode i:m.instructions.toArray())if(i.getOpcode()==ARETURN) {
    InsnList l=new InsnList();l.add(new VarInsnNode(ALOAD,2));
    l.add(new MethodInsnNode(INVOKESTATIC,helper,"transform","([BLjava/lang/String;)[B",false));m.instructions.insertBefore(i,l);count++;
   }
  }
  if(count<1||count>8)throw new IllegalStateException("Unexpected Playground transformer return count: "+count);
  ClassWriter out=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(out);Files.write(Paths.get(args[1]),out.toByteArray());
 }
}
