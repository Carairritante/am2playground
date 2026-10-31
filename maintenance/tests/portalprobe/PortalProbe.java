package portalprobe;
import cpw.mods.fml.common.Mod;import cpw.mods.fml.common.event.FMLServerStartedEvent;
import java.lang.reflect.*;import java.nio.file.*;import java.util.*;
@Mod(modid="portalprobe",name="Temporary Ritual Portal Probe",version="1",dependencies="required-after:am2pg;required-after:Thaumcraft;required-after:AWWayofTime")
public class PortalProbe {
 static Object observedEntity,observedDrops;
 @cpw.mods.fml.common.eventhandler.SubscribeEvent(priority=cpw.mods.fml.common.eventhandler.EventPriority.LOWEST)
 public void observe(net.minecraftforge.event.entity.living.LivingDropsEvent e)throws Exception {if(e.getClass().getField("entityLiving").get(e)==observedEntity)observedDrops=e;}
 static Object staticCall(String c,String n,Object...a)throws Exception{for(Method m:Class.forName(c).getMethods())if(Modifier.isStatic(m.getModifiers())&&m.getName().equals(n)&&m.getParameterTypes().length==a.length)try{return m.invoke(null,a);}catch(IllegalArgumentException ex){}throw new NoSuchMethodException(c+"."+n);}
 static Object call(Object o,String n,Object...a)throws Exception {for(Method m:o.getClass().getMethods())if(m.getName().equals(n)&&m.getParameterTypes().length==a.length)try{return m.invoke(o,a);}catch(IllegalArgumentException ex){}throw new NoSuchMethodException(n);}
 static Object field(String c,String f)throws Exception{return Class.forName(c).getField(f).get(null);}
 static Object stack(Object item,int meta)throws Exception{return Class.forName("net.minecraft.item.ItemStack").getConstructor(Class.forName("net.minecraft.item.Item"),int.class,int.class).newInstance(item,1,meta);}
 static Object entityItem(Object world,Object item)throws Exception{return Class.forName("net.minecraft.entity.item.EntityItem").getConstructor(Class.forName("net.minecraft.world.World"),double.class,double.class,double.class,Class.forName("net.minecraft.item.ItemStack")).newInstance(world,0.,70.,0.,item);}
 static Object event(Object portal,Object world,boolean hit,boolean cancel)throws Exception {
  ArrayList<Object> drops=new ArrayList<Object>();drops.add(entityItem(world,stack(field("thaumcraft.common.config.ConfigItems","itemEldritchObject"),3)));
  Class<?> ec=Class.forName("net.minecraftforge.event.entity.living.LivingDropsEvent");
  Object ev=ec.getConstructor(Class.forName("net.minecraft.entity.EntityLivingBase"),Class.forName("net.minecraft.util.DamageSource"),ArrayList.class,int.class,boolean.class,int.class).newInstance(portal,field("net.minecraft.util.DamageSource","field_76377_j"),drops,0,hit,0);
  if(cancel)call(ev,"setCanceled",true);
  call(field("net.minecraftforge.common.MinecraftForge","EVENT_BUS"),"post",ev);
  return ev;
 }
 static int orbs(Object e)throws Exception{int n=0;for(Object drop:(List<?>)e.getClass().getField("drops").get(e)){Object s=call(drop,"func_92059_d");if(call(s,"func_77973_b")==field("airburn.am2playground.items.PGItems","resource")&&call(field("airburn.am2playground.items.PGItems","whiteOrbBloodBoss"),"func_77960_j").equals(call(s,"func_77960_j"))){Object tag=call(s,"func_77978_p");if(tag!=null&&Arrays.equals((int[])call(tag,"func_74759_k","am2_points"),new int[]{5,5,5}))n++;}}return n;}
 @Mod.EventHandler public void started(FMLServerStartedEvent e){List<String> log=new ArrayList<String>();int code=0;try{
  Object server=cpw.mods.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
  Object world=call(server,"func_71218_a",0);
  Class<?> wc=Class.forName("net.minecraft.world.World"),pc=Class.forName("thaumcraft.common.entities.monster.boss.EntityCultistPortal");
  Object natural=pc.getConstructor(wc).newInstance(world);
  if(orbs(event(natural,world,true,false))!=0)throw new AssertionError("Natural portal got orb");log.add("NATURAL_PORTAL_UNCHANGED=PASS");
  Method factory=Class.forName("airburn.am2playground.compat.thaumcraft.PGThaumcraftCompat").getMethod("getCrimsonPortal",wc);
  Object ritual=factory.invoke(null,world),data=call(ritual,"getEntityData");
  boolean marked=Boolean.TRUE.equals(call(data,"func_74767_n","am2pg:eldritchMobilization"));log.add("RITUAL_FACTORY_MARKED="+marked);
  Object dropEvent=event(ritual,world,true,false);int count=orbs(dropEvent);log.add("RITUAL_ORB_COUNT="+count);
  if(!marked){if(count!=0)throw new AssertionError("Unexpected baseline reward");log.add("ORIGINAL_MISSING_ORB_REPRODUCED=PASS");}
  else{
   if(count!=1)throw new AssertionError("Expected exactly one orb");
   int size=((List<?>)dropEvent.getClass().getField("drops").get(dropEvent)).size();if(size<2)throw new AssertionError("Existing pearl lost");
   if(orbs(event(ritual,world,true,false))!=0)throw new AssertionError("Duplicate orb");log.add("DUPLICATE_EVENT_PROTECTION=PASS");
   if(orbs(event(factory.invoke(null,world),world,false,false))!=0)throw new AssertionError("Non-player death rewarded");log.add("NON_PLAYER_DEATH_NO_ORB=PASS");
   if(orbs(event(factory.invoke(null,world),world,true,true))!=0)throw new AssertionError("Canceled drops rewarded");log.add("CANCELED_DROPS_NO_ORB=PASS");
   log.add("EXISTING_DROPS_PRESERVED=PASS");
   Object player=staticCall("net.minecraftforge.common.util.FakePlayerFactory","getMinecraft",world);
   Object real=factory.invoke(null,world);call(real,"func_70107_b",0.,80.,0.);
   observedEntity=real;observedDrops=null;call(field("net.minecraftforge.common.MinecraftForge","EVENT_BUS"),"register",this);
   Object damage=staticCall("net.minecraft.util.DamageSource","func_76365_a",player);
   if(!Boolean.TRUE.equals(call(real,"func_70097_a",damage,100000.0f)))throw new AssertionError("Real damage rejected");
   if(observedDrops==null||orbs(observedDrops)!=1)throw new AssertionError("Real portal death did not drop orb");
   log.add("ACTUAL_PLAYER_KILL_ORB=PASS");
   Object props=staticCall("am2.playerextensions.ExtendedProperties","For",player);call(props,"setMagicLevel",1);
   Object dataPlayer=staticCall("am2.playerextensions.SkillData","For",player);
   Object silver=Enum.valueOf((Class)Class.forName("am2.api.spell.enums.SkillPointTypes"),"SILVER");
   int beforeSilver=((Number)call(dataPlayer,"getSpellPoints",silver)).intValue();
   Object useStack=call(field("airburn.am2playground.items.PGItems","whiteOrbBloodBoss"),"func_77946_l");
   Class.forName("net.minecraft.item.Item").getMethod("func_77659_a",Class.forName("net.minecraft.item.ItemStack"),wc,Class.forName("net.minecraft.entity.player.EntityPlayer")).invoke(call(useStack,"func_77973_b"),useStack,world,player);
   int afterSilver=((Number)call(dataPlayer,"getSpellPoints",silver)).intValue();
   log.add("ACTUAL_ORB_USE_SILVER_BEFORE="+beforeSilver+"; AFTER="+afterSilver);
   if(beforeSilver!=afterSilver)throw new AssertionError("Unexpected free silver points");
   for(String skill:new String[]{"bloodDevastation","bloodVortex"})if(!Boolean.TRUE.equals(staticCall("airburn.am2playground.utils.helpers.PGLearnHelper","isSkillKnown",player,field("airburn.am2playground.spell.PGSpells",skill))))throw new AssertionError("Skill was not learned: "+skill);
   log.add("ACTUAL_ORB_USE_BLOOD_SKILLS=PASS");
  }
  Object template=field("airburn.am2playground.items.PGItems","whiteOrbBloodBoss"),tag=call(template,"func_77978_p");
  if(!Arrays.equals((int[])call(tag,"func_74759_k","am2_points"),new int[]{5,5,5}))throw new AssertionError("Wrong orb points");
  Object skills=call(tag,"func_150295_c","am2_skills",3);if(!Integer.valueOf(2).equals(call(skills,"func_74745_c")))throw new AssertionError("Missing blood skills");log.add("ORB_POINTS_BLUE_GREEN_RED=5,5,5; SILVER=0");log.add("BLOOD_SKILL_COUNT=2");
  log.add(marked?"PORTAL_PATCH=PASS":"PORTAL_BASELINE=PASS");
 }catch(Throwable ex){code=1;log.add("ERROR="+ex);ex.printStackTrace();}
 try{Files.write(Paths.get("portal-probe-result.txt"),log);}catch(Exception ex){ex.printStackTrace();}for(String line:log)System.out.println("PORTAL_PROBE "+line);
 cpw.mods.fml.common.FMLCommonHandler.instance().exitJava(code,false);
 }
}
