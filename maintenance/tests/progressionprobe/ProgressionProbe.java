package progressionprobe;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Integration checks against real AM2, Witchery, Botania and progression classes. */
@Mod(modid="progressionprobe",name="Temporary Advanced Silver Progression Probe",version="1",dependencies="required-after:am2pg;required-after:Botania;required-after:witchery")
public final class ProgressionProbe {
 static final String RULES="airburn.am2playground.progression.SilverProgressionRules";
 static final String PROGRESS="airburn.am2playground.progression.SilverProgression";
 static final String[] TARGETS={"Level","Nullify","CovenConnection","FloweringNature","Diabolic","Ethereal","Sacrament"};
 static Object world,player,props,data,inventory;
 static List<String> log=new ArrayList<String>();

 static Object call(Object target,String name,Object...args)throws Exception {
  for(Method m:target.getClass().getMethods())if(m.getName().equals(name)&&m.getParameterTypes().length==args.length)
   try{return m.invoke(target,args);}catch(IllegalArgumentException ignored){}
  throw new NoSuchMethodException(target.getClass().getName()+"."+name);
 }
 static Object stat(String cls,String name,Object...args)throws Exception {
  for(Method m:Class.forName(cls).getMethods())if(Modifier.isStatic(m.getModifiers())&&m.getName().equals(name)&&m.getParameterTypes().length==args.length)
   try{return m.invoke(null,args);}catch(IllegalArgumentException ignored){}
  throw new NoSuchMethodException(cls+"."+name);
 }
 static Object field(String cls,String name)throws Exception{return Class.forName(cls).getField(name).get(null);}
 static Object part(String name)throws Exception{return call(field("am2.spell.SkillManager","instance"),"getSkill",name);}
 static void set(Object target,String name,Object value)throws Exception{
  for(Class<?> cls=target.getClass();cls!=null;cls=cls.getSuperclass())try{Field f=cls.getDeclaredField(name);f.setAccessible(true);f.set(target,value);return;}catch(NoSuchFieldException ignored){}
  throw new NoSuchFieldException(name);
 }
 static Object get(Object target,String name)throws Exception{
  for(Class<?> cls=target.getClass();cls!=null;cls=cls.getSuperclass())try{Field f=cls.getDeclaredField(name);f.setAccessible(true);return f.get(target);}catch(NoSuchFieldException ignored){}
  throw new NoSuchFieldException(name);
 }
 static Object compound()throws Exception{return Class.forName("net.minecraft.nbt.NBTTagCompound").newInstance();}
 static Object stack(Object item,int count,int meta)throws Exception{return Class.forName("net.minecraft.item.ItemStack").getConstructor(Class.forName("net.minecraft.item.Item"),int.class,int.class).newInstance(item,count,meta);}
 static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 static boolean known(String key)throws Exception{return Boolean.TRUE.equals(stat(RULES,"known",player,key));}
 static String missing(String key)throws Exception{return (String)stat(RULES,"missing",player,part(key));}
 static void magic(int level)throws Exception{call(props,"setMagicLevel",level);}
 static void dimension(int dimension)throws Exception{set(player,"field_71093_bK",dimension);}
 static void clearInventory(Object inv)throws Exception{for(int i=0;i<((Number)call(inv,"func_70302_i_")).intValue();i++)call(inv,"func_70299_a",i,null);}
 static void tablet(int mana)throws Exception{
  Object s=stack(field("vazkii.botania.common.item.ModItems","manaTablet"),1,0);
  addMana(s,mana);
  call(inventory,"func_70299_a",0,s);
 }
 static void addMana(Object stack,int amount)throws Exception {Class.forName("vazkii.botania.api.mana.IManaItem").getMethod("addMana",Class.forName("net.minecraft.item.ItemStack"),int.class).invoke(call(stack,"func_77973_b"),stack,amount);}
 static void coven(int count)throws Exception{
  Object list=Class.forName("net.minecraft.nbt.NBTTagList").newInstance();
  for(int n=0;n<count;n++){Object witch=compound();call(witch,"func_74778_a","WitchName","Progression Test Witch "+n);call(list,"func_74742_a",witch);}
  Object saved=stat("com.emoniph.witchery.infusion.Infusion","getNBT",player);
  call(saved,"func_74782_a","WITCCoven",list);
 }
 static void consecration(boolean value)throws Exception{
  Object entityData=call(player,"getEntityData");Object persisted=call(entityData,"func_74775_l","PlayerPersisted");
  call(persisted,"func_74757_a","am2pg:advancedSilverConsecrated",value);
  call(entityData,"func_74782_a","PlayerPersisted",persisted);
 }
 static void learn(Object p,String key)throws Exception{stat("airburn.am2playground.utils.helpers.PGLearnHelper","learnSkill",p,part(key));}
 static void prepare(String key)throws Exception{
  call(data,"clearAllKnowledge");clearInventory(inventory);
  Object baubles=stat("baubles.api.BaublesApi","getBaubles",player);if(baubles!=null)clearInventory(baubles);
  set(props,"hasRitual",true);set(props,"guardian9",true);set(props,"guardian10",true);
  magic(99);call(props,"setMaxMana",10000000.0f);call(props,"setCurrentMana",10000000.0f);
  coven(3);tablet(200000);consecration(false);
  dimension("Diabolic".equals(key)?-1:"Ethereal".equals(key)?1:0);
  if("Sacrament".equals(key)){
   dimension(-1);learn(player,"Diabolic");dimension(1);learn(player,"Ethereal");dimension(0);consecration(true);
  }
 }
 static Object spell(String[] names,boolean split)throws Exception{
  Object s=stack(field("am2.items.ItemsCommonProxy","spell"),1,0);call(s,"func_77982_d",compound());
  List<String> components=new ArrayList<String>(),modifiers=new ArrayList<String>();
  for(String name:names){
   Object p=part(name);require(p!=null,"Unknown combo part: "+name);learn(player,name);
   if(Class.forName("am2.api.spell.component.interfaces.ISpellComponent").isInstance(p))components.add(name);else modifiers.add(name);
  }
  learn(player,"Touch");
  Object utils=field("am2.spell.SpellUtils","instance");
  if(!split)call(utils,"addSpellStageToScroll",s,"Touch",components.toArray(new String[0]),modifiers.toArray(new String[0]));
  else{
   require(names.length>=2,"Split test needs multiple parts");
   List<String> firstC=new ArrayList<String>(),firstM=new ArrayList<String>(),lastC=new ArrayList<String>(),lastM=new ArrayList<String>();
   for(int n=0;n<names.length;n++){
    boolean component=Class.forName("am2.api.spell.component.interfaces.ISpellComponent").isInstance(part(names[n]));
    (n==names.length-1?(component?lastC:lastM):(component?firstC:firstM)).add(names[n]);
   }
   call(utils,"addSpellStageToScroll",s,"Touch",firstC.toArray(new String[0]),firstM.toArray(new String[0]));
   call(utils,"addSpellStageToScroll",s,"Touch",lastC.toArray(new String[0]),lastM.toArray(new String[0]));
  }
  return s;
 }
 static Object event(Object spell,float cost)throws Exception{
  Class<?> outer=Class.forName("am2.api.events.SpellCastingEvent"),pre=Class.forName("am2.api.events.SpellCastingEvent$Pre");
  return pre.getConstructor(outer,Class.forName("net.minecraft.item.ItemStack"),Class.forName("am2.api.spell.ItemSpellBase"),Class.forName("net.minecraft.entity.EntityLivingBase"),float.class,float.class,boolean.class)
   .newInstance(outer.newInstance(),spell,call(spell,"func_77973_b"),player,cost,0.0f,false);
 }
 static int points(String color)throws Exception{return ((Number)call(data,"getSpellPoints",Enum.valueOf((Class)Class.forName("am2.api.spell.enums.SkillPointTypes"),color))).intValue();}
 static Object skillOrb(String key)throws Exception{
  Object s=stack(field("airburn.am2playground.items.PGItems","resource"),1,3),tag=compound(),list=Class.forName("net.minecraft.nbt.NBTTagList").newInstance();
  int id=((Number)call(field("am2.spell.SkillManager","instance"),"getShiftedPartID",part(key))).intValue();
  Object n=Class.forName("net.minecraft.nbt.NBTTagInt").getConstructor(int.class).newInstance(id);call(list,"func_74742_a",n);
  call(tag,"func_74782_a","am2_skills",list);call(tag,"func_74783_a","am2_points",new int[]{3,3,3});call(s,"func_77982_d",tag);return s;
 }
 static void useOrb(Object s)throws Exception{Class.forName("net.minecraft.item.Item").getMethod("func_77659_a",Class.forName("net.minecraft.item.ItemStack"),Class.forName("net.minecraft.world.World"),Class.forName("net.minecraft.entity.player.EntityPlayer")).invoke(call(s,"func_77973_b"),s,world,player);}
 static List<Object> ritual(int x,int y,int z,Object manaLink)throws Exception{
  Object stone=field("net.minecraft.init.Blocks","field_150348_b"),air=field("net.minecraft.init.Blocks","field_150350_a"),emerald=field("net.minecraft.init.Blocks","field_150475_bE");
  Object chalk=field("am2.blocks.BlocksCommonProxy","wizardChalk"),candle=field("am2.blocks.BlocksCommonProxy","candle");
  for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++){
   call(world,"func_147465_d",x+dx,y-1,z+dz,stone,0,3);call(world,"func_147465_d",x+dx,y,z+dz,air,0,3);
  }
  int[][] marks={{-1,1},{-1,-1},{1,1},{1,-1},{-2,2},{-2,-2},{2,2},{2,-2},{-3,1},{-3,0},{-3,-1},{3,1},{3,0},{3,-1},{1,-3},{0,-3},{-1,-3},{1,3},{0,3},{-1,3}};
  for(int[] mark:marks)call(world,"func_147465_d",x+mark[0],y,z+mark[1],chalk,0,3);
  for(int[] mark:new int[][]{{2,0},{-2,0},{0,2},{0,-2}})call(world,"func_147465_d",x+mark[0],y,z+mark[1],candle,0,3);
  call(world,"func_147465_d",x,y,z,emerald,0,3);
  List<Object> items=new ArrayList<Object>();Object reagents=call(manaLink,"getReagents");
  for(int n=0;n<Array.getLength(reagents);n++){
   Object item=Class.forName("net.minecraft.entity.item.EntityItem").getConstructor(Class.forName("net.minecraft.world.World"),double.class,double.class,double.class,Class.forName("net.minecraft.item.ItemStack")).newInstance(world,x+1.25,y+0.25,z+0.25,call(Array.get(reagents,n),"func_77946_l"));
   require(Boolean.TRUE.equals(call(world,"func_72838_d",item)),"Could not spawn ritual reagent");items.add(item);
  }
  Object shape=call(manaLink,"getRitualShape");require(Boolean.TRUE.equals(call(shape,"checkStructure",world,x,y,z)),"Constructed World Fusion structure is invalid");return items;
 }
 static boolean invokeRitual(Object manaLink,Object scroll,int x,int y,int z)throws Exception{
  return Boolean.TRUE.equals(call(manaLink,"applyEffectBlock",scroll,world,x,y,z,1,x+0.5,y+0.5,z+0.5,player));
 }
 static boolean allDead(List<Object> items)throws Exception{for(Object item:items)if(!Boolean.TRUE.equals(get(item,"field_70128_L")))return false;return true;}
 static boolean allAlive(List<Object> items)throws Exception{for(Object item:items)if(Boolean.TRUE.equals(get(item,"field_70128_L")))return false;return true;}

 @Mod.EventHandler public void started(FMLServerStartedEvent e){int code=0;try{
  Object server=cpw.mods.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();world=call(server,"func_71218_a",0);
  player=stat("net.minecraftforge.common.util.FakePlayerFactory","getMinecraft",world);
  props=stat("am2.playerextensions.ExtendedProperties","For",player);data=stat("am2.playerextensions.SkillData","For",player);inventory=get(player,"field_71071_by");
  stat(PROGRESS,"init");

  for(String key:TARGETS){prepare(key);set(props,"hasRitual",false);require(missing(key)!=null,"World Fusion not required: "+key);require(!Boolean.TRUE.equals(stat(PROGRESS,"beforeLearn",player,part(key))),"Direct acquisition bypassed World Fusion: "+key);require(!known(key),"Blocked ability became known: "+key);}
  require(!Boolean.TRUE.equals(stat(RULES,"isTarget",part("ManaLink"))),"Mana Link is gated by its own ritual");
  require(Boolean.TRUE.equals(stat(PROGRESS,"beforeLearn",player,part("ManaLink"))),"Mana Link no longer learnable before ritual");log.add("WORLD_FUSION_REQUIRED_ALL_SEVEN_AND_MANALINK_EXEMPT=PASS");

  for(String key:TARGETS){prepare(key);int minimum=((Number)stat(RULES,"level",key)).intValue();magic(minimum-1);require(missing(key)!=null,"Low magic level accepted: "+key);magic(minimum);require(missing(key)==null,"Exact minimum level rejected: "+key+" "+missing(key));}
  log.add("ALL_SEVEN_MAGIC_LEVEL_BOUNDARIES=PASS");

  prepare("Nullify");set(props,"guardian10",false);set(props,"guardian9",true);require(missing("Nullify")!=null,"Fire Guardian substituted for Ender Guardian");set(props,"guardian10",true);require(missing("Nullify")==null,"Ender Guardian completion rejected");
  prepare("Diabolic");set(props,"guardian9",false);require(missing("Diabolic")!=null,"Diabolic lacks Fire Guardian gate");set(props,"guardian9",true);dimension(0);require(missing("Diabolic")!=null,"Diabolic unlocked outside Nether");dimension(-1);require(missing("Diabolic")==null,"Diabolic rejected in Nether");
  prepare("Ethereal");dimension(0);require(missing("Ethereal")!=null,"Ethereal unlocked outside End");dimension(1);require(missing("Ethereal")==null,"Ethereal rejected in End");log.add("PERSONAL_GUARDIAN_AND_DIMENSION_REQUIREMENTS=PASS");

  prepare("CovenConnection");coven(2);require(((Number)stat(RULES,"covenSize",player)).intValue()==2 && missing("CovenConnection")!=null,"Two-witch coven accepted");coven(3);require(((Number)stat(RULES,"covenSize",player)).intValue()==3 && missing("CovenConnection")==null,"Three-witch coven rejected");log.add("REAL_WITCHERY_COVEN_TWO_THREE_BOUNDARY=PASS");
  prepare("FloweringNature");tablet(199999);require(((Number)stat(RULES,"botaniaMana",player)).longValue()==199999 && missing("FloweringNature")!=null,"199999 Botania mana accepted");tablet(200000);require(((Number)stat(RULES,"botaniaMana",player)).longValue()==200000 && missing("FloweringNature")==null,"200000 Botania mana rejected");
  tablet(100000);Object ring=stack(field("vazkii.botania.common.item.ModItems","manaRing"),1,0);addMana(ring,99999);Object baubles=stat("baubles.api.BaublesApi","getBaubles",player);call(baubles,"func_70299_a",1,ring);
  require(((Number)stat(RULES,"botaniaMana",player)).longValue()==199999,"Inventory/Baubles mana not summed correctly");addMana(ring,1);require(((Number)stat(RULES,"botaniaMana",player)).longValue()==200000 && missing("FloweringNature")==null,"Exact inventory/Baubles mana boundary failed");log.add("REAL_BOTANIA_MANA_BOUNDARY_AND_BAUBLES_SUM=PASS");

  prepare("Level");set(props,"hasRitual",false);Object orb=skillOrb("Level");int b=points("BLUE"),g=points("GREEN"),r=points("RED"),silver=points("SILVER");useOrb(orb);
  require(((Number)get(orb,"field_77994_a")).intValue()==1 && !known("Level"),"Blocked orb consumed or taught ability");require(points("BLUE")==b&&points("GREEN")==g&&points("RED")==r&&points("SILVER")==silver,"Blocked orb changed points");set(props,"hasRitual",true);useOrb(orb);require(((Number)get(orb,"field_77994_a")).intValue()==0 && known("Level"),"Eligible orb failed to teach/consume");require(points("BLUE")==b+3&&points("GREEN")==g+3&&points("RED")==r+3,"Eligible orb lost configured rewards");log.add("ORB_GATE_PRESERVES_ITEM_AND_POINTS_UNTIL_ELIGIBLE=PASS");

  prepare("Level");String[] levelCombo=(String[])stat(RULES,"combo","Level");Object split=spell(levelCombo,true);stat(PROGRESS,"onCast",event(split,1));require(!known("Level"),"Recipe split across stages unlocked ability");
  Object valid=spell(levelCombo,false);call(props,"setCurrentMana",0.0f);stat(PROGRESS,"onCast",event(valid,100));require(!known("Level"),"Insufficient mana unlocked ability");call(props,"setCurrentMana",10000000.0f);Object canceled=event(valid,1);call(canceled,"setCanceled",true);stat(PROGRESS,"onCast",canceled);require(!known("Level"),"Canceled cast unlocked ability");log.add("SAME_STAGE_MANA_AND_CANCELED_CAST_CHECKS=PASS");

  for(String key:TARGETS){prepare(key);Object s=spell((String[])stat(RULES,"combo",key),false);int before=points("SILVER");long mana=((Number)stat(RULES,"botaniaMana",player)).longValue();int witches=((Number)stat(RULES,"covenSize",player)).intValue();stat(PROGRESS,"onCast",event(s,1));require(known(key),"Valid combo did not discover "+key);require(points("SILVER")==before,"Combo changed free talent points: "+key);require(((Number)stat(RULES,"botaniaMana",player)).longValue()==mana,"Discovery consumed Botania mana");require(((Number)stat(RULES,"covenSize",player)).intValue()==witches,"Discovery consumed coven witches");stat(PROGRESS,"onCast",event(s,1));require(points("SILVER")==before,"Repeated combo farmed talent points");log.add("COMBO_"+key.toUpperCase()+"=PASS");}

  prepare("Level");Object liveSpell=spell((String[])stat(RULES,"combo","Level"),false);call(field("net.minecraftforge.common.MinecraftForge","EVENT_BUS"),"post",event(liveSpell,1));require(known("Level"),"Live spell event hook did not discover Level");log.add("ACTUAL_FORGE_SPELL_EVENT_DISCOVERY_HOOK=PASS");

  prepare("Sacrament");consecration(false);Object star=stack(field("net.minecraft.init.Items","field_151156_bN"),2,0),paradox=stack(field("airburn.am2playground.items.PGItems","resource"),2,0);call(inventory,"func_70299_a",10,star);call(inventory,"func_70299_a",11,paradox);magic(89);require(!Boolean.TRUE.equals(stat(PROGRESS,"consecrate",player)),"Low-level consecration accepted");require(((Number)get(star,"field_77994_a")).intValue()==2 && ((Number)get(paradox,"field_77994_a")).intValue()==2,"Failed consecration consumed materials");magic(90);require(Boolean.TRUE.equals(stat(PROGRESS,"consecrate",player)),"Eligible consecration rejected");require(((Number)get(star,"field_77994_a")).intValue()==1 && ((Number)get(paradox,"field_77994_a")).intValue()==1,"Consecration did not consume exactly one of each extra reagent");require(Boolean.TRUE.equals(stat(RULES,"consecrated",player)),"Consecration flag missing");stat(PROGRESS,"consecrate",player);require(((Number)get(star,"field_77994_a")).intValue()==1 && ((Number)get(paradox,"field_77994_a")).intValue()==1,"Repeated consecration consumed extra materials");log.add("CONSECRATION_REQUIREMENTS_CONSUMPTION_AND_IDEMPOTENCE=PASS");

  prepare("Sacrament");magic(90);consecration(false);clearInventory(inventory);set(props,"hasRitual",false);
  Object manaLink=part("ManaLink"),ritualScroll=spell(new String[]{"ManaLink"},false);
  List<Object> firstReagents=ritual(40,80,40,manaLink);require(invokeRitual(manaLink,ritualScroll,40,80,40),"Native World Fusion did not complete");
  require(Boolean.TRUE.equals(stat(RULES,"hasRitual",player)) && !Boolean.TRUE.equals(stat(RULES,"consecrated",player)),"First World Fusion must initiate without advanced consecration");require(allDead(firstReagents),"Native ritual did not consume original ground reagents");log.add("ACTUAL_FIRST_WORLD_FUSION_NATIVE_PATH=PASS");
  List<Object> finalReagents=ritual(56,80,40,manaLink);require(!invokeRitual(manaLink,ritualScroll,56,80,40),"Repeated ritual without extra materials was accepted");require(allAlive(finalReagents),"Failed advanced ritual consumed ground reagents");require(Boolean.TRUE.equals(call(call(manaLink,"getRitualShape"),"checkStructure",world,56,80,40)),"Failed advanced ritual destroyed its structure");require(!Boolean.TRUE.equals(stat(RULES,"consecrated",player)),"Failed advanced ritual marked consecration");log.add("ACTUAL_CONSECRATION_FAILURE_PRESERVES_CIRCLE_AND_GROUND_REAGENTS=PASS");
  Object finalStar=stack(field("net.minecraft.init.Items","field_151156_bN"),1,0),finalParadox=stack(field("airburn.am2playground.items.PGItems","resource"),1,0);call(inventory,"func_70299_a",10,finalStar);call(inventory,"func_70299_a",11,finalParadox);
  require(invokeRitual(manaLink,ritualScroll,56,80,40),"Eligible repeated World Fusion did not complete advanced consecration");require(Boolean.TRUE.equals(stat(RULES,"consecrated",player)) && allDead(finalReagents),"Advanced consecration did not mark completion and consume ground reagents");require(call(inventory,"func_70301_a",10)==null && call(inventory,"func_70301_a",11)==null,"Advanced ritual did not remove both consumed inventory slots");require(!Boolean.TRUE.equals(call(call(manaLink,"getRitualShape"),"checkStructure",world,56,80,40)),"Successful advanced ritual left its consumable structure intact");log.add("ACTUAL_MANA_LINK_REPEAT_CONSECRATION_EVENT_AND_CONSUMPTION=PASS");

  Object saved=compound();call(props,"saveNBTData",saved);set(props,"hasRitual",false);set(props,"guardian10",false);call(props,"loadNBTData",saved);require(Boolean.TRUE.equals(stat(RULES,"hasRitual",player)) && Boolean.TRUE.equals(get(props,"guardian10")),"Ritual/guardian completion did not survive NBT roundtrip");Object persisted=call(call(player,"getEntityData"),"func_74775_l","PlayerPersisted");require(Boolean.TRUE.equals(call(persisted,"func_74767_n","am2pg:advancedSilverConsecrated")),"Consecration not in persistent player NBT");log.add("RITUAL_GUARDIAN_AND_CONSECRATION_PERSISTENCE=PASS");
  log.add("ADVANCED_SILVER_PROGRESSION=PASS");
 }catch(Throwable error){code=1;log.add("ERROR="+error);error.printStackTrace();}
  try{Files.write(Paths.get("progression-probe-result.txt"),log);}catch(Exception error){error.printStackTrace();}
  for(String line:log)System.out.println("PROGRESSION_PROBE "+line);
  cpw.mods.fml.common.FMLCommonHandler.instance().exitJava(code,false);
 }
}
