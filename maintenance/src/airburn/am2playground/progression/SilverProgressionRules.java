package airburn.am2playground.progression;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Locale;

/** Read-only requirements for the seven advanced discoveries. Optional mods are queried lazily. */
public final class SilverProgressionRules {
 public static final String SACRAMENT_TAG="am2pg:advancedSilverConsecrated";
 public static final long MIN_BOTANIA_MANA=200000L;
 public static final int MIN_COVEN_SIZE=3;
 private static final String PREFIX="am2pg.progress.missing.";
 private static final String[] PARTS={"Level","Nullify","CovenConnection","FloweringNature","Diabolic","Ethereal","Sacrament"};
 private static final String[][] COMBOS={
  {"ManaDrain","LifeTap","BuffPower"},
  {"Dispel","Silence","Reflect"},
  {"Charm","Summon","Duration"},
  {"Grow","Heal","Regeneration"},
  {"Ignition","Wither","Curse"},
  {"AstralDistortion","Invisibility","Lunar"},
  {"Bless","Heal","Healing","BuffPower","Duration"}
 };
 private static final int[] LEVELS={45,50,50,50,65,70,90};
 private SilverProgressionRules() {}

 public static boolean isTarget(Object part) {return index(key(part))>=0;}

 /** Return a registry name, normalizing the seven supported names when supplied directly. */
 public static String key(Object part) {
  if(part==null)return null;
  String name=part instanceof String?(String)part:null;
  if(name==null) {
   Object manager=staticField("am2.spell.SkillManager","instance");
   Object registered=invoke(manager,"getSkillName",part);
   if(registered instanceof String)name=(String)registered;
  }
  if(name==null) {
   Object spellName=invoke(part,"getSpellName");
   if(spellName instanceof String)name=(String)spellName;
  }
  if(name==null)name=part.getClass().getSimpleName();
  int n=index(name);return n>=0?PARTS[n]:name;
 }

 public static String[] combo(String part) {
  int n=index(part);return n<0?null:COMBOS[n].clone();
 }

 public static int level(String part) {int n=index(part);return n<0?0:LEVELS[n];}

 /** A translation key for the first unmet prerequisite, or null when learning may proceed. */
 public static String missing(Object player,Object part) {
  String name=key(part);int n=index(name);
  if(n<0)return null;
  if(!SilverProgression.advancedAvailable())return PREFIX+"requiresAM25";
  Object props=properties(player);
  if(props==null)return PREFIX+"properties";
  if(!Boolean.TRUE.equals(field(props,"hasRitual")))return PREFIX+"worldFusion";
  int required=LEVELS[n];
  if(number(invoke(props,"getMagicLevel"),0).intValue()<required)return PREFIX+"level"+required;
  if("Nullify".equals(name)&&!Boolean.TRUE.equals(field(props,"guardian10")))return PREFIX+"enderGuardian";
  if("CovenConnection".equals(name)) {
   if(type("com.emoniph.witchery.entity.EntityCovenWitch")==null)return PREFIX+"witchery";
   if(covenSize(player)<MIN_COVEN_SIZE)return PREFIX+"coven";
  }
  if("FloweringNature".equals(name)) {
   if(type("vazkii.botania.api.mana.IManaItem")==null)return PREFIX+"botania";
   if(botaniaMana(player)<MIN_BOTANIA_MANA)return PREFIX+"botaniaMana";
  }
  if("Diabolic".equals(name)) {
   if(!Boolean.TRUE.equals(field(props,"guardian9")))return PREFIX+"fireGuardian";
   if(dimension(player)!=-1)return PREFIX+"nether";
  }
  if("Ethereal".equals(name)&&dimension(player)!=1)return PREFIX+"end";
  if("Sacrament".equals(name)) {
   if(!known(player,"Diabolic"))return PREFIX+"diabolic";
   if(!known(player,"Ethereal"))return PREFIX+"ethereal";
   if(!consecrated(player))return PREFIX+"sacramentRitual";
  }
  return null;
 }

