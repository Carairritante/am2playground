package airburn.am2playground.progression;

import java.lang.reflect.*;
import java.util.*;
import am2.api.events.WorldFusionEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;

/** Server-authoritative learning gates; optional mod APIs are resolved only when needed. */
public final class SilverProgression {
 private static final String[] TARGETS={"Level","Nullify","CovenConnection","FloweringNature","Diabolic","Ethereal","Sacrament"};
 private static final List<Object> unlockers=new ArrayList<Object>();
 private static final Map<Object,Map<String,Long>> notices=Collections.synchronizedMap(new WeakHashMap<Object,Map<String,Long>>());
 private static boolean initialized;
 public static synchronized void init() {
  if(initialized)return;
  try {
   Object manager=field("am2.spell.SkillManager","instance");
   for(String name:TARGETS) {
    Object part=call(manager,"getSkill",name);if(part==null)continue;
    String[] recipe=SilverProgressionRules.combo(name);
    for(String required:recipe)if(call(manager,"getSkill",required)==null)throw new IllegalStateException("Unregistered discovery ingredient");
    unlockers.add(create("airburn.am2playground.utils.SpellUnlocker",part,recipe));
   }
   call(field("net.minecraftforge.common.MinecraftForge","EVENT_BUS"),"register",new SilverProgression());
   initialized=true;
  } catch(Exception error){throw new IllegalStateException("Cannot initialize advanced discoveries",error);}
 }
 public static boolean beforeLearn(Object player,Object part) {
  if(!SilverProgressionRules.isTarget(part))return true;
  if(remote(player))return false;
  if(SilverProgressionRules.known(player,SilverProgressionRules.key(part)))return true;
  String missing=SilverProgressionRules.missing(player,part);
  if(missing!=null){message(player,missing);return false;}
  return true;
 }
 /** A pending advanced skill blocks the whole orb, before points or inventory are changed. */
 public static boolean allowOrbUse(Object stack,Object player) {
  try {
   Object tag=call(stack,new String[]{"func_77978_p","getTagCompound"});if(tag==null)return true;
   Object list=call(tag,new String[]{"func_150295_c","getTagList"},"am2_skills",3);
   int size=((Number)call(list,new String[]{"func_74745_c","tagCount"})).intValue();
   Object manager=field("am2.spell.SkillManager","instance");
   for(int n=0;n<size;n++) {
    int id=Integer.parseInt(String.valueOf(call(list,new String[]{"func_150307_f","func_150307_f"},n)));
    Object part=call(manager,"getSkill",id);
    if(SilverProgressionRules.isTarget(part)) {
     if(remote(player))return false;
     if(!beforeLearn(player,part))return false;
    }
   }
   return true;
  } catch(Exception error){throw new IllegalStateException("Cannot validate skill orb",error);}
 }
 public static boolean onCast(Object event) {
  try {
   if(Boolean.TRUE.equals(call(event,"isCanceled")))return false;
   Object player=get(event,"caster");
   if(!type("net.minecraft.entity.player.EntityPlayer").isInstance(player)||remote(player))return false;
   Object props=stat("am2.playerextensions.ExtendedProperties","For",player);
   if(((Number)call(props,"getCurrentMana")).floatValue()<((Number)get(event,"manaCost")).floatValue())return false;
   init();boolean discovered=false;
   for(Object unlocker:unlockers) {
    Object part=get(unlocker,"unlock");String name=SilverProgressionRules.key(part);
    if(SilverProgressionRules.known(player,name))continue;
    if(!Boolean.TRUE.equals(call(unlocker,"willSpellUnlock",get(event,"stack"))))continue;
    if(!Boolean.TRUE.equals(call(unlocker,"unlockIsInPrimaryTree",player))){message(player,"am2pg.progress.missing.primaryTree");continue;}
    if(!beforeLearn(player,part))continue;
    if(Boolean.TRUE.equals(stat("airburn.am2playground.utils.helpers.PGLearnHelper","learnSkill",player,part))) {
     discovered=true;message(player,"am2pg.progress.discovered",call(field("am2.spell.SkillManager","instance"),"getDisplayName",part));
    }
   }
   return discovered;
  }catch(Exception error){throw new IllegalStateException("Cannot check advanced discovery",error);}
 }
 /** This helper is called only after the world ritual has also been validated by its event. */
 public static boolean consecrate(Object player) {
  if(remote(player)||!SilverProgressionRules.hasRitual(player))return false;
  try {
   if(((Number)call(stat("am2.playerextensions.ExtendedProperties","For",player),"getMagicLevel")).intValue()<90)return false;
   if(!SilverProgressionRules.known(player,"Diabolic")||!SilverProgressionRules.known(player,"Ethereal")||SilverProgressionRules.consecrated(player))return false;
   Object inventory=get(player,"field_71071_by","inventory");
   Object star=field("net.minecraft.init.Items","field_151156_bN","nether_star");
   Object resource=field("airburn.am2playground.items.PGItems","resource");
   int starSlot=find(inventory,star,0),paradoxSlot=find(inventory,resource,0);
   if(starSlot<0||paradoxSlot<0)return false;
   // Only deduct after all prerequisites and both distinct inventory slots are confirmed.
   call(inventory,new String[]{"func_70298_a","decrStackSize"},starSlot,1);
   call(inventory,new String[]{"func_70298_a","decrStackSize"},paradoxSlot,1);
   call(inventory,new String[]{"func_70296_d","markDirty"});
   Object data=call(player,"getEntityData");
   Object saved=call(data,new String[]{"func_74775_l","getCompoundTag"},"PlayerPersisted");
   call(saved,new String[]{"func_74757_a","setBoolean"},SilverProgressionRules.SACRAMENT_TAG,true);
   call(data,new String[]{"func_74782_a","setTag"},"PlayerPersisted",saved);
   message(player,"am2pg.progress.consecrated");return true;
  }catch(Exception error){throw new IllegalStateException("Cannot complete consecration",error);}
 }
 @SubscribeEvent(priority=EventPriority.HIGH)
 public void worldFusion(WorldFusionEvent event) {
  Object player=event.caster;
  if(!type("net.minecraft.entity.player.EntityPlayer").isInstance(player)||!SilverProgressionRules.hasRitual(player))return;
  event.setCanceled(true);
  try {
   if(SilverProgressionRules.consecrated(player)){message(player,"am2pg.progress.alreadyConsecrated");return;}
   String missing=consecrationMissing(player);if(missing!=null){message(player,missing);return;}
   Object shape=call(event.ritual,"getRitualShape");
   if(!Boolean.TRUE.equals(call(shape,"checkStructure",event.world,event.x,event.y,event.z))){message(player,"am2pg.progress.missing.ritualShape");return;}
   Object bounds=stat("net.minecraft.util.AxisAlignedBB",new String[]{"func_72330_a","getBoundingBox"},(double)event.x-3,event.y*1.0,(double)event.z-3,(double)event.x+4,(double)event.y+1,(double)event.z+4);
   List<?> items=(List<?>)call(event.world,new String[]{"func_72872_a","getEntitiesWithinAABB"},type("net.minecraft.entity.item.EntityItem"),bounds);
   Object[] required=(Object[])call(event.ritual,"getReagents");
   int[] counts=new int[required.length];int total=0;
   for(Object entity:items) {
    Object stack=call(entity,new String[]{"func_92059_d","getEntityItem"});int count=((Number)get(stack,"field_77994_a","stackSize")).intValue();
    if(count<=0)continue;int match=-1;
    for(int n=0;n<required.length;n++)if(same(stack,required[n])){match=n;break;}
    if(match<0){message(player,"am2pg.progress.missing.ritualReagents");return;}
    counts[match]+=count;total+=count;
   }
   for(int n=0;n<required.length;n++)if(counts[n]!=((Number)get(required[n],"field_77994_a","stackSize")).intValue()){message(player,"am2pg.progress.missing.ritualReagents");return;}
   if(total!=required.length||!consecrate(player))return;
   for(Object entity:items)call(entity,new String[]{"func_70106_y","setDead"});
   call(field("am2.RitualShapeHelper","instance"),"consumeRitualShape",event.ritual,event.world,event.x,event.y,event.z);
   event.completed=true;
  }catch(Exception error){throw new IllegalStateException("Cannot process World Fusion extension",error);}
 }
 private static String consecrationMissing(Object player)throws Exception {
  if(((Number)call(stat("am2.playerextensions.ExtendedProperties","For",player),"getMagicLevel")).intValue()<90)return "am2pg.progress.missing.level90";
  if(!SilverProgressionRules.known(player,"Diabolic"))return "am2pg.progress.missing.diabolic";
  if(!SilverProgressionRules.known(player,"Ethereal"))return "am2pg.progress.missing.ethereal";
  Object inv=get(player,"field_71071_by","inventory");
  if(find(inv,field("net.minecraft.init.Items","field_151156_bN","nether_star"),0)<0||find(inv,field("airburn.am2playground.items.PGItems","resource"),0)<0)return "am2pg.progress.missing.consecrationItems";
  return null;
 }
 private static int find(Object inventory,Object item,int meta)throws Exception {
  int size=((Number)call(inventory,new String[]{"func_70302_i_","getSizeInventory"})).intValue();
  for(int n=0;n<size;n++) {
   Object s=call(inventory,new String[]{"func_70301_a","getStackInSlot"},n);if(s==null)continue;
   if(call(s,new String[]{"func_77973_b","getItem"})==item&&((Number)call(s,new String[]{"func_77960_j","getItemDamage"})).intValue()==meta&&((Number)get(s,"field_77994_a","stackSize")).intValue()>0)return n;
  }return -1;
 }
 private static boolean same(Object a,Object b)throws Exception {
  return Boolean.TRUE.equals(stat("am2.utility.InventoryUtilities","compareItemStacks",a,b,true,false,true,true));
 }
 private static boolean remote(Object player) {
  try{return Boolean.TRUE.equals(get(get(player,"field_70170_p","worldObj"),"field_72995_K","isRemote"));}
  catch(Exception error){throw new IllegalStateException(error);}
 }
 private static void message(Object player,String key,Object...args) {
  if(type("net.minecraftforge.common.util.FakePlayer").isInstance(player))return;
  synchronized(notices) {
   Map<String,Long> recent=notices.get(player);if(recent==null){recent=new HashMap<String,Long>();notices.put(player,recent);}
   long now=System.currentTimeMillis();Long last=recent.get(key);if(last!=null&&now-last<3000)return;recent.put(key,now);
  }
  try{call(player,new String[]{"func_145747_a","addChatMessage"},create("net.minecraft.util.ChatComponentTranslation",key,args));}
  catch(Exception error){throw new IllegalStateException(error);}
 }
 private static Class<?> type(String name) {try{return Class.forName(name);}catch(ClassNotFoundException error){throw new IllegalStateException(error);}}
 private static Object field(String cls,String...names)throws Exception{return get(type(cls),null,names);}
 private static Object get(Object o,String...names)throws Exception{return get(o.getClass(),o,names);}
 private static Object get(Class<?> cls,Object o,String[] names)throws Exception {
  for(String name:names)for(Class<?> c=cls;c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException ignored){}
  throw new NoSuchFieldException(Arrays.toString(names));
 }
 private static Object stat(String cls,String name,Object...args)throws Exception{return stat(cls,new String[]{name},args);}
 private static Object stat(String cls,String[] names,Object...args)throws Exception{return invoke(type(cls),null,names,args);}
 private static Object call(Object o,String name,Object...args)throws Exception{return call(o,new String[]{name},args);}
 private static Object call(Object o,String[] names,Object...args)throws Exception{return o==null?null:invoke(o.getClass(),o,names,args);}
 private static Object invoke(Class<?> cls,Object o,String[] names,Object[] args)throws Exception {
  for(String name:names)for(Method m:cls.getMethods())if(m.getName().equals(name)&&m.getParameterTypes().length==args.length)
   try{return m.invoke(o,args);}catch(IllegalArgumentException ignored){}
  throw new NoSuchMethodException(cls.getName()+Arrays.toString(names));
 }
 private static Object create(String cls,Object...args)throws Exception {
  for(Constructor<?> c:type(cls).getConstructors())if(c.getParameterTypes().length==args.length)
   try{return c.newInstance(args);}catch(IllegalArgumentException ignored){}
  throw new NoSuchMethodException(cls+" constructor");
 }
}
