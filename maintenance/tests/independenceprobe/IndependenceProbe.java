package independenceprobe;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Local fixture only: stable menu, disposable world, real packets and Oculus. */
@Mod(modid="independenceprobe",name="Disposable AM2 Independence Probe",version="1",dependencies="required-after:arsmagica2;after:am2pg")
public final class IndependenceProbe {
 static final String PROGRESS="airburn.am2playground.progression.SilverProgression",HOOKS="airburn.am2playground.progression.SilverOculusHooks";
 static final String[] TARGETS={"Level","Nullify","CovenConnection","FloweringNature","Diabolic","Ethereal","Sacrament"};
 static final List<String> log=Collections.synchronizedList(new ArrayList<String>());
 static Object mc,world,player;static int state,ticks;static boolean finished;static volatile boolean worldRequested,serverDone;
 static boolean expect(){return Boolean.getBoolean("independence.expectProgress");}
 static Class<?> cls(String name)throws Exception{return Class.forName(name);}
 static boolean exists(String name){try{Class.forName(name,false,IndependenceProbe.class.getClassLoader());return true;}catch(ClassNotFoundException missing){return false;}}
 static Object call(Object object,String name,Object...args)throws Exception {
  for(Class<?> c=object instanceof Class?(Class<?>)object:object.getClass();c!=null;c=c.getSuperclass())for(Method m:c.getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterTypes().length==args.length)try{m.setAccessible(true);return m.invoke(object instanceof Class?null:object,args);}catch(IllegalArgumentException ignored){}
  throw new NoSuchMethodException(name);
 }
 static Object stat(String name,String method,Object...args)throws Exception{return call(cls(name),method,args);}
 static Object get(Object object,String name)throws Exception {
  for(Class<?> c=object instanceof Class?(Class<?>)object:object.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(object instanceof Class?null:object);}catch(NoSuchFieldException ignored){}
  throw new NoSuchFieldException(name);
 }
 static void set(Object object,String name,Object value)throws Exception {
  for(Class<?> c=object.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);f.set(object,value);return;}catch(NoSuchFieldException ignored){}
  throw new NoSuchFieldException(name);
 }
 static Object make(String name,Object...args)throws Exception {
  for(Constructor<?> c:cls(name).getConstructors())if(c.getParameterTypes().length==args.length)try{return c.newInstance(args);}catch(IllegalArgumentException ignored){}
  throw new NoSuchMethodException(name+" constructor");
 }
 @SuppressWarnings({"rawtypes","unchecked"}) static Object en(String name,String value)throws Exception{return Enum.valueOf((Class)cls(name),value);}
 static int num(Object value){return ((Number)value).intValue();}
 static void need(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 static void pass(String marker){log.add(marker+"=PASS");System.out.println("INDEPENDENCE_PROBE "+marker+"=PASS");}
 static Object field(String name,String member)throws Exception{return get(cls(name),member);}
 static Object manager()throws Exception{return field("am2.spell.SkillManager","instance");}
 static Object treeManager()throws Exception{return field("am2.spell.SkillTreeManager","instance");}
 static Object data(Object p)throws Exception{return stat("am2.playerextensions.SkillData","For",p);}
 static Object part(String name)throws Exception{return call(manager(),"getSkill",name);}
 static int kind(Object p)throws Exception{return cls("am2.api.spell.component.interfaces.ISpellShape").isInstance(p)?0:cls("am2.api.spell.component.interfaces.ISpellComponent").isInstance(p)?1:cls("am2.api.spell.component.interfaces.ISpellModifier").isInstance(p)?2:3;}
 static int points(Object d,String color)throws Exception{return num(call(d,"getSpellPoints",en("am2.api.spell.enums.SkillPointTypes",color)));}
 static void points(Object d,String color,int value)throws Exception{call(d,"setSpellPoints",value,en("am2.api.spell.enums.SkillPointTypes",color));}
 static boolean known(Object d,Object entry)throws Exception{return (Boolean)call(d,"isEntryKnown",entry);}
 static String learnState(Object d,Object entry,Object p)throws Exception{return String.valueOf(call(d,"getLearnState",entry,p));}
 static boolean marker(String name)throws Exception {
  if(!exists(name))return false;
  try{return num(get(cls(name),"am2pg$independentCompatibility"))==1;}catch(NoSuchFieldException missing){return false;}
 }
 static void startupChecks()throws Exception {
  boolean helper=exists(PROGRESS);need(helper==expect(),"Expected advanced Playground helper presence");
  log.add("CUSTOM_CORE_WORLD_FUSION_EVENT_PRESENT="+exists("am2.api.events.WorldFusionEvent"));
  for(String name:new String[]{"am2.playerextensions.SkillData","am2.guis.GuiSkillTrees"})need(marker(name)==expect(),"Playground owns conditional overlay "+name);
  if(exists("am2.spell.components.ManaLink"))need(marker("am2.spell.components.ManaLink")==expect(),"Playground owns ManaLink overlay");
  pass("PLAYGROUND_OVERLAYS_CONDITIONAL_ON_MOD_PRESENCE");
  if(expect()){
   need((Boolean)get(cls(PROGRESS),"initialized"),"Advanced progression initialized");
   boolean available;
   try{available=(Boolean)stat(PROGRESS,"advancedAvailable");}
   catch(NoSuchMethodException missing){available=(Boolean)stat("airburn.am2playground.progression.SilverProgressionRules","advancedAvailable");}
   String selected=System.getProperty("independence.advancedAvailable","auto");
   if(!selected.equals("auto"))need(available==Boolean.parseBoolean(selected),"Advanced capability matches core family");
   int count=((List<?>)get(cls(PROGRESS),"unlockers")).size(),registered=0;
   for(String target:TARGETS)if(part(target)!=null)registered++;
   need(count==(available?registered:0),"Only available advanced discoveries are registered");
   if(available)need(count>=5,"All five native advanced discoveries registered");
   log.add("ADVANCED_AVAILABLE="+available);log.add("ADVANCED_DISCOVERIES="+count);pass("ADVANCED_DISCOVERY_CAPABILITY_AND_REGISTRATION");
  }else pass("ORIGINAL_OR_ABSENT_PLAYGROUND_HAS_NO_MAINTENANCE_HELPER");
 }
 static void prerequisites(Object d,Object entry,Set<Object> seen)throws Exception {
  if(!seen.add(entry))return;
  for(Object prerequisite:(Object[])get(entry,"prerequisites")){
   prerequisites(d,prerequisite,seen);Object p=get(prerequisite,"registeredItem");
   call(d,new String[]{"setShapeKnown","setComponentKnown","setModifierKnown","setTalentKnown"}[kind(p)],num(call(manager(),"getShiftedPartID",p)));
  }
 }
 static void reset(Object d)throws Exception{call(d,"clearAllKnowledge");points(d,"SILVER",17);points(d,"BLUE",17);set(d,"primaryTree",en("am2.api.spell.enums.SkillTrees","None"));}
 static byte[] packet(Object p,Object entry,int category)throws Exception {
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
  out.writeByte(1);out.writeInt(num(call(p,"func_145782_y")));out.writeInt(num(call(manager(),"getShiftedPartID",get(entry,"registeredItem"))));out.writeInt(category);return bytes.toByteArray();
 }
 @SuppressWarnings("unchecked") static void policyChecks(Object fake,Object d)throws Exception {
  List<Object> entries=new ArrayList<Object>();for(String name:new String[]{"Offense","Defense","Utility","Talents"})entries.addAll((List<Object>)call(treeManager(),"getTree",en("am2.api.spell.enums.SkillTrees",name)));
  Object[] selected=new Object[4];Object talentPrerequisites=null;
  for(Object entry:entries){if(!Boolean.TRUE.equals(get(entry,"enabled")))continue;Object p=get(entry,"registeredItem");int k=kind(p);boolean silver="SILVER".equals(String.valueOf(call(treeManager(),"getSkillPointTypeForPart",p)));
   if(selected[k]==null||silver)selected[k]=entry;
   if(k==3&&silver&&((Object[])get(entry,"prerequisites")).length>0)talentPrerequisites=entry;
  }
  Map<Integer,Object> types=(Map<Integer,Object>)get(treeManager(),"skillPointTypeList");
  for(int k=0;k<3;k++){
   Object entry=selected[k];need(entry!=null,"Representative spell category "+k);Object p=get(entry,"registeredItem");int id=num(call(manager(),"getShiftedPartID",p));Object prior=types.put(id,en("am2.api.spell.enums.SkillPointTypes","SILVER"));
   try{
    reset(d);prerequisites(d,entry,Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>()));set(d,"primaryTree",en("am2.api.spell.enums.SkillTrees","None"));
    need(!((Boolean)stat(HOOKS,"canPurchase",p)),"Silver spell purchase denied category "+k);
    need(!"CAN_LEARN".equals(learnState(d,entry,fake)),"UI denies Silver category "+k);
    int before=points(d,"SILVER");call(d,"handlePacketData",packet(fake,entry,k));need(!known(d,entry)&&points(d,"SILVER")==before,"Server rejects Silver purchase without spending category "+k);
    call(d,new String[]{"setShapeKnown","setComponentKnown","setModifierKnown"}[k],id);need("LEARNED".equals(learnState(d,entry,fake)),"Existing discovery remains learned");
   }finally{if(prior==null)types.remove(id);else types.put(id,prior);}
  }
  pass("SILVER_SHAPE_COMPONENT_MODIFIER_UI_AND_REAL_SERVER_PACKET_REJECTION");
  need(talentPrerequisites!=null,"Silver talent with prerequisites available");
  reset(d);int before=points(d,"SILVER");call(d,"handlePacketData",packet(fake,talentPrerequisites,3));need(!known(d,talentPrerequisites)&&points(d,"SILVER")==before,"Talent cannot bypass prerequisites");
  prerequisites(d,talentPrerequisites,Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>()));set(d,"primaryTree",en("am2.api.spell.enums.SkillTrees","None"));
  need("CAN_LEARN".equals(learnState(d,talentPrerequisites,fake)),"Eligible talent can be learned");
  call(d,"handlePacketData",packet(fake,talentPrerequisites,0));need(!known(d,talentPrerequisites)&&points(d,"SILVER")==before,"Wrong talent category packet rejected");
  call(d,"handlePacketData",packet(fake,talentPrerequisites,3));need(known(d,talentPrerequisites)&&points(d,"SILVER")==before-1,"Eligible talent spends exactly one Silver");
  pass("SILVER_TALENT_PREREQUISITES_CATEGORY_AND_POINT_ACCOUNTING");
  Object nativeShape=call(treeManager(),"getSkillTreeEntry",part("Projectile"));reset(d);prerequisites(d,nativeShape,Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>()));
  int nativeBefore=points(d,"BLUE");call(d,"handlePacketData",packet(fake,nativeShape,0));need(known(d,nativeShape)&&points(d,"BLUE")==nativeBefore-1,"Native Blue purchase preserved");
  pass("NATIVE_COLORED_SKILL_PURCHASE_PRESERVED");
 }
 static void serverChecks()throws Exception {
  Object fake=stat("net.minecraftforge.common.util.FakePlayerFactory","getMinecraft",world);set(get(fake,"field_71075_bZ"),"field_75098_d",false);set(fake,"field_71135_a",get(player,"field_71135_a"));
  if(expect()){
   Object fakeData=data(fake);policyChecks(fake,fakeData);
   if(!((Boolean)stat(PROGRESS,"advancedAvailable"))){
    reset(fakeData);Object level=part("Level");need(level!=null,"Unavailable advanced skill remains identifiable");
    need(!((Boolean)stat(PROGRESS,"beforeLearn",fake,level)),"Mithion advanced direct learning blocked");
    need(num(stat(PROGRESS,"beforeWorldFusion",null,world,0,70,0,fake))==-1,"Mithion ritual keeps native path");
    Object item=field("airburn.am2playground.items.PGItems","resource"),orb=make("net.minecraft.item.ItemStack",item,1,3),tag=make("net.minecraft.nbt.NBTTagCompound"),list=make("net.minecraft.nbt.NBTTagList");
    call(list,"func_74742_a",make("net.minecraft.nbt.NBTTagInt",num(call(manager(),"getShiftedPartID",level))));
    call(tag,"func_74782_a","am2_skills",list);call(tag,"func_74783_a","am2_points",new int[]{3,3,3});call(orb,"func_77982_d",tag);
    int blue=points(fakeData,"BLUE"),silver=points(fakeData,"SILVER");
    need(!((Boolean)stat(PROGRESS,"allowOrbUse",orb,fake)),"Mithion advanced orb gate blocks before use");
    call(item,"func_77659_a",orb,world,fake);
    need(num(get(orb,"field_77994_a"))==1&&points(fakeData,"BLUE")==blue&&points(fakeData,"SILVER")==silver,"Mithion advanced orb preserves stack and points");
    pass("MITHION_ADVANCED_LEARNING_AND_ORB_BLOCKED_WITHOUT_CONSUMPTION");
   }
  }
  Object d=data(player);points(d,"BLUE",3);points(d,"GREEN",4);points(d,"RED",5);points(d,"SILVER",17);call(d,"forceSync");
  serverDone=true;pass("REAL_INTEGRATED_SERVER_AND_FAKE_PLAYER_CHECKS");
 }
 static void shot(String name)throws Exception{stat("net.minecraft.util.ScreenShotHelper","func_148259_a",new File("."),name,get(mc,"field_71443_c"),get(mc,"field_71440_d"),call(mc,"func_147110_a"));}
 static void finish(int code){finished=true;try{Files.write(Paths.get("independence-result.txt"),log);}catch(Exception e){e.printStackTrace();code=1;}cpw.mods.fml.common.FMLCommonHandler.instance().exitJava(code,false);}
 static void fail(Throwable error){error.printStackTrace();log.add("ERROR="+error);finish(1);}
 @Mod.EventHandler public void init(FMLInitializationEvent event){cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(this);}
 @SubscribeEvent public void server(TickEvent.ServerTickEvent event){if(finished||event.phase!=TickEvent.Phase.END||!worldRequested||serverDone)return;try{
  Object server=stat("net.minecraft.server.MinecraftServer","func_71276_C"),manager=call(server,"func_71203_ab");List<?> players=(List<?>)get(manager,"field_72404_b");if(players.isEmpty())return;player=players.get(0);world=get(player,"field_70170_p");serverChecks();
 }catch(Throwable error){fail(error);}}
 @SubscribeEvent public void client(TickEvent.ClientTickEvent event){if(finished||event.phase!=TickEvent.Phase.END)return;try{
  if(mc==null)mc=stat("net.minecraft.client.Minecraft","func_71410_x");ticks++;
  if(state==0){Object screen=get(mc,"field_71462_r");if(screen==null||!screen.getClass().getName().equals("net.minecraft.client.gui.GuiMainMenu"))return;if(ticks<60)return;
   startupChecks();shot("0-main-menu.png");pass("STABLE_MAIN_MENU");Object settings=make("net.minecraft.world.WorldSettings",42L,en("net.minecraft.world.WorldSettings$GameType","SURVIVAL"),false,false,field("net.minecraft.world.WorldType","field_77138_c"));state=1;ticks=0;worldRequested=true;call(mc,"func_71371_a","independence-disposable","Independence disposable",settings);return;
  }
  if(state==1&&serverDone&&get(mc,"field_71439_g")!=null&&ticks>30){Object cp=get(mc,"field_71439_g"),d=data(cp);if(points(d,"SILVER")!=17)return;
   need(points(d,"BLUE")==3&&points(d,"GREEN")==4&&points(d,"RED")==5,"Native colored point sync preserved");pass("ACTUAL_NETWORK_SILVER_AND_COLORED_POINT_SYNC");Object gui=make("am2.guis.GuiSkillTrees",cp);call(mc,"func_147108_a",gui);state=2;ticks=0;return;
  }
  if(state==2&&ticks>20){Object gui=get(mc,"field_71462_r");need(gui!=null&&gui.getClass().getName().equals("am2.guis.GuiSkillTrees"),"Oculus displayed");shot("1-oculus.png");pass("ACTUAL_OCULUS_RENDER_WITH_CONDITIONAL_SILVER_COUNTER");log.add("AM2_INDEPENDENCE=PASS");finish(0);}
 }catch(Throwable error){fail(error);}}
}
