package airburn.am2playground.preloader;

import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Playground-owned overlays. This pass is never present when Playground is absent. */
public final class IndependentCompatibilityTransform implements Opcodes {
 private static final String MARKER="am2pg$independentCompatibility";
 private static final String UI="airburn/am2playground/progression/SilverOculusHooks";
 private static final String PROGRESS="airburn/am2playground/progression/SilverProgression";
 private IndependentCompatibilityTransform(){}
 public static byte[] transform(byte[] bytes,String transformedName) {
  if(bytes==null||transformedName==null)return bytes;
  String name=transformedName.replace('.','/');
  if(!name.equals("am2/spell/components/ManaLink")&&!name.equals("am2/playerextensions/SkillData")&&!name.equals("am2/guis/GuiSkillTrees"))return bytes;
  ClassNode c=new ClassNode();new ClassReader(bytes).accept(c,0);
  for(FieldNode f:(List<FieldNode>)c.fields)if(f.name.equals(MARKER))return bytes;
  int changed=name.endsWith("/ManaLink")?manaLink(c):name.endsWith("/SkillData")?skillData(c):gui(c);
  if(changed==0)throw new IllegalStateException("Unsupported AM2 compatibility layout: "+name);
  c.fields.add(new FieldNode(ACC_PRIVATE|ACC_STATIC|ACC_FINAL|ACC_SYNTHETIC,MARKER,"I",null,1));
  ClassWriter out=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(out);return out.toByteArray();
 }
 private static int manaLink(ClassNode c) {
  for(MethodNode m:(List<MethodNode>)c.methods)if(m.name.equals("applyEffectBlock")&&m.desc.equals("(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;IIIIDDDLnet/minecraft/entity/EntityLivingBase;)Z")) {
   InsnList l=new InsnList();LabelNode nativePath=new LabelNode();
   l.add(new VarInsnNode(ALOAD,0));l.add(new VarInsnNode(ALOAD,2));
   l.add(new VarInsnNode(ILOAD,3));l.add(new VarInsnNode(ILOAD,4));l.add(new VarInsnNode(ILOAD,5));l.add(new VarInsnNode(ALOAD,13));
   l.add(new MethodInsnNode(INVOKESTATIC,PROGRESS,"beforeWorldFusion","(Ljava/lang/Object;Ljava/lang/Object;IIILjava/lang/Object;)I",false));
   l.add(new InsnNode(DUP));l.add(new InsnNode(ICONST_M1));l.add(new JumpInsnNode(IF_ICMPEQ,nativePath));l.add(new InsnNode(IRETURN));
   l.add(nativePath);l.add(new FrameNode(F_SAME1,0,null,1,new Object[]{INTEGER}));l.add(new InsnNode(POP));m.instructions.insert(l);return 1;
  }
  return 0;
 }
 private static int skillData(ClassNode c) {
  int changed=0;
  for(MethodNode m:(List<MethodNode>)c.methods) {
   if(m.name.equals("handlePacketData")&&m.desc.equals("([B)Z")) {
    InsnList l=new InsnList();LabelNode allowed=new LabelNode();
    l.add(new VarInsnNode(ALOAD,0));l.add(new VarInsnNode(ALOAD,1));
    l.add(new MethodInsnNode(INVOKESTATIC,UI,"allowPacket","(Ljava/lang/Object;[B)Z",false));
    l.add(new JumpInsnNode(IFNE,allowed));l.add(new InsnNode(ICONST_1));l.add(new InsnNode(IRETURN));
    l.add(allowed);l.add(new FrameNode(F_SAME,0,null,0,null));m.instructions.insert(l);changed++;
    boolean silverRead=false;
    for(AbstractInsnNode n:m.instructions.toArray())if(n instanceof MethodInsnNode) {
     MethodInsnNode call=(MethodInsnNode)n;
     if(call.name.equals("setSpellPoints")&&call.desc.equals("(ILam2/api/spell/enums/SkillPointTypes;)V"))silverRead=true;
    }
    if(!silverRead)for(AbstractInsnNode n:m.instructions.toArray())if(n instanceof MethodInsnNode) {
     MethodInsnNode call=(MethodInsnNode)n;
     if(call.name.equals("setSpellPoints")&&call.desc.equals("(III)V")) {
      // The existing call's receiver and AMDataReader remain in stable method locals.
      AbstractInsnNode p=previous(n);int reader=-1;
      for(int scan=0;p!=null&&scan<12;scan++,p=previous(p))if(p instanceof VarInsnNode&&p.getOpcode()==ALOAD&&((VarInsnNode)p).var!=0){reader=((VarInsnNode)p).var;break;}
      if(reader<0)throw new IllegalStateException("Missing skill packet reader local");
      InsnList sync=new InsnList();sync.add(new VarInsnNode(ALOAD,0));sync.add(new VarInsnNode(ALOAD,reader));
      sync.add(new MethodInsnNode(INVOKEVIRTUAL,"am2/network/AMDataReader","getInt","()I",false));
      sync.add(new FieldInsnNode(GETSTATIC,"am2/api/spell/enums/SkillPointTypes","SILVER","Lam2/api/spell/enums/SkillPointTypes;"));
      sync.add(new MethodInsnNode(INVOKEVIRTUAL,c.name,"setSpellPoints","(ILam2/api/spell/enums/SkillPointTypes;)V",false));m.instructions.insert(n,sync);changed++;
     }
    }
   }
   if(m.name.equals("getLearnState")&&m.desc.startsWith("(Lam2/api/SkillTreeEntry;")&&m.desc.endsWith("Lam2/api/spell/enums/LearnStates;")) {
    for(AbstractInsnNode n:m.instructions.toArray())if(n.getOpcode()==ARETURN){InsnList l=new InsnList();l.add(new VarInsnNode(ALOAD,1));l.add(new MethodInsnNode(INVOKESTATIC,UI,"filterLearnState","(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",false));l.add(new TypeInsnNode(CHECKCAST,"am2/api/spell/enums/LearnStates"));m.instructions.insertBefore(n,l);changed++;}
   }
  }
  return changed;
 }
 private static int gui(ClassNode c) {
  int changed=0;
  for(MethodNode m:(List<MethodNode>)c.methods)if((m.name.equals("func_73863_a")||m.name.equals("drawScreen"))&&m.desc.equals("(IIF)V")) {
   InsnList begin=new InsnList();begin.add(new VarInsnNode(ALOAD,0));begin.add(new MethodInsnNode(INVOKESTATIC,UI,"beginDraw","(Ljava/lang/Object;)V",false));m.instructions.insert(begin);changed++;
   int text=-1;
   for(AbstractInsnNode n:m.instructions.toArray()) {
    if(n instanceof TypeInsnNode&&n.getOpcode()==NEW&&((TypeInsnNode)n).desc.equals("java/util/ArrayList")) {
     AbstractInsnNode at=next(next(next(n)));
     if(at instanceof VarInsnNode&&at.getOpcode()==ASTORE)text=((VarInsnNode)at).var;
    }
   }
   for(AbstractInsnNode n:m.instructions.toArray())if(n instanceof MethodInsnNode) {
    MethodInsnNode call=(MethodInsnNode)n;
    if(call.getOpcode()==INVOKESPECIAL&&(call.name.equals("func_73863_a")||call.name.equals("drawScreen"))&&call.desc.equals("(IIF)V")) {
     InsnList l=new InsnList();l.add(new VarInsnNode(ALOAD,0));l.add(new MethodInsnNode(INVOKESTATIC,UI,"drawCounter","(Ljava/lang/Object;)V",false));m.instructions.insertBefore(n,l);changed++;
    }
    if((call.name.equals("func_78276_b")||call.name.equals("drawString"))&&call.desc.equals("(Ljava/lang/String;III)I")) {
     call.setOpcode(INVOKESTATIC);call.owner=UI;call.name="nativePointCount";call.desc="(Ljava/lang/Object;Ljava/lang/String;III)I";call.itf=false;changed++;
    }
    if((call.name.equals("func_73729_b")||call.name.equals("drawTexturedModalRect"))&&call.desc.equals("(IIIIII)V")&&is37(previous(n))&&is37(previous(previous(n)))) {
     call.setOpcode(INVOKESTATIC);call.owner=UI;call.name="nativePointFrame";call.desc="(Ljava/lang/Object;IIIIII)V";call.itf=false;changed++;
    }
    if(text>=0&&call.name.equals("getSkillPointTypeForPart")) {
     AbstractInsnNode at=next(n);
     if(at instanceof VarInsnNode&&at.getOpcode()==ASTORE){InsnList l=new InsnList();l.add(new VarInsnNode(ALOAD,0));l.add(new VarInsnNode(ALOAD,text));l.add(new MethodInsnNode(INVOKESTATIC,UI,"addDiscoveryHint","(Ljava/lang/Object;Ljava/lang/Object;)V",false));m.instructions.insert(at,l);changed++;}
    }
   }
   for(AbstractInsnNode n:m.instructions.toArray())if(n.getOpcode()==RETURN) {
    InsnList tip=new InsnList();tip.add(new VarInsnNode(ALOAD,0));tip.add(new VarInsnNode(ILOAD,1));tip.add(new VarInsnNode(ILOAD,2));tip.add(new MethodInsnNode(INVOKESTATIC,UI,"drawCounterTooltip","(Ljava/lang/Object;II)V",false));m.instructions.insertBefore(n,tip);changed++;
   }
  }
  return changed;
 }
 private static boolean is37(AbstractInsnNode n){return n instanceof IntInsnNode&&((IntInsnNode)n).operand==37||n instanceof LdcInsnNode&&Integer.valueOf(37).equals(((LdcInsnNode)n).cst);}
 private static AbstractInsnNode previous(AbstractInsnNode n){do{n=n.getPrevious();}while(n!=null&&n.getOpcode()<0);return n;}
 private static AbstractInsnNode next(AbstractInsnNode n){do{n=n.getNext();}while(n!=null&&n.getOpcode()<0);return n;}
}
