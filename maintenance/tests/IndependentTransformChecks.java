import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import airburn.am2playground.preloader.IndependentCompatibilityTransform;

/** Authored synthetic fixtures: no AM2 or Playground implementation is distributed. */
public final class IndependentTransformChecks implements Opcodes {
 private static final String POINT="am2/api/spell/enums/SkillPointTypes",STATE="am2/api/spell/enums/LearnStates";
 private static void need(boolean value,String why){if(!value)throw new AssertionError(why);}
 private static ClassNode fixture(String name,String parent){ClassNode c=new ClassNode();c.version=V1_8;c.access=ACC_PUBLIC;c.name=name;c.superName=parent;return c;}
 private static MethodNode method(ClassNode c,String name,String desc,AbstractInsnNode...body){MethodNode m=new MethodNode(ACC_PUBLIC,name,desc,null,null);for(AbstractInsnNode n:body)m.instructions.add(n);c.methods.add(m);return m;}
 private static MethodInsnNode invoke(int kind,String owner,String name,String desc){return new MethodInsnNode(kind,owner,name,desc,false);}
 private static byte[] bytes(ClassNode c){ClassWriter out=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(out);return out.toByteArray();}
 private static ClassNode read(byte[] bytes){ClassNode c=new ClassNode();new ClassReader(bytes).accept(c,0);return c;}
 private static int calls(ClassNode c,String owner,String name){int count=0;for(MethodNode m:(List<MethodNode>)c.methods)for(AbstractInsnNode n:m.instructions.toArray())if(n instanceof MethodInsnNode&&((MethodInsnNode)n).owner.equals(owner)&&((MethodInsnNode)n).name.equals(name))count++;return count;}
 private static void verify(ClassNode c)throws Exception{for(MethodNode m:(List<MethodNode>)c.methods)new Analyzer(new BasicVerifier()).analyze(c.name,m);}
 private static ClassNode transform(ClassNode c)throws Exception{
  byte[] first=IndependentCompatibilityTransform.transform(bytes(c),c.name.replace('/','.'));
  need(Arrays.equals(first,IndependentCompatibilityTransform.transform(first,c.name)),"Transform must be idempotent: "+c.name);
  ClassNode result=read(first);verify(result);return result;
 }
 private static ClassNode skill(boolean alreadySynced) {
  ClassNode c=fixture("am2/playerextensions/SkillData","java/lang/Object");
  MethodNode packet=method(c,"handlePacketData","([B)Z",new InsnNode(ACONST_NULL),new VarInsnNode(ASTORE,2),new VarInsnNode(ALOAD,0));
  for(int n=0;n<3;n++){packet.instructions.add(new VarInsnNode(ALOAD,2));packet.instructions.add(invoke(INVOKEVIRTUAL,"am2/network/AMDataReader","getInt","()I"));}
  packet.instructions.add(invoke(INVOKEVIRTUAL,c.name,"setSpellPoints","(III)V"));
  if(alreadySynced){packet.instructions.add(new VarInsnNode(ALOAD,0));packet.instructions.add(new VarInsnNode(ALOAD,2));packet.instructions.add(invoke(INVOKEVIRTUAL,"am2/network/AMDataReader","getInt","()I"));packet.instructions.add(new FieldInsnNode(GETSTATIC,POINT,"SILVER","L"+POINT+";"));packet.instructions.add(invoke(INVOKEVIRTUAL,c.name,"setSpellPoints","(IL"+POINT+";)V"));}
  packet.instructions.add(new InsnNode(ICONST_1));packet.instructions.add(new InsnNode(IRETURN));
  method(c,"getLearnState","(Lam2/api/SkillTreeEntry;Lnet/minecraft/entity/player/EntityPlayer;)L"+STATE+";",new FieldInsnNode(GETSTATIC,STATE,"CAN_LEARN","L"+STATE+";"),new InsnNode(ARETURN));
  return c;
 }
 public static void main(String[] args)throws Exception {
  ClassNode mana=fixture("am2/spell/components/ManaLink","java/lang/Object");
  method(mana,"applyEffectBlock","(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;IIIIDDDLnet/minecraft/entity/EntityLivingBase;)Z",new InsnNode(ICONST_0),new InsnNode(IRETURN));
  ClassNode changed=transform(mana);
  need(calls(changed,"airburn/am2playground/progression/SilverProgression","beforeWorldFusion")==1,"Mana Link must have one PG-owned hook");
  for(boolean alreadySynced:new boolean[]{false,true}){
   ClassNode skill=transform(skill(alreadySynced));
   need(calls(skill,"airburn/am2playground/progression/SilverOculusHooks","allowPacket")==1,"Server guard missing");
   need(calls(skill,"airburn/am2playground/progression/SilverOculusHooks","filterLearnState")==1,"Client state filter missing");
   int silver=0;for(MethodNode m:(List<MethodNode>)skill.methods)for(AbstractInsnNode n:m.instructions.toArray())if(n instanceof MethodInsnNode&&((MethodInsnNode)n).name.equals("setSpellPoints")&&((MethodInsnNode)n).desc.equals("(IL"+POINT+";)V"))silver++;
   need(silver==1,"Fourth point value must be read exactly once, including cores with the native fix");
  }
  ClassNode gui=fixture("am2/guis/GuiSkillTrees","net/minecraft/client/gui/GuiScreen");
  method(gui,"func_73863_a","(IIF)V",new TypeInsnNode(NEW,"java/util/ArrayList"),new InsnNode(DUP),invoke(INVOKESPECIAL,"java/util/ArrayList","<init>","()V"),new VarInsnNode(ASTORE,4),new FieldInsnNode(GETSTATIC,"am2/spell/SkillTreeManager","instance","Lam2/spell/SkillTreeManager;"),new InsnNode(ACONST_NULL),invoke(INVOKEVIRTUAL,"am2/spell/SkillTreeManager","getSkillPointTypeForPart","(Lam2/api/spell/component/interfaces/ISkillTreeEntry;)L"+POINT+";"),new VarInsnNode(ASTORE,5),new VarInsnNode(ALOAD,0),new VarInsnNode(ILOAD,1),new VarInsnNode(ILOAD,2),new VarInsnNode(FLOAD,3),invoke(INVOKESPECIAL,gui.superName,"func_73863_a","(IIF)V"),new InsnNode(RETURN));
  gui=transform(gui);
  need(calls(gui,"airburn/am2playground/progression/SilverOculusHooks","drawCounter")==1,"Silver display must be PG-owned");
  need(calls(gui,"airburn/am2playground/progression/SilverOculusHooks","addDiscoveryHint")==1,"Silver discovery hint missing");
  byte[] untouched=bytes(fixture("example/Unrelated","java/lang/Object"));
  need(IndependentCompatibilityTransform.transform(untouched,"example.Unrelated")==untouched,"Unrelated classes changed");
  System.out.println("PASS: independent PG hooks, bytecode stack verification, native/fixed sync and idempotence.");
 }
}
