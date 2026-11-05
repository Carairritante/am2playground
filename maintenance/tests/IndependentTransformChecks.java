import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import airburn.am2playground.preloader.IndependentCompatibilityTransform;

/** Authored synthetic fixtures: no AM2 or Playground implementation is distributed. */
public final class IndependentTransformChecks implements Opcodes {
 private static final String POINT="am2/api/spell/enums/SkillPointTypes",STATE="am2/api/spell/enums/LearnStates";
 private static final String SELF="airburn/am2playground/preloader/IndependentCompatibilityTransform";
 private static final String SLOT_RING="airburn/am2playground/containers/slots/SlotAnyRing";
 private static final String INV_RING="airburn/am2playground/containers/inventory/InventoryRecursionRing";
 private static final String CONT_RING="airburn/am2playground/containers/ContainerRecursionRing";
 private static final String ITEM_RING="airburn/am2playground/items/baubles/ItemBaublesRecursionRing";
 private static final String ITEM_HELL="airburn/am2playground/items/baubles/ItemBaublesHellRing";
 private static final String NBT_HELPER="airburn/am2playground/utils/helpers/PGNBTHelper";
 private static void need(boolean value,String why){if(!value)throw new AssertionError(why);}
 private static ClassNode fixture(String name,String parent){ClassNode c=new ClassNode();c.version=V1_8;c.access=ACC_PUBLIC;c.name=name;c.superName=parent;return c;}
 private static MethodNode method(ClassNode c,String name,String desc,AbstractInsnNode...body){MethodNode m=new MethodNode(ACC_PUBLIC,name,desc,null,null);for(AbstractInsnNode n:body)m.instructions.add(n);c.methods.add(m);return m;}
 private static MethodInsnNode invoke(int kind,String owner,String name,String desc){return new MethodInsnNode(kind,owner,name,desc,false);}
 private static MethodInsnNode invokeItf(String owner,String name,String desc){return new MethodInsnNode(INVOKEINTERFACE,owner,name,desc,true);}
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
 private static void checkRecursionRingTransforms()throws Exception {
  ClassNode slot=fixture(SLOT_RING,"java/lang/Object");
  method(slot,"func_75214_a","(Lnet/minecraft/item/ItemStack;)Z",
   new VarInsnNode(ALOAD,1),invoke(INVOKEVIRTUAL,"net/minecraft/item/ItemStack","func_77973_b","()Lnet/minecraft/item/Item;"),
   new TypeInsnNode(CHECKCAST,"baubles/api/IBauble"),new VarInsnNode(ALOAD,1),
   invoke(INVOKESTATIC,"net/minecraft/client/Minecraft","func_71410_x","()Lnet/minecraft/client/Minecraft;"),
   new FieldInsnNode(GETFIELD,"net/minecraft/client/Minecraft","field_71439_g","Lnet/minecraft/client/entity/EntityClientPlayerMP;"),
   invokeItf("baubles/api/IBauble","canEquip","(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;)Z"),
   new InsnNode(IRETURN));
  method(slot,"isSameRing","(Lnet/minecraft/item/ItemStack;)Z",
   new VarInsnNode(ALOAD,1),invoke(INVOKESTATIC,NBT_HELPER,"getSessionHash","(Lnet/minecraft/item/ItemStack;)Ljava/lang/Integer;"),
   new InsnNode(POP),new InsnNode(ICONST_0),new InsnNode(IRETURN));
  slot=transform(slot);
  need(calls(slot,"net/minecraft/client/Minecraft","func_71410_x")==0,"Server slot validation must not reference client Minecraft");
  need(calls(slot,SELF,"canEquipBaubleRing")==1,"Slot must validate ring equip server-safely");
  need(calls(slot,SELF,"blockRingSlotInsert")==1,"Slot must block nested recursion rings");

  ClassNode inv=fixture(INV_RING,"java/lang/Object");
  method(inv,"<init>","(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/item/ItemStack;)V",
   new VarInsnNode(ALOAD,0),invoke(INVOKESPECIAL,"java/lang/Object","<init>","()V"),
   new VarInsnNode(ALOAD,2),new VarInsnNode(ALOAD,1),invoke(INVOKESTATIC,NBT_HELPER,"genAndSetSessionHash","(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;)V"),
   new InsnNode(RETURN));
  method(inv,"getListFromStack","(Lnet/minecraft/item/ItemStack;)Ljava/util/List;",
   new InsnNode(ACONST_NULL),new InsnNode(ARETURN));
  method(inv,"func_70299_a","(ILnet/minecraft/item/ItemStack;)V",
   new VarInsnNode(ALOAD,2),invoke(INVOKESTATIC,NBT_HELPER,"getSessionHash","(Lnet/minecraft/item/ItemStack;)Ljava/lang/Integer;"),
   new InsnNode(POP),new InsnNode(RETURN));
  method(inv,"func_94041_b","(ILnet/minecraft/item/ItemStack;)Z",
   new InsnNode(ICONST_1),new InsnNode(IRETURN));
  inv=transform(inv);
  need(calls(inv,SELF,"genSessionHashIfGui")==1,"Tick inventory construction must not spam SessionHash");
  need(calls(inv,SELF,"filterRingList")==1,"Ring list must filter nested recursion rings");
  need(calls(inv,SELF,"blockRingSlotInsert")==1,"Inventory setSlot must block nested recursion rings");
  need(calls(inv,SELF,"isAllowedInRecursionSlot")==1,"isItemValidForSlot must reject recursion rings");

  ClassNode cont=fixture(CONT_RING,"java/lang/Object");
  method(cont,"func_75134_a","(Lnet/minecraft/entity/player/EntityPlayer;)V",
   new VarInsnNode(ALOAD,0),new FieldInsnNode(GETFIELD,CONT_RING,"rings","L"+INV_RING+";"),
   invoke(INVOKEVIRTUAL,INV_RING,"func_70305_f","()V"),new InsnNode(RETURN));
  cont=transform(cont);
  need(calls(cont,SELF,"saveContainerRing")==1&&calls(cont,SELF,"closeContainerRing")==1,"Container must save first and close session hash after duplicate check");

  ClassNode ring=fixture(ITEM_RING,"java/lang/Object");
  for(String mName:new String[]{"wornTick","equip","unequip"}) {
   method(ring,mName,"(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;)V",
    new InsnNode(ACONST_NULL),new InsnNode(ICONST_0),invoke(INVOKEVIRTUAL,INV_RING,"func_70301_a","(I)Lnet/minecraft/item/ItemStack;"),new InsnNode(POP),
    new InsnNode(ACONST_NULL),new VarInsnNode(ALOAD,1),new VarInsnNode(ALOAD,2),invokeItf("baubles/api/IBauble","onWornTick","(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;)V"),
    new InsnNode(ACONST_NULL),new VarInsnNode(ALOAD,2),new VarInsnNode(ALOAD,1),invokeItf("travellersgear/api/ITravellersGear","onTravelGearTick","(Lnet/minecraft/entity/EntityPlayer;Lnet/minecraft/item/ItemStack;)V"),
    new InsnNode(ACONST_NULL),new VarInsnNode(ALOAD,1),new VarInsnNode(ALOAD,2),invokeItf("am2/api/items/IAetherBauble","onTick","(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;)V"),
    new InsnNode(RETURN));
  }
  method(ring,"addMana","(Lnet/minecraft/item/ItemStack;I)V",
   new VarInsnNode(ALOAD,1),invoke(INVOKESTATIC,INV_RING,"getInventoryFromNBT","(Lnet/minecraft/item/ItemStack;)[Lnet/minecraft/item/ItemStack;"),
   new InsnNode(POP),new InsnNode(RETURN));
  ring=transform(ring);
  need(calls(ring,SELF,"enterRing")==3&&calls(ring,SELF,"exitRing")==3,"Recursion ring depth guards missing");
  need(calls(ring,SELF,"beginInnerRingSlot")==3,"Inner ring slot reader/persister missing");
  need(calls(ring,"am2/api/items/IAetherBauble","onEquip")==1,"equip must call IAetherBauble.onEquip");
  need(calls(ring,"baubles/api/IBauble","onUnequipped")==1,"unequip must call IBauble.onUnequipped");
  need(calls(ring,"travellersgear/api/ITravellersGear","onTravelGearUnequip")==1,"unequip must call ITravellersGear.onTravelGearUnequip");
  need(calls(ring,"am2/api/items/IAetherBauble","onUnequip")==1,"unequip must call IAetherBauble.onUnequip");
  need(calls(ring,SELF,"handleAddMana")==1,"addMana must distribute mana across all slots safely");

  ClassNode hell=fixture(ITEM_HELL,"java/lang/Object");
  method(hell,"unequip","(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;)V",
   new LdcInsnNode(""),invoke(INVOKESTATIC,"java/util/UUID","fromString","(Ljava/lang/String;)Ljava/util/UUID;"),
   new InsnNode(POP),new InsnNode(RETURN));
  hell=transform(hell);
  need(calls(hell,SELF,"safeUuidFromString")==1,"Hell ring unequip must guard UUID parsing");
  need(IndependentCompatibilityTransform.safeUuidFromString("")!=null&&IndependentCompatibilityTransform.safeUuidFromString(null)!=null,"Empty UUID string must not throw");

  File baseJar=new File("maintenance/base/AM2PlayGround-Beta-0.12.1-CR1.jar");
  if(!baseJar.isFile())baseJar=new File("base/AM2PlayGround-Beta-0.12.1-CR1.jar");
  if(baseJar.isFile()) {
   ZipFile zip=new ZipFile(baseJar);
   try {
    for(String entryName:new String[]{SLOT_RING,INV_RING,CONT_RING,ITEM_RING,ITEM_HELL}) {
     ZipEntry entry=zip.getEntry(entryName+".class");
     need(entry!=null,"Missing class in base JAR: "+entryName);
     InputStream in=zip.getInputStream(entry);
     ByteArrayOutputStream buf=new ByteArrayOutputStream();
     byte[] tmp=new byte[4096];int r;
     while((r=in.read(tmp))!=-1)buf.write(tmp,0,r);
     in.close();
     byte[] raw=buf.toByteArray();
     byte[] out=IndependentCompatibilityTransform.transform(raw,entryName.replace('/','.'));
     need(!Arrays.equals(raw,out),"Base class was not transformed: "+entryName);
     need(Arrays.equals(out,IndependentCompatibilityTransform.transform(out,entryName.replace('/','.'))),"Base class transform not idempotent: "+entryName);
     ClassNode verified=read(out);
     verify(verified);
     if(entryName.equals(SLOT_RING))need(calls(verified,"net/minecraft/client/Minecraft","func_71410_x")==0,"Real SlotAnyRing still references client Minecraft");
    }
   } finally {
    zip.close();
   }
  }
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
  MethodNode screen=(MethodNode)gui.methods.get(0);
  InsnList counter=new InsnList();counter.add(new VarInsnNode(ALOAD,0));
  counter.add(new InsnNode(ICONST_0));counter.add(new InsnNode(ICONST_0));counter.add(new IntInsnNode(SIPUSH,210));counter.add(new InsnNode(ICONST_0));counter.add(new IntInsnNode(BIPUSH,37));counter.add(new IntInsnNode(BIPUSH,37));counter.add(invoke(INVOKEVIRTUAL,gui.name,"func_73729_b","(IIIIII)V"));
  for(int i=0;i<3;i++){counter.add(new InsnNode(ACONST_NULL));counter.add(new LdcInsnNode("999"));counter.add(new InsnNode(ICONST_0));counter.add(new InsnNode(ICONST_0));counter.add(new LdcInsnNode(0xffffff));counter.add(invoke(INVOKEVIRTUAL,"net/minecraft/client/gui/FontRenderer","func_78276_b","(Ljava/lang/String;III)I"));counter.add(new InsnNode(POP));}
  screen.instructions.insert(counter);
  gui=transform(gui);
  need(calls(gui,"airburn/am2playground/progression/SilverOculusHooks","drawCounter")==1,"Silver display must be PG-owned");
  need(calls(gui,"airburn/am2playground/progression/SilverOculusHooks","addDiscoveryHint")==1,"Silver discovery hint missing");
  need(calls(gui,"airburn/am2playground/progression/SilverOculusHooks","beginDraw")==1,"Server presence must be checked per screen frame");
  need(calls(gui,"airburn/am2playground/progression/SilverOculusHooks","nativePointFrame")==1,"Native frame fallback missing");
  need(calls(gui,"airburn/am2playground/progression/SilverOculusHooks","nativePointCount")==3,"All native count draws must have a fallback");
  need(calls(gui,"airburn/am2playground/progression/SilverOculusHooks","drawCounterTooltip")==1,"Exact-value point tooltip missing");
  checkRecursionRingTransforms();
  String hooks="airburn.am2playground.progression.SilverOculusHooks";
  Class<?> panel=Class.forName(hooks);java.lang.reflect.Method compact=panel.getMethod("compact",int.class);
  for(int value:new int[]{0,999,1000,1500,999999,Integer.MAX_VALUE,Integer.MIN_VALUE})need(String.valueOf(compact.invoke(null,value)).length()<=6,"Large balance must fit: "+value);
  need("0".equals(compact.invoke(null,0))&&"1.5k".equals(compact.invoke(null,1500)),"Point formatting changed unexpectedly");
  byte[] untouched=bytes(fixture("example/Unrelated","java/lang/Object"));
  need(IndependentCompatibilityTransform.transform(untouched,"example.Unrelated")==untouched,"Unrelated classes changed");
  System.out.println("PASS: independent PG hooks, recursion ring server fixes, bytecode stack verification, native/fixed sync and idempotence.");
 }
}