 public static boolean known(Object player,String skill) {
  int n=index(skill);if(n>=0)skill=PARTS[n];
  Object manager=staticField("am2.spell.SkillManager","instance");
  Object part=invoke(manager,"getSkill",skill);
  if(part==null)return false;
  return Boolean.TRUE.equals(invokeStatic("airburn.am2playground.utils.helpers.PGLearnHelper","isSkillKnown",player,part));
 }

 public static boolean hasRitual(Object player) {
  return Boolean.TRUE.equals(field(properties(player),"hasRitual"));
 }

 public static boolean consecrated(Object player) {
  Object data=invoke(player,"getEntityData");
  Object saved=invoke(data,new String[]{"func_74775_l","getCompoundTag"},"PlayerPersisted");
  return Boolean.TRUE.equals(invoke(saved,new String[]{"func_74767_n","getBoolean"},SACRAMENT_TAG));
 }

 public static int dimension(Object player) {
  Object dimension=field(player,"field_71093_bK","dimension");
  if(dimension instanceof Number)return ((Number)dimension).intValue();
  Object world=field(player,"field_70170_p","worldObj");
  Object provider=field(world,"field_73011_w","provider");
  return number(field(provider,"field_76574_g","dimensionId"),Integer.MIN_VALUE).intValue();
 }

 /** Count only the player's recorded Witchery coven, not nearby NPCs. */
 public static int covenSize(Object player) {
  return Math.max(0,number(invokeStatic("com.emoniph.witchery.entity.EntityCovenWitch","getCovenSize",player),0).intValue());
 }

 /** Sum actual IManaItem contents without extracting mana, including Baubles and Botania boxes. */
 public static long botaniaMana(Object player) {
  Class<?> manaItem=type("vazkii.botania.api.mana.IManaItem");
  if(player==null||manaItem==null)return 0L;
  IdentityHashMap<Object,Boolean> seen=new IdentityHashMap<Object,Boolean>();
  long mana=inventoryMana(field(player,"field_71071_by","inventory"),manaItem,seen);
  Object baubles=invokeStatic("baubles.api.BaublesApi","getBaubles",player);
  return add(mana,inventoryMana(baubles,manaItem,seen));
 }

 private static long inventoryMana(Object inventory,Class<?> manaItem,IdentityHashMap<Object,Boolean> seen) {
  if(inventory==null)return 0L;
  int size=number(invoke(inventory,new String[]{"func_70302_i_","getSizeInventory"}),0).intValue();
  long mana=0L;
  // InventoryPlayer/Baubles are small. Bound malformed third-party inventory implementations.
  for(int slot=0;slot<Math.min(Math.max(0,size),4096);slot++)
   mana=add(mana,stackMana(invoke(inventory,new String[]{"func_70301_a","getStackInSlot"},slot),manaItem,seen,0));
  return mana;
 }

 private static long stackMana(Object stack,Class<?> manaItem,IdentityHashMap<Object,Boolean> seen,int depth) {
  if(stack==null||depth>8||seen.put(stack,Boolean.TRUE)!=null)return 0L;
  Object count=field(stack,"field_77994_a","stackSize");
  if(count instanceof Number&&((Number)count).intValue()<=0)return 0L;
  Object item=invoke(stack,new String[]{"func_77973_b","getItem"});
  long mana=0L;
  if(item!=null&&manaItem.isInstance(item))
   mana=Math.max(0L,number(invoke(manaItem,item,new String[]{"getMana"},new Object[]{stack}),0L).longValue());
  Class<?> box=type("vazkii.botania.common.item.ItemBaubleBox");
  if(box!=null&&item!=null&&box.isInstance(item)) {
   Object contents=null;
   try {
    Class<?> stackClass=type("net.minecraft.item.ItemStack");
    contents=java.lang.invoke.MethodHandles.publicLookup().findStatic(box,"loadStacks",
     java.lang.invoke.MethodType.methodType(Array.newInstance(stackClass,0).getClass(),stackClass)).invokeWithArguments(stack);
   }catch(Throwable unavailable){contents=null;}
   if(contents!=null&&contents.getClass().isArray())
    for(int slot=0;slot<Array.getLength(contents);slot++)
     mana=add(mana,stackMana(Array.get(contents,slot),manaItem,seen,depth+1));
  }
  return mana;
 }

