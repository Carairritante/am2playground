package regenprobe;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.eventhandler.*;
import airburn.am2playground.event.events.AM2ManaRegenPlayerEvent;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

@Mod(modid="regenprobe",name="Temporary Mana Regeneration Probe",version="1",dependencies="required-after:am2pg")
public final class RegenProbe {
 String cancelStage; int duration=30;
 public void cancel(Event event)throws Exception {
  if(cancelStage!=null && event.getClass().getName().equals("airburn.am2playground.event.events.AM2ManaRegenPlayerEvent$"+cancelStage)) {
   event.getClass().getField("regenTicks").setInt(event,duration);event.setCanceled(true);
  }
 }
 @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true)
 public void material(AM2ManaRegenPlayerEvent.ArmorMaterial e)throws Exception{cancel(e);}
 @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true)
 public void skills(AM2ManaRegenPlayerEvent.SkillData e)throws Exception{cancel(e);}
 @SubscribeEvent(priority=EventPriority.LOWEST,receiveCanceled=true)
 public void infusion(AM2ManaRegenPlayerEvent.ArmorInfusion e)throws Exception{cancel(e);}
 static Object call(Object o,String name,Object...args)throws Exception {
  for(Method m:o.getClass().getMethods())if(m.getName().equals(name)&&m.getParameterTypes().length==args.length)
   try{return m.invoke(o,args);}catch(IllegalArgumentException ignored){}
  throw new NoSuchMethodException(name);
 }
 static Object stat(String cls,String name,Object...args)throws Exception {
  for(Method m:Class.forName(cls).getMethods())if(Modifier.isStatic(m.getModifiers())&&m.getName().equals(name)&&m.getParameterTypes().length==args.length)
   try{return m.invoke(null,args);}catch(IllegalArgumentException ignored){}
  throw new NoSuchMethodException(cls+"."+name);
 }
 static Object field(String cls,String name)throws Exception{return Class.forName(cls).getField(name).get(null);}
 static void set(Object target,String name,int value)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.setInt(target,value);}
 static void learn(Object player,Object skill)throws Exception{stat("airburn.am2playground.utils.helpers.PGLearnHelper","learnSkill",player,skill);}
 static FutureTask<Object> tick(Object props)throws Exception {
  call(props,"setCurrentMana",0.0f);set(props,"ticksForFullRegen",100);set(props,"ticksSinceLastRegen",9);set(props,"ticksToRegen",1);
  FutureTask<Object> task=new FutureTask<Object>(() -> call(props,"manaBurnoutTick"));
  Thread thread=new Thread(task,"Disposable mana regression probe");thread.setDaemon(true);thread.start();return task;
 }
 @Mod.EventHandler public void started(FMLServerStartedEvent event) {
  List<String> log=new ArrayList<String>();int code=0;
  try {
   Object server=cpw.mods.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
   Object world=call(server,"func_71218_a",0);
   Object player=stat("net.minecraftforge.common.util.FakePlayerFactory","getMinecraft",world);
   Object props=stat("am2.playerextensions.ExtendedProperties","For",player);
   Object data=stat("am2.playerextensions.SkillData","For",player);
   call(data,"clearAllKnowledge");call(props,"setMagicLevel",1);call(props,"setMaxMana",1000.0f);
   learn(player,call(field("am2.spell.SkillManager","instance"),"getSkill","ManaRegenIII"));
   learn(player,field("airburn.am2playground.spell.PGSpells","manaRegenArch"));
   boolean baseline="baseline".equals(System.getProperty("regen.expected"));
   try {
    tick(props).get(3,TimeUnit.SECONDS);
    if(baseline)throw new AssertionError("Baseline did not reproduce the loop");
   }catch(TimeoutException hang) {
    if(!baseline)throw new AssertionError("Mana regeneration still hangs");
    log.add("ORIGINAL_ARCHMAGE_INFINITE_LOOP_REPRODUCED=PASS");finish(log,0);return;
   }
   float mana=((Number)call(props,"getCurrentMana")).floatValue();
   if(Math.abs(mana-(1000.0f/45.0f*10.0f))>0.1f)throw new AssertionError("Archmage duration lost or stacked with tier III: "+mana);
   log.add("ARCHMAGE_RETURNS_WITH_45_PERCENT_DURATION=PASS");
   call(data,"clearAllKnowledge");
   call(field("net.minecraftforge.common.MinecraftForge","EVENT_BUS"),"register",this);
   for(String stage:new String[]{"ArmorMaterial","SkillData","ArmorInfusion"}) {
    cancelStage=stage;duration=30;tick(props).get(3,TimeUnit.SECONDS);
    mana=((Number)call(props,"getCurrentMana")).floatValue();
    if(Math.abs(mana-(1000.0f/30.0f*10.0f))>0.1f)throw new AssertionError("Canceled stage lost duration: "+stage+" "+mana);
    log.add("CANCELED_"+stage.toUpperCase()+"_FORWARD_AND_DURATION=PASS");
   }
   cancelStage="SkillData";duration=0;tick(props).get(3,TimeUnit.SECONDS);
   mana=((Number)call(props,"getCurrentMana")).floatValue();
   if(Float.isNaN(mana)||Float.isInfinite(mana)||mana<=0||mana>1000.1f)throw new AssertionError("Invalid mana from zero duration: "+mana);
   log.add("ZERO_DURATION_FINITE_AND_NO_LOOP=PASS");
   cancelStage=null;
   learn(player,field("airburn.am2playground.spell.PGSpells","atronach"));tick(props).get(3,TimeUnit.SECONDS);
   mana=((Number)call(props,"getCurrentMana")).floatValue();
   if(mana>=0.001f)throw new AssertionError("Atronach suppression changed: "+mana);
   log.add("ATRONACH_REGEN_SUPPRESSION_PRESERVED=PASS");
   log.add("MANA_REGEN_FIX=PASS");
  }catch(Throwable error){code=1;log.add("ERROR="+error);error.printStackTrace();}
  finish(log,code);
 }
 static void finish(List<String> log,int code) {
  try{Files.write(Paths.get("regen-probe-result.txt"),log);}catch(Exception e){e.printStackTrace();}
  for(String line:log)System.out.println("REGEN_PROBE "+line);
  cpw.mods.fml.common.FMLCommonHandler.instance().exitJava(code,false);
 }
}
