package airburn.am2playground.progression;

import java.lang.reflect.*;
import java.util.*;

/** Silver purchases and the extra Oculus display belong exclusively to Playground. */
public final class SilverOculusHooks {
 private SilverOculusHooks(){}
 public static boolean canPurchase(Object part) {
  if(part==null)return false;
  try {
   Object kind=call(field("am2.spell.SkillTreeManager","instance"),"getSkillPointTypeForPart",part);
   if(!"SILVER".equals(String.valueOf(kind)))return true;
   return !type("am2.api.spell.component.interfaces.ISpellShape").isInstance(part)
       &&!type("am2.api.spell.component.interfaces.ISpellComponent").isInstance(part)
       &&!type("am2.api.spell.component.interfaces.ISpellModifier").isInstance(part);
  }catch(Exception error){throw new IllegalStateException("Cannot validate Playground Silver purchase",error);}
 }
 public static Object filterLearnState(Object state,Object entry) {
  if(!"CAN_LEARN".equals(String.valueOf(state)))return state;
  try {
   if(canPurchase(get(entry,"registeredItem")))return state;
   return enumValue("am2.api.spell.enums.LearnStates","CANNOT_LEARN");
  }catch(Exception error){throw new IllegalStateException("Cannot filter Playground Oculus purchase",error);}
 }
 /** Packet rejection happens before the native handler changes points or skills. */
 public static boolean allowPacket(Object skillData,byte[] data) {
  try {
   Object entity=get(skillData,"entity");
   if(entity==null||Boolean.TRUE.equals(get(get(entity,"field_70170_p","worldObj"),"field_72995_K","isRemote")))return true;
   Object reader=create("am2.network.AMDataReader",data,false);
   int sub=((Number)call(reader,"getByte")).intValue();
   if(sub!=1)return true; // Native SPELL_LEARNED packet ID, not a new protocol.
   call(reader,"getInt");int id=((Number)call(reader,"getInt")).intValue();int category=((Number)call(reader,"getInt")).intValue();
   Object part=call(field("am2.spell.SkillManager","instance"),"getSkill",id);
   if(canPurchase(part)) {
    Object tree=field("am2.spell.SkillTreeManager","instance");
    if(!"SILVER".equals(String.valueOf(call(tree,"getSkillPointTypeForPart",part))))return true;
    Object entry=call(tree,"getSkillTreeEntry",part);
    if(category==3&&entry!=null&&"CAN_LEARN".equals(String.valueOf(call(skillData,"getLearnState",entry,entity))))return true;
   }
   call(skillData,"setFullSync");return false;
  }catch(Exception error){throw new IllegalStateException("Cannot validate Playground Oculus packet",error);}
 }
 public static void drawCounter(Object gui) {
  try {
   Object mc=stat("net.minecraft.client.Minecraft",new String[]{"func_71410_x","getMinecraft"});
   Object font=get(mc,"field_71466_p","fontRenderer"),player=get(mc,"field_71439_g","thePlayer");
   Object skill=stat("am2.playerextensions.SkillData",new String[]{"For"},player);
   String count=String.valueOf(call(skill,"getSpellPoints",enumValue("am2.api.spell.enums.SkillPointTypes","SILVER")));
   int size=((Number)get(gui,"xSize")).intValue(),height=((Number)get(gui,"ySize")).intValue();
   int left=(((Number)get(gui,"field_146294_l","width")).intValue()-size)/2;
   int top=(((Number)get(gui,"field_146295_m","height")).intValue()-height)/2;
   int width=((Number)call(font,new String[]{"func_78256_a","getStringWidth"},count)).intValue();
   call(font,new String[]{"func_78276_b","drawString"},count,left+size-18-width/2,top+25,0xBBBBBB);
  }catch(Exception error){throw new IllegalStateException("Cannot display Playground Silver points",error);}
 }
 @SuppressWarnings("unchecked") public static void addDiscoveryHint(Object gui,Object lines) {
  if(!(lines instanceof List))return;
  try {
   Object entry=get(gui,"hoveredItem");if(entry==null)return;
   Object part=get(entry,"registeredItem");if(canPurchase(part))return;
   Object mc=stat("net.minecraft.client.Minecraft",new String[]{"func_71410_x","getMinecraft"});
   Object player=get(mc,"field_71439_g","thePlayer"),skill=stat("am2.playerextensions.SkillData",new String[]{"For"},player);
   if("LEARNED".equals(String.valueOf(call(skill,"getLearnState",entry,player))))return;
   String hint=String.valueOf(stat("net.minecraft.util.StatCollector",new String[]{"func_74838_a","translateToLocal"},"am2pg.gui.silverDiscoveryOnly"));
   if(!((List)lines).contains(hint))((List)lines).add(hint);
  }catch(Exception error){throw new IllegalStateException("Cannot display Playground discovery hint",error);}
 }
 @SuppressWarnings({"rawtypes","unchecked"}) private static Object enumValue(String name,String value)throws Exception{return Enum.valueOf((Class)type(name),value);}
 private static Class<?> type(String name)throws Exception{return Class.forName(name);}
 private static Object field(String name,String...fields)throws Exception{return get(type(name),null,fields);}
 private static Object get(Object object,String...names)throws Exception{return get(object.getClass(),object,names);}
 private static Object get(Class<?> cls,Object object,String[] names)throws Exception{
  for(String name:names)for(Class<?> at=cls;at!=null;at=at.getSuperclass())try{Field f=at.getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(NoSuchFieldException ignored){}
  throw new NoSuchFieldException(Arrays.toString(names));
 }
 private static Object call(Object object,String name,Object...args)throws Exception{return call(object,new String[]{name},args);}
 private static Object call(Object object,String[] names,Object...args)throws Exception{return invoke(object.getClass(),object,names,args);}
 private static Object stat(String cls,String[] names,Object...args)throws Exception{return invoke(type(cls),null,names,args);}
 private static Object invoke(Class<?> cls,Object object,String[] names,Object[] args)throws Exception{
  for(String name:names)for(Class<?> at=cls;at!=null;at=at.getSuperclass())for(Method m:at.getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterTypes().length==args.length)try{m.setAccessible(true);return m.invoke(object,args);}catch(IllegalArgumentException ignored){}
  throw new NoSuchMethodException(cls.getName()+Arrays.toString(names));
 }
 private static Object create(String cls,Object...args)throws Exception{
  for(Constructor<?> ctor:type(cls).getConstructors())if(ctor.getParameterTypes().length==args.length)try{return ctor.newInstance(args);}catch(IllegalArgumentException ignored){}
  throw new NoSuchMethodException(cls+" constructor");
 }
}