 private static long add(long a,long b) {return b>Long.MAX_VALUE-a?Long.MAX_VALUE:a+b;}

 private static Object properties(Object player) {
  return invokeStatic("am2.playerextensions.ExtendedProperties","For",player);
 }

 private static int index(String name) {
  if(name==null)return -1;
  String clean=name.replace(" ","").replace("_","").toLowerCase(Locale.ROOT);
  for(int n=0;n<PARTS.length;n++)if(PARTS[n].toLowerCase(Locale.ROOT).equals(clean))return n;
  return -1;
 }

 private static Class<?> type(String name) {
  try{return Class.forName(name,false,SilverProgressionRules.class.getClassLoader());}
  catch(ClassNotFoundException|LinkageError|RuntimeException ignored){return null;}
 }

 private static Object staticField(String className,String name) {
  Class<?> cls=type(className);if(cls==null)return null;
  try{return cls.getField(name).get(null);}
  catch(ReflectiveOperationException|LinkageError|RuntimeException ignored){return null;}
 }

 private static Object field(Object target,String...names) {
  if(target==null)return null;
  for(String name:names)for(Class<?> cls=target.getClass();cls!=null;cls=cls.getSuperclass())
   try{Field field=cls.getDeclaredField(name);field.setAccessible(true);return field.get(target);}
   catch(ReflectiveOperationException|RuntimeException ignored){}
  return null;
 }

 private static Object invokeStatic(String className,String name,Object...args) {
  return invokeStatic(type(className),name,args);
 }

 private static Object invokeStatic(Class<?> cls,String name,Object...args) {
  return invoke(cls,null,new String[]{name},args);
 }

 private static Object invoke(Object target,String name,Object...args) {
  return invoke(target,new String[]{name},args);
 }

 private static Object invoke(Object target,String[] names,Object...args) {
  return target==null?null:invoke(target.getClass(),target,names,args);
 }

 private static Object invoke(Class<?> cls,Object target,String[] names,Object[] args) {
  if(cls==null)return null;
  Method[] methods;
  try{methods=cls.getMethods();}catch(LinkageError|RuntimeException unavailable){return null;}
  for(String name:names)for(Method method:methods)
   if(method.getName().equals(name)&&(target!=null||Modifier.isStatic(method.getModifiers()))&&matches(method.getParameterTypes(),args))
    try{return method.invoke(target,args);}
    catch(ReflectiveOperationException|LinkageError|RuntimeException ignored){return null;}
  return null;
 }

 private static boolean matches(Class<?>[] parameters,Object[] args) {
  if(parameters.length!=args.length)return false;
  for(int n=0;n<parameters.length;n++) {
   if(args[n]==null){if(parameters[n].isPrimitive())return false;continue;}
   Class<?> cls=parameters[n];
   if(cls==int.class)cls=Integer.class;
   else if(cls==long.class)cls=Long.class;
   else if(cls==float.class)cls=Float.class;
   else if(cls==double.class)cls=Double.class;
   else if(cls==boolean.class)cls=Boolean.class;
   else if(cls==short.class)cls=Short.class;
   else if(cls==byte.class)cls=Byte.class;
   else if(cls==char.class)cls=Character.class;
   if(!cls.isInstance(args[n]))return false;
  }
  return true;
 }

 private static Number number(Object value,Number fallback) {return value instanceof Number?(Number)value:fallback;}
}
