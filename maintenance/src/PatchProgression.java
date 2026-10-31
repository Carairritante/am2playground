import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Add new learning hooks without redistributing original mod classes or sources. */
public final class PatchProgression implements Opcodes {
 public static void main(String[] args)throws Exception {
  ClassNode c=new ClassNode();new ClassReader(Files.readAllBytes(Paths.get(args[0]))).accept(c,0);
  String helper="airburn/am2playground/progression/SilverProgression";int changes=0;
  for(MethodNode m:(List<MethodNode>)c.methods) {
   for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof MethodInsnNode&&((MethodInsnNode)i).owner.equals(helper))throw new IllegalStateException("Progression already patched");
   if(args[2].equals("init")&&m.name.equals("postInit")) {
    for(AbstractInsnNode i:m.instructions.toArray())if(i.getOpcode()==RETURN){m.instructions.insertBefore(i,new MethodInsnNode(INVOKESTATIC,helper,"init","()V",false));changes++;}
   }
   if(args[2].equals("cast")&&m.name.equals("onSpellCast")) {
    for(AbstractInsnNode i:m.instructions.toArray())if(i.getOpcode()==RETURN){InsnList l=new InsnList();l.add(new VarInsnNode(ALOAD,1));l.add(new MethodInsnNode(INVOKESTATIC,helper,"onCast","(Ljava/lang/Object;)Z",false));l.add(new InsnNode(POP));m.instructions.insertBefore(i,l);changes++;}
   }
   boolean learn=args[2].equals("learn")&&m.name.equals("learnSkill")&&m.desc.equals("(Lnet/minecraft/entity/player/EntityPlayer;Lam2/api/spell/component/interfaces/ISkillTreeEntry;)Z");
   boolean orb=args[2].equals("orb")&&m.name.equals("func_77659_a");
   if(learn||orb) {
    InsnList l=new InsnList();LabelNode allowed=new LabelNode();
    l.add(new VarInsnNode(ALOAD,learn?0:1));l.add(new VarInsnNode(ALOAD,learn?1:3));
    l.add(new MethodInsnNode(INVOKESTATIC,helper,learn?"beforeLearn":"allowOrbUse","(Ljava/lang/Object;Ljava/lang/Object;)Z",false));
    l.add(new JumpInsnNode(IFNE,allowed));
    l.add(learn?new InsnNode(ICONST_0):new VarInsnNode(ALOAD,1));l.add(new InsnNode(learn?IRETURN:ARETURN));
    l.add(allowed);l.add(new FrameNode(F_SAME,0,null,0,null));m.instructions.insert(l);changes++;
   }
  }
  if(changes<1||(!args[2].equals("cast")&&changes!=1))throw new IllegalStateException("Unexpected progression hook layout: "+changes);
  ClassWriter out=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(out);Files.write(Paths.get(args[1]),out.toByteArray());
 }
}
