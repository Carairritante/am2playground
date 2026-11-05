package airburn.am2playground.preloader;

import java.lang.reflect.*;
import java.util.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Playground-owned overlays. This pass is never present when Playground is absent. */
public final class IndependentCompatibilityTransform implements Opcodes {
 private static final String MARKER="am2pg$independentCompatibility";
 private static final String SELF="airburn/am2playground/preloader/IndependentCompatibilityTransform";
 private static final String UI="airburn/am2playground/progression/SilverOculusHooks";
 private static final String PROGRESS="airburn/am2playground/progression/SilverProgression";
 private static final String SLOT_RING="airburn/am2playground/containers/slots/SlotAnyRing";
 private static final String INV_RING="airburn/am2playground/containers/inventory/InventoryRecursionRing";
 private static final String CONT_RING="airburn/am2playground/containers/ContainerRecursionRing";
 private static final String ITEM_RING="airburn/am2playground/items/baubles/ItemBaublesRecursionRing";
 private static final String ITEM_HELL="airburn/am2playground/items/baubles/ItemBaublesHellRing";
 private static final String NBT_HELPER="airburn/am2playground/utils/helpers/PGNBTHelper";
 private static final ThreadLocal<ArrayList<Object[]>> RING_FRAMES=new ThreadLocal<ArrayList<Object[]>>();
 private IndependentCompatibilityTransform(){}
 public static byte[] transform(byte[] bytes,String transformedName) {
  if(bytes==null||transformedName==null)return bytes;
  String name=transformedName.replace('.','/');
  if(!isTarget(name))return bytes;
  ClassNode c=new ClassNode();new ClassReader(bytes).accept(c,0);
  for(FieldNode f:(List<FieldNode>)c.fields)if(f.name.equals(MARKER))return bytes;
  int changed=apply(name,c);
  if(changed==0)throw new IllegalStateException("Unsupported AM2 compatibility layout: "+name);
  c.fields.add(new FieldNode(ACC_PRIVATE|ACC_STATIC|ACC_FINAL|ACC_SYNTHETIC,MARKER,"I",null,1));
  ClassWriter out=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(out);return out.toByteArray();
 }
 private static boolean isTarget(String name) {
  return name.equals("am2/spell/components/ManaLink")
   ||name.equals("am2/playerextensions/SkillData")
   ||name.equals("am2/guis/GuiSkillTrees")
   ||name.equals(SLOT_RING)
   ||name.equals(INV_RING)
   ||name.equals(CONT_RING)
   ||name.equals(ITEM_RING)
   ||name.equals(ITEM_HELL);
 }
 private static int apply(String name,ClassNode c) {
  if(name.endsWith("/ManaLink"))return manaLink(c);
  if(name.endsWith("/SkillData"))return skillData(c);
  if(name.endsWith("/GuiSkillTrees"))return gui(c);
  if(name.equals(SLOT_RING))return slotAnyRing(c);
  if(name.equals(INV_RING))return inventoryRecursionRing(c);
  if(name.equals(CONT_RING))return containerRecursionRing(c);
  if(name.equals(ITEM_RING))return itemRecursionRing(c);
  if(name.equals(ITEM_HELL))return itemHellRing(c);
  return 0;
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
 private static int slotAnyRing(ClassNode c) {
  int changed=0;
  for(MethodNode m:(List<MethodNode>)c.methods) {
   if((m.name.equals("func_75214_a")||m.name.equals("isItemValid"))&&m.desc.equals("(Lnet/minecraft/item/ItemStack;)Z")) {
    for(AbstractInsnNode n:m.instructions.toArray())if(n instanceof MethodInsnNode&&((MethodInsnNode)n).owner.equals("net/minecraft/client/Minecraft")) {
     AbstractInsnNode f=next(n),equip=next(f);
     if(f instanceof FieldInsnNode&&equip instanceof MethodInsnNode&&((MethodInsnNode)equip).name.equals("canEquip")) {
      InsnList rep=new InsnList();
      rep.add(new VarInsnNode(ALOAD,0));
      rep.add(new FieldInsnNode(GETFIELD,c.name,"inv","L"+INV_RING+";"));
      rep.add(new MethodInsnNode(INVOKESTATIC,SELF,"canEquipBaubleRing","(Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Ljava/lang/Object;)Z",false));
      m.instructions.insertBefore(n,rep);
      m.instructions.remove(n);m.instructions.remove(f);m.instructions.remove(equip);
      changed++;
     }
    }
   }
   if(m.name.equals("isSameRing")&&m.desc.equals("(Lnet/minecraft/item/ItemStack;)Z")) {
    for(AbstractInsnNode n:m.instructions.toArray())if(n instanceof MethodInsnNode) {
     MethodInsnNode call=(MethodInsnNode)n;
     if(call.owner.equals(NBT_HELPER)&&call.name.equals("getSessionHash")&&call.desc.equals("(Lnet/minecraft/item/ItemStack;)Ljava/lang/Integer;")) {
      call.owner=SELF;call.name="blockRingSlotInsert";changed++;
     }
    }
   }
  }
  return changed>=2?changed:0;
 }
 private static int inventoryRecursionRing(ClassNode c) {
  int changed=0;
  c.fields.add(new FieldNode(ACC_PUBLIC|ACC_SYNTHETIC,"am2pg$player","Lnet/minecraft/entity/player/EntityPlayer;",null,null));
  for(MethodNode m:(List<MethodNode>)c.methods) {
   if(m.name.equals("<init>")&&m.desc.equals("(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/item/ItemStack;)V")) {
    for(AbstractInsnNode n:m.instructions.toArray())if(n instanceof MethodInsnNode) {
     MethodInsnNode call=(MethodInsnNode)n;
     if(call.getOpcode()==INVOKESPECIAL&&call.name.equals("<init>")&&call.owner.equals("java/lang/Object")) {
      InsnList put=new InsnList();
      put.add(new VarInsnNode(ALOAD,0));put.add(new VarInsnNode(ALOAD,1));
      put.add(new FieldInsnNode(PUTFIELD,c.name,"am2pg$player","Lnet/minecraft/entity/player/EntityPlayer;"));
      m.instructions.insert(call,put);changed++;
     }
     if(call.owner.equals(NBT_HELPER)&&call.name.equals("genAndSetSessionHash")&&call.desc.equals("(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;)V")) {
      call.owner=SELF;call.name="genSessionHashIfGui";changed++;
     }
    }
   }
   if(m.name.equals("getListFromStack")&&m.desc.equals("(Lnet/minecraft/item/ItemStack;)Ljava/util/List;")) {
    for(AbstractInsnNode n:m.instructions.toArray())if(n.getOpcode()==ARETURN) {
     m.instructions.insertBefore(n,new MethodInsnNode(INVOKESTATIC,SELF,"filterRingList","(Ljava/util/List;)Ljava/util/List;",false));changed++;
    }
   }
   if((m.name.equals("func_70299_a")||m.name.equals("setInventorySlotContents"))&&m.desc.equals("(ILnet/minecraft/item/ItemStack;)V")) {
    for(AbstractInsnNode n:m.instructions.toArray())if(n instanceof MethodInsnNode) {
     MethodInsnNode call=(MethodInsnNode)n;
     if(call.owner.equals(NBT_HELPER)&&call.name.equals("getSessionHash")&&call.desc.equals("(Lnet/minecraft/item/ItemStack;)Ljava/lang/Integer;")) {
      call.owner=SELF;call.name="blockRingSlotInsert";changed++;
     }
    }
   }
   if((m.name.equals("func_94041_b")||m.name.equals("isItemValidForSlot"))&&m.desc.equals("(ILnet/minecraft/item/ItemStack;)Z")) {
    for(AbstractInsnNode n:m.instructions.toArray())if(n.getOpcode()==ICONST_1) {
     InsnList chk=new InsnList();
     chk.add(new VarInsnNode(ALOAD,2));
     chk.add(new MethodInsnNode(INVOKESTATIC,SELF,"isAllowedInRecursionSlot","(Lnet/minecraft/item/ItemStack;)Z",false));
     m.instructions.insertBefore(n,chk);m.instructions.remove(n);changed++;
    }
   }
  }
  return changed>=5?changed:0;
 }
 private static int containerRecursionRing(ClassNode c) {
  int changed=0;
  for(MethodNode m:(List<MethodNode>)c.methods) {
   if((m.name.equals("func_75134_a")||m.name.equals("onContainerClosed"))&&m.desc.equals("(Lnet/minecraft/entity/player/EntityPlayer;)V")) {
    for(AbstractInsnNode n:m.instructions.toArray())if(n instanceof MethodInsnNode) {
     MethodInsnNode call=(MethodInsnNode)n;
     if(call.owner.equals(INV_RING)&&(call.name.equals("func_70305_f")||call.name.equals("closeInventory"))&&call.desc.equals("()V")) {
      call.setOpcode(INVOKESTATIC);call.owner=SELF;call.name="saveContainerRing";call.desc="(Ljava/lang/Object;)V";call.itf=false;
      for(AbstractInsnNode r:m.instructions.toArray())if(r.getOpcode()==RETURN) {
       InsnList close=new InsnList();
       close.add(new VarInsnNode(ALOAD,0));
       close.add(new MethodInsnNode(INVOKESTATIC,SELF,"closeContainerRing","(Ljava/lang/Object;)V",false));
       m.instructions.insertBefore(r,close);
      }
      changed++;break;
     }
    }
   }
  }
  return changed;
 }
 private static int itemRecursionRing(ClassNode c) {
  int changed=0;
  for(MethodNode m:(List<MethodNode>)c.methods) {
   if((m.name.equals("wornTick")||m.name.equals("equip")||m.name.equals("unequip"))&&m.desc.equals("(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;)V")) {
    m.instructions.insert(new MethodInsnNode(INVOKESTATIC,SELF,"enterRing","()V",false));
    for(AbstractInsnNode n:m.instructions.toArray()) {
     if(n.getOpcode()==RETURN) {
      m.instructions.insertBefore(n,new MethodInsnNode(INVOKESTATIC,SELF,"exitRing","()V",false));changed++;
     } else if(n instanceof MethodInsnNode) {
      MethodInsnNode call=(MethodInsnNode)n;
      if(call.owner.equals(INV_RING)&&(call.name.equals("func_70301_a")||call.name.equals("getStackInSlot"))&&call.desc.equals("(I)Lnet/minecraft/item/ItemStack;")) {
       call.setOpcode(INVOKESTATIC);call.owner=SELF;call.name="beginInnerRingSlot";call.desc="(Ljava/lang/Object;I)Lnet/minecraft/item/ItemStack;";call.itf=false;changed++;
      } else if(m.name.equals("equip")&&call.owner.equals("am2/api/items/IAetherBauble")&&call.name.equals("onTick")) {
       call.name="onEquip";changed++;
      } else if(m.name.equals("unequip")) {
       if(call.owner.equals("baubles/api/IBauble")&&call.name.equals("onWornTick")){call.name="onUnequipped";changed++;}
       else if(call.owner.equals("travellersgear/api/ITravellersGear")&&call.name.equals("onTravelGearTick")){call.name="onTravelGearUnequip";changed++;}
       else if(call.owner.equals("am2/api/items/IAetherBauble")&&call.name.equals("onTick")){call.name="onUnequip";changed++;}
      }
     }
    }
   }
   if(m.name.equals("addMana")&&m.desc.equals("(Lnet/minecraft/item/ItemStack;I)V")) {
    for(AbstractInsnNode n:m.instructions.toArray())if(n instanceof MethodInsnNode) {
     MethodInsnNode call=(MethodInsnNode)n;
     if(call.owner.equals(INV_RING)&&call.name.equals("getInventoryFromNBT")&&call.desc.equals("(Lnet/minecraft/item/ItemStack;)[Lnet/minecraft/item/ItemStack;")) {
      InsnList args=new InsnList();args.add(new VarInsnNode(ALOAD,0));args.add(new VarInsnNode(ILOAD,2));
      m.instructions.insertBefore(call,args);
      call.owner=SELF;call.name="handleAddMana";call.desc="(Lnet/minecraft/item/ItemStack;Ljava/lang/Object;I)[Lnet/minecraft/item/ItemStack;";changed++;
     }
    }
   }
  }
  return changed>=8?changed:0;
 }
 private static int itemHellRing(ClassNode c) {
  int changed=0;
  for(MethodNode m:(List<MethodNode>)c.methods)if(m.name.equals("unequip")&&m.desc.equals("(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;)V")) {
   for(AbstractInsnNode n:m.instructions.toArray())if(n instanceof MethodInsnNode) {
    MethodInsnNode call=(MethodInsnNode)n;
    if(call.owner.equals("java/util/UUID")&&call.name.equals("fromString")&&call.desc.equals("(Ljava/lang/String;)Ljava/util/UUID;")) {
     call.owner=SELF;call.name="safeUuidFromString";changed++;
    }
   }
  }
  return changed;
 }
 public static boolean isRecursionRingStack(ItemStack stack) {
  if(stack==null)return false;
  try {
   Object item=call(stack,new String[]{"func_77973_b","getItem"});
   return item!=null&&item.getClass().getName().equals("airburn.am2playground.items.baubles.ItemBaublesRecursionRing");
  } catch(Exception ignored){return false;}
 }
 public static boolean isAllowedInRecursionSlot(ItemStack stack) {
  return stack==null||!isRecursionRingStack(stack);
 }
 public static Integer blockRingSlotInsert(ItemStack stack) {
  if(isRecursionRingStack(stack))return Integer.valueOf(1);
  try {
   return (Integer)stat("airburn.am2playground.utils.helpers.PGNBTHelper","getSessionHash",stack);
  } catch(Exception ignored){return null;}
 }
 public static boolean canEquipBaubleRing(Object bauble,ItemStack stack,Object inv) {
  if(bauble==null||stack==null||isRecursionRingStack(stack))return false;
  Object player=resolveSlotPlayer(inv);
  if(player==null)return true;
  try {
   return Boolean.TRUE.equals(call(bauble,"canEquip",stack,player));
  } catch(Exception ignored){return false;}
 }
 private static Object resolveSlotPlayer(Object inv) {
  if(inv!=null) {
   try{Object p=get(inv,"am2pg$player");if(p!=null)return p;}catch(Exception ignored){}
  }
  try {
   Class<?> mc=Class.forName("net.minecraft.client.Minecraft",false,IndependentCompatibilityTransform.class.getClassLoader());
   Object inst=invoke(mc,null,new String[]{"func_71410_x","getMinecraft"},new Object[0]);
   if(inst!=null)return get(inst,"field_71439_g","thePlayer");
  } catch(Throwable ignored){}
  return null;
 }
 public static void genSessionHashIfGui(ItemStack stack,EntityPlayer player) {
  if(!frames().isEmpty()||stack==null)return;
  try {
   stat("airburn.am2playground.utils.helpers.PGNBTHelper","genAndSetSessionHash",stack,player);
  } catch(Exception ignored){}
 }
 public static List filterRingList(List list) {
  if(list==null||frames().size()>1)return new ArrayList();
  for(int i=list.size()-1;i>=0;i--) {
   Object o=list.get(i);
   if(o instanceof ItemStack&&isRecursionRingStack((ItemStack)o))list.remove(i);
  }
  return list;
 }
 public static boolean sameSessionHash(Integer a,Integer b) {
  return a!=null&&b!=null&&a.intValue()==b.intValue();
 }
 public static void saveContainerRing(Object rings) {
  if(rings==null)return;
  try{call(rings,"saveToStack");}catch(Exception ignored){}
 }
 public static void closeContainerRing(Object container) {
  if(container==null)return;
  try {
   Object rings=get(container,"rings");
   if(rings!=null)call(rings,new String[]{"func_70305_f","closeInventory"});
   Object invPlayer=get(container,"invPlayer");
   if(invPlayer!=null) {
    int size=((Number)call(invPlayer,new String[]{"func_70302_i_","getSizeInventory"})).intValue();
    for(int i=0;i<size;i++) {
     Object s=call(invPlayer,new String[]{"func_70301_a","getStackInSlot"},Integer.valueOf(i));
     if(s instanceof ItemStack&&stat("airburn.am2playground.utils.helpers.PGNBTHelper","getSessionHash",s)!=null) {
      stat("airburn.am2playground.utils.helpers.PGNBTHelper","removeSessionHash",s);
     }
    }
   }
  } catch(Exception ignored){}
 }
 public static void enterRing() {
  ArrayList<Object[]> stack=frames();
  if(stack.size()>8)stack.clear();
  stack.add(new Object[4]);
 }
 public static ItemStack beginInnerRingSlot(Object inv,int slot) {
  ArrayList<Object[]> stack=frames();
  if(stack.isEmpty())stack.add(new Object[4]);
  Object[] top=stack.get(stack.size()-1);
  flushSlot(top);
  if(stack.size()>1||inv==null)return null;
  try {
   Object ringObj=get(inv,"inventoryRing");
   if(!(ringObj instanceof ItemStack))return null;
   ItemStack ring=(ItemStack)ringObj;
   ItemStack inner=(ItemStack)stat("airburn.am2playground.containers.inventory.InventoryRecursionRing","getItemFromNBT",ring,Integer.valueOf(slot));
   if(inner==null)return null;
   if(isRecursionRingStack(inner)) {
    Object itemsObj=get(inv,"items");
    if(itemsObj instanceof ItemStack[]) {
     ItemStack[] items=(ItemStack[])itemsObj;
     if(slot>=0&&slot<items.length){items[slot]=null;call(inv,"saveToStack");}
    }
    return null;
   }
   top[0]=inv;top[1]=Integer.valueOf(slot);top[2]=inner;top[3]=inner.func_77946_l();
   return inner;
  } catch(Exception ignored){return null;}
 }
 public static void exitRing() {
  ArrayList<Object[]> stack=frames();
  if(!stack.isEmpty()) {
   Object[] top=stack.remove(stack.size()-1);
   flushSlot(top);
  }
 }
 private static void flushSlot(Object[] frame) {
  if(frame==null)return;
  Object inv=frame[0];Integer slot=(Integer)frame[1];ItemStack cur=(ItemStack)frame[2];ItemStack before=(ItemStack)frame[3];
  frame[1]=null;frame[2]=null;frame[3]=null;
  if(inv==null||slot==null||cur==null)return;
  try {
   if(!stacksEqual(cur,before)) {
    Object itemsObj=get(inv,"items");
    if(itemsObj instanceof ItemStack[]) {
     ItemStack[] items=(ItemStack[])itemsObj;
     int idx=slot.intValue();
     if(idx>=0&&idx<items.length) {
      items[idx]=cur;
      call(inv,"saveToStack");
     }
    }
   }
  } catch(Exception ignored){}
 }
 public static ItemStack[] handleAddMana(ItemStack stack,Object self,int mana) {
  if(stack==null||mana==0||!frames().isEmpty())return new ItemStack[0];
  enterRing();
  try {
   ItemStack[] inv=(ItemStack[])stat("airburn.am2playground.containers.inventory.InventoryRecursionRing","getInventoryFromNBT",stack);
   if(inv==null)return new ItemStack[0];
   Class<?> manaType=typeOrNull("vazkii.botania.api.mana.IManaItem");
   if(manaType==null)return new ItemStack[0];
   boolean changed=false;
   for(int j=0;j<inv.length&&mana!=0;j++) {
    ItemStack inner=inv[j];
    if(inner==null||isRecursionRingStack(inner))continue;
    Object item=call(inner,new String[]{"func_77973_b","getItem"});
    if(item==null||!manaType.isInstance(item))continue;
    int cur=((Number)call(item,"getMana",inner)).intValue();
    int max=((Number)call(item,"getMaxMana",inner)).intValue();
    int delta=mana>0?Math.min(mana,Math.max(0,max-cur)):Math.max(mana,-Math.max(0,cur));
    if(delta!=0) {
     call(item,"addMana",inner,Integer.valueOf(delta));
     mana-=delta;changed=true;
    }
   }
   if(changed)stat("airburn.am2playground.containers.inventory.InventoryRecursionRing","saveInventoryToNBT",stack,inv);
  } catch(Exception ignored){}
  finally{exitRing();}
  return new ItemStack[0];
 }
 public static UUID safeUuidFromString(String value) {
  if(value!=null&&!value.trim().isEmpty()) {
   try{return UUID.fromString(value.trim());}catch(IllegalArgumentException ignored){}
  }
  return new UUID(0L,0L);
 }
 private static ArrayList<Object[]> frames() {
  ArrayList<Object[]> list=RING_FRAMES.get();
  if(list==null){list=new ArrayList<Object[]>();RING_FRAMES.set(list);}
  return list;
 }
 private static boolean stacksEqual(ItemStack a,ItemStack b) {
  if(a==b)return true;
  if(a==null||b==null)return false;
  try {
   return Boolean.TRUE.equals(stat("net.minecraft.item.ItemStack",new String[]{"func_77989_b","areItemStacksEqual"},a,b));
  } catch(Exception ignored){return false;}
 }
 private static Class<?> type(String name)throws Exception{return Class.forName(name);}
 private static Class<?> typeOrNull(String name){try{return Class.forName(name);}catch(Throwable ignored){return null;}}
 private static Object get(Object o,String...names)throws Exception {
  for(String name:names)for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException ignored){}
  throw new NoSuchFieldException(Arrays.toString(names));
 }
 private static Object stat(String cls,String name,Object...args)throws Exception{return invoke(type(cls),null,new String[]{name},args);}
 private static Object stat(String cls,String[] names,Object...args)throws Exception{return invoke(type(cls),null,names,args);}
 private static Object call(Object o,String name,Object...args)throws Exception{return call(o,new String[]{name},args);}
 private static Object call(Object o,String[] names,Object...args)throws Exception{return o==null?null:invoke(o.getClass(),o,names,args);}
 private static Object invoke(Class<?> cls,Object o,String[] names,Object[] args)throws Exception {
  for(String name:names)for(Method m:cls.getMethods())if(m.getName().equals(name)&&m.getParameterTypes().length==args.length)
   try{return m.invoke(o,args);}catch(IllegalArgumentException ignored){}
  throw new NoSuchMethodException(cls.getName()+Arrays.toString(names));
 }
 private static boolean is37(AbstractInsnNode n){return n instanceof IntInsnNode&&((IntInsnNode)n).operand==37||n instanceof LdcInsnNode&&Integer.valueOf(37).equals(((LdcInsnNode)n).cst);}
 private static AbstractInsnNode previous(AbstractInsnNode n){do{n=n.getPrevious();}while(n!=null&&n.getOpcode()<0);return n;}
 private static AbstractInsnNode next(AbstractInsnNode n){do{n=n.getNext();}while(n!=null&&n.getOpcode()<0);return n;}
}
