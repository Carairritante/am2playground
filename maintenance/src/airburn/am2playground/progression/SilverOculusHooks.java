package airburn.am2playground.progression;

import java.lang.reflect.*;
import java.util.*;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import org.lwjgl.opengl.GL11;

/** Silver purchases and the extra Oculus display belong exclusively to Playground. */
public final class SilverOculusHooks {
 private SilverOculusHooks(){}
 private static final Map<Object,Boolean> servers=Collections.synchronizedMap(new WeakHashMap<Object,Boolean>());
 private static boolean registered,panelActive;
 private static final int PANEL_WIDTH=37,PANEL_HEIGHT=37;
 private static final String[] POINTS={"BLUE","GREEN","RED","SILVER"};
 private static final int[] COLORS={0x0000ff,0x00ff00,0xff0000,0xbbbbbb};
 public static synchronized void registerNetworkHooks() {
  if(registered)return;
  try {
   Object fml=stat("cpw.mods.fml.common.FMLCommonHandler",new String[]{"instance"});
   call(call(fml,"bus"),"register",new SilverOculusHooks());registered=true;
  }catch(Exception error){throw new IllegalStateException("Cannot register Playground connection observer",error);}
 }
 @SubscribeEvent public void channels(FMLNetworkEvent.CustomPacketRegistrationEvent event) {
  if(event.side.isClient()&&event.registrations.contains("am2pg"))try {
   servers.put(get(event,"manager"),Boolean.valueOf("REGISTER".equals(event.operation)));
  }catch(Exception error){throw new IllegalStateException("Cannot observe Playground server registration",error);}
 }
 @SubscribeEvent public void disconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
  try{servers.remove(get(event,"manager"));}
  catch(Exception error){throw new IllegalStateException("Cannot reset Playground connection",error);}
 }
 public static boolean serverHasPlayground() {
  try {
   Object fml=stat("cpw.mods.fml.client.FMLClientHandler",new String[]{"instance"});
   Object manager=call(fml,"getClientToServerNetworkManager");
   return manager!=null&&Boolean.TRUE.equals(servers.get(manager));
  }catch(Exception error){return false;}
 }
 public static void beginDraw(Object gui) {panelActive=serverHasPlayground();}
 public static void nativePointFrame(Object gui,int x,int y,int u,int v,int width,int height) {
  if(panelActive)return;
  try{call(gui,new String[]{"func_73729_b","drawTexturedModalRect"},x,y,u,v,width,height);}
  catch(Exception error){throw new IllegalStateException("Cannot draw native Oculus points frame",error);}
 }
 public static int nativePointCount(Object font,String text,int x,int y,int color) {
  if(panelActive)return 0;
  try{return ((Number)call(font,new String[]{"func_78276_b","drawString"},text,x,y,color)).intValue();}
  catch(Exception error){throw new IllegalStateException("Cannot draw native Oculus points",error);}
 }
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
  if(!panelActive)return;
  GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
  try {
   Object mc=stat("net.minecraft.client.Minecraft",new String[]{"func_71410_x","getMinecraft"});
   Object font=get(mc,"field_71466_p","fontRenderer"),player=get(mc,"field_71439_g","thePlayer");
   Object skill=stat("am2.playerextensions.SkillData",new String[]{"For"},player);
   int[] box=panelBounds(gui);int left=box[0],top=box[1];
   GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_DEPTH_TEST);
   call(gui,new String[]{"func_73729_b","drawTexturedModalRect"},left,top,210,0,PANEL_WIDTH,PANEL_HEIGHT);
   for(int i=0;i<4;i++) {
    int x=left+5+i%2*12,y=top+5+i/2*10,color=COLORS[i];
    int balance=((Number)call(skill,"getSpellPoints",enumValue("am2.api.spell.enums.SkillPointTypes",POINTS[i]))).intValue();
    String count=compact(balance);
    int width=((Number)call(font,new String[]{"func_78256_a","getStringWidth"},count)).intValue();
    float scale=Math.min(1f,9f/Math.max(1,width));
    GL11.glPushMatrix();
    try {
     GL11.glTranslatef(x-width*scale/2f,y+4-4*scale,0);GL11.glScalef(scale,scale,1);
     GL11.glColor4f(1,1,1,1);
     call(font,new String[]{"func_78276_b","drawString"},count,0,0,color);
    }finally{GL11.glPopMatrix();}
   }
  }catch(Exception error){throw new IllegalStateException("Cannot display Playground Silver points",error);}
  finally{GL11.glPopAttrib();}
 }
 public static String compact(int value) {
  long magnitude=Math.abs((long)value);if(magnitude<1000)return Integer.toString(value);
  double divisor=magnitude>=999500000L?1000000000d:magnitude>=999500L?1000000d:1000d;
  String suffix=divisor==1000000000d?"B":divisor==1000000d?"M":"k";
  String number=String.format(Locale.ROOT,"%.1f",value/divisor);
  if(number.endsWith(".0"))number=number.substring(0,number.length()-2);
  return number+suffix;
 }
 public static int[] panelBounds(Object gui)throws Exception {
  int size=((Number)get(gui,"xSize")).intValue(),height=((Number)get(gui,"ySize")).intValue();
  int screenWidth=((Number)get(gui,"field_146294_l","width")).intValue();
  int screenHeight=((Number)get(gui,"field_146295_m","height")).intValue();
  return new int[]{Math.max(2,Math.min(screenWidth-PANEL_WIDTH-2,(screenWidth-size)/2+size-29)),Math.max(2,(screenHeight-height)/2),PANEL_WIDTH,PANEL_HEIGHT};
 }
 public static void drawCounterTooltip(Object gui,int mouseX,int mouseY) {
  if(!panelActive)return;
  try {
   int[] box=panelBounds(gui);int index=-1;
   for(int i=0;i<4;i++){int x=box[0]+i%2*12,y=box[1]+3+i/2*10;if(mouseX>=x&&mouseX<x+12&&mouseY>=y&&mouseY<y+10)index=i;}
   if(index<0)return;
   Object mc=stat("net.minecraft.client.Minecraft",new String[]{"func_71410_x","getMinecraft"});
   Object skill=stat("am2.playerextensions.SkillData",new String[]{"For"},get(mc,"field_71439_g","thePlayer"));
   Object balance=call(skill,"getSpellPoints",enumValue("am2.api.spell.enums.SkillPointTypes",POINTS[index]));
   String label=String.valueOf(stat("net.minecraft.util.StatCollector",new String[]{"func_74838_a","translateToLocal"},"am2pg.gui.points."+POINTS[index].toLowerCase(Locale.ROOT)));
   call(gui,new String[]{"func_146283_a","drawHoveringText"},Collections.singletonList(label+": "+balance),mouseX,mouseY);
  }catch(Exception error){throw new IllegalStateException("Cannot display Playground points tooltip",error);}
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
