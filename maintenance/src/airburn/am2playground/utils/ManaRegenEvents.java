package airburn.am2playground.utils;

import airburn.am2playground.event.events.AM2ManaRegenPlayerEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;

/** Sign encodes cancellation; magnitude keeps the duration chosen by event handlers. */
public final class ManaRegenEvents {
 private ManaRegenEvents() {}
 private static int post(AM2ManaRegenPlayerEvent event) {
  boolean canceled=MinecraftForge.EVENT_BUS.post(event);
  int ticks=Math.max(1,event.regenTicks);
  return canceled ? -ticks : ticks;
 }
 public static int onManaRegen_ArmorMaterial(EntityPlayer player,int ticks) {
  return post(new AM2ManaRegenPlayerEvent.ArmorMaterial(player,ticks));
 }
 public static int onManaRegen_SkillData(EntityPlayer player,int ticks) {
  return post(new AM2ManaRegenPlayerEvent.SkillData(player,ticks));
 }
 public static int onManaRegen_ArmorInfusion(EntityPlayer player,int ticks) {
  return post(new AM2ManaRegenPlayerEvent.ArmorInfusion(player,ticks));
 }
}
