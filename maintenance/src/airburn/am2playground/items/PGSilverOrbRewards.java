package airburn.am2playground.items;

import java.util.List;
import am2.api.spell.enums.SkillPointTypes;
import am2.playerextensions.ExtendedProperties;
import am2.playerextensions.SkillData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.common.util.FakePlayer;
import org.lwjgl.input.Keyboard;

/** Common-side item hook, usable even when optional integrations are absent. */
public final class PGSilverOrbRewards {
 private static final String SILVER="am2_silver_points";
 private PGSilverOrbRewards() {}

 /** The original item use method consumes one orb for both sets of rewards. */
 public static boolean grantSilver(ItemStack stack,EntityPlayer player) {
  if(stack==null || stack.field_77994_a<=0 || !stack.func_77942_o()
    || stack.func_77978_p().func_74762_e(SILVER)!=1
    || ExtendedProperties.For(player).getMagicLevel()<=0)return false;
  if(!player.field_70170_p.field_72995_K) {
   SkillData data=SkillData.For(player);
   data.incrementSpellPoints(SkillPointTypes.SILVER);
   data.forceSync();
   if(!(player instanceof FakePlayer))
    player.func_145747_a(new ChatComponentTranslation("am2pg.chatmsg.orbSilver",new Object[]{1}));
  }
  return true;
 }

 public static void addSilverTooltip(ItemStack stack,EntityPlayer player,List<String> lines) {
  if(stack!=null && stack.func_77942_o() && stack.func_77978_p().func_74762_e(SILVER)==1
    && ExtendedProperties.For(player).getMagicLevel()>0
    && (Keyboard.isKeyDown(42)||Keyboard.isKeyDown(54)))
   lines.add(new ChatComponentTranslation("am2pg.tooltip.orbSilver",new Object[]{1}).func_150260_c());
 }
}
