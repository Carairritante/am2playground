package airburn.am2playground.preloader;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Canceled stages must skip forward, while retaining the event's adjusted duration. */
public final class ManaRegenTransformFix implements Opcodes {
 private ManaRegenTransformFix() {}
 public static boolean repair(MethodNode method) {
  String[] names={"onManaRegen_ArmorMaterial","onManaRegen_SkillData","onManaRegen_ArmorInfusion","onManaRegen_End"};
  MethodInsnNode[] calls=new MethodInsnNode[4];
  for(AbstractInsnNode i:method.instructions.toArray())if(i instanceof MethodInsnNode) {
   MethodInsnNode call=(MethodInsnNode)i;
   if(!call.owner.equals("airburn/am2playground/preloader/PGEventFactories"))continue;
   for(int n=0;n<names.length;n++)if(call.name.equals(names[n])) {
    if(calls[n]!=null)throw new IllegalStateException("Duplicate regeneration hook");
    calls[n]=call;
   }
  }
  LabelNode[] starts=new LabelNode[4];
  for(int n=0;n<4;n++) {
   if(calls[n]==null)throw new IllegalStateException("Missing regeneration hook: "+names[n]);
   AbstractInsnNode load=previous(previous(calls[n]));
   if(!(load instanceof VarInsnNode)||load.getOpcode()!=ALOAD||((VarInsnNode)load).var!=3)
    throw new IllegalStateException("Unexpected regeneration arguments");
   starts[n]=new LabelNode();method.instructions.insertBefore(load,starts[n]);
  }
  for(int n=0;n<3;n++) {
   AbstractInsnNode[] old=new AbstractInsnNode[5];AbstractInsnNode at=calls[n];
   for(int k=0;k<5;k++)old[k]=at=next(at);
   if(old[0].getOpcode()!=ISTORE||old[1].getOpcode()!=ILOAD||old[2].getOpcode()!=IFEQ
     ||old[3].getOpcode()!=ILOAD||old[4].getOpcode()!=ISTORE)
    throw new IllegalStateException("Unexpected regeneration branch");
   for(AbstractInsnNode i:old)method.instructions.remove(i);
   calls[n].owner="airburn/am2playground/utils/ManaRegenEvents";
   InsnList fixed=new InsnList();
   fixed.add(new VarInsnNode(ISTORE,4));
   fixed.add(new VarInsnNode(ILOAD,4));
   fixed.add(new MethodInsnNode(INVOKESTATIC,"java/lang/Math","abs","(I)I",false));
   fixed.add(new VarInsnNode(ISTORE,2));
   fixed.add(new VarInsnNode(ILOAD,4));
   fixed.add(new JumpInsnNode(IFLT,starts[n+1]));
   method.instructions.insert(calls[n],fixed);
  }
  return true;
 }
 private static AbstractInsnNode previous(AbstractInsnNode i){do{i=i.getPrevious();}while(i!=null&&i.getOpcode()<0);return i;}
 private static AbstractInsnNode next(AbstractInsnNode i){do{i=i.getNext();}while(i!=null&&i.getOpcode()<0);return i;}
}
