package cauldronprobe;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

@Mod(modid="cauldronprobe",name="Temporary Cauldron Integration Probe",version="1",dependencies="required-after:am2pg;required-after:witchery")
public class CauldronProbe {
  static Object call(Object o,String name,Object...args) throws Exception {
    for(Method m:o.getClass().getMethods()) if(m.getName().equals(name) && m.getParameterTypes().length==args.length) {
      try { return m.invoke(o,args); } catch(IllegalArgumentException bad) { }
    }
    throw new NoSuchMethodException(name);
  }
  static Object field(String c,String f)throws Exception{return Class.forName(c).getField(f).get(null);}
  static Object stack(Object item,int meta)throws Exception{
    return Class.forName("net.minecraft.item.ItemStack").getConstructor(Class.forName("net.minecraft.item.Item"),int.class,int.class).newInstance(item,1,meta);
  }
  static Object nbt()throws Exception{return Class.forName("net.minecraft.nbt.NBTTagCompound").newInstance();}
  static String mapped(String srg,String dev){return System.getProperty("probe.dev","false").equals("true")?dev:srg;}
  static void add(Object nbt,Object list,Object stack)throws Exception {
    Object itemTag=nbt();
    call(stack,mapped("func_77955_b","writeToNBT"),itemTag);
    call(list,mapped("func_74742_a","appendTag"),itemTag);
  }
  @Mod.EventHandler public void complete(FMLLoadCompleteEvent event) {
    List<String> lines=new ArrayList<String>(); int exit=0;
    try {
      Object registry=field("com.emoniph.witchery.brewing.WitcheryBrewRegistry","INSTANCE");
      Object rune=field("am2.items.ItemsCommonProxy","rune");
      Object tag=nbt(),list=Class.forName("net.minecraft.nbt.NBTTagList").newInstance();
      call(tag,mapped("func_74782_a","setTag"),"Items",list);
      boolean all=true;
      for(int meta:new int[]{0,7,9,15}) {
        Object s=stack(rune,meta),action=call(registry,"getActionForItemStack",s);
        lines.add("RUNE "+meta+" REGISTERED="+(action!=null));
        if(action==null){all=false;continue;}
        if(!Boolean.TRUE.equals(call(registry,"canAdd",tag,action,true)))throw new AssertionError("Rejected rune "+meta);
        add(tag,list,s);
      }
      Object note=field("airburn.am2playground.items.PGItems","note");
      Object action=call(registry,"getActionForItemStack",stack(note,0));
      boolean can=Boolean.TRUE.equals(call(registry,"canAdd",tag,action,true));
      lines.add("NOTE0 CAN_FINALIZE="+can);
      if(all) {
        if(!can)throw new AssertionError("Note cannot finalize");
        Class<?> alc=Class.forName("com.emoniph.witchery.brewing.action.BrewActionList");
        Field ingredients=registry.getClass().getDeclaredField("ingredients");ingredients.setAccessible(true);
        Object actions=alc.getConstructors()[0].newInstance(tag,ingredients.get(registry));
        Method result=action.getClass().getDeclaredMethod("getRecipeResult",alc);result.setAccessible(true);
        Object out=result.invoke(action,actions);
        Object resource=field("airburn.am2playground.items.PGItems","resource");
        if(call(out,mapped("func_77973_b","getItem"))!=resource || !Integer.valueOf(0).equals(call(out,mapped("func_77960_j","getItemDamage"))))throw new AssertionError("Wrong recipe output");
        lines.add("VOID_PARADOX OUTPUT=PASS");
        Object liveNote=stack(note,0);
        liveNote.getClass().getField(mapped("field_77994_a","stackSize")).setInt(liveNote,0);
        add(tag,list,liveNote);
        Object withNote=alc.getConstructors()[0].newInstance(tag,ingredients.get(registry));
        if(call(withNote,"getTopAction")!=action)throw new AssertionError("Temporary Note lost in cauldron serialization");
        if(result.invoke(action,withNote)==null)throw new AssertionError("Temporary Note invalidated output");
        lines.add("XYLOPHONE_ZERO_COUNT_NOTE_SERIALIZATION=PASS");
        if(call(registry,"getActionForItemStack",stack(note,1))!=null)throw new AssertionError("Unexpected note1 action");
        lines.add("WRONG_NOTE_REJECTED=PASS");
        // Idempotency: do not replace any previously registered action.
        Object before=call(registry,"getActionForItemStack",stack(rune,7));
        Method ensure=Class.forName("airburn.am2playground.compat.witchery.brews.PGWitcheryBrews").getDeclaredMethod("ensureIngredients",java.lang.reflect.Array.newInstance(Class.forName("net.minecraft.item.ItemStack"),0).getClass());ensure.setAccessible(true);
        Object arr=java.lang.reflect.Array.newInstance(Class.forName("net.minecraft.item.ItemStack"),1);java.lang.reflect.Array.set(arr,0,stack(rune,7));ensure.invoke(null,arr);
        if(before!=call(registry,"getActionForItemStack",stack(rune,7)))throw new AssertionError("Existing action replaced");
        lines.add("IDEMPOTENCY=PASS");
      } else if(can)throw new AssertionError("Baseline unexpectedly works");
      lines.add(all?"PATCHED_INTEGRATION=PASS":"ORIGINAL_BUG_REPRODUCED=PASS");
    }catch(Throwable t){exit=1;lines.add("ERROR="+t);t.printStackTrace();}
    try {Files.write(Paths.get("probe-result.txt"),lines);}catch(Exception ex){ex.printStackTrace();}
    for(String line:lines)System.out.println("CAULDRON_PROBE "+line);
    cpw.mods.fml.common.FMLCommonHandler.instance().exitJava(exit,false);
  }
}
