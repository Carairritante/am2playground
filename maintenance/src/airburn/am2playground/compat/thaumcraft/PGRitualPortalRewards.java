package airburn.am2playground.compat.thaumcraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import thaumcraft.common.entities.monster.boss.EntityCultistPortal;
import airburn.am2playground.items.PGItems;
/** Reward only portals created by the addon's Eldritch Mobilization factory. */
public final class PGRitualPortalRewards {
 private static final String RITUAL="am2pg:eldritchMobilization";
 private static final String CLAIMED="am2pg:eldritchOrbDropped";
 private static final String SILVER="am2_silver_points";
 public static Entity mark(Entity portal) {
  if(portal!=null)portal.getEntityData().func_74757_a(RITUAL,true);
  return portal;
 }
 public static void onDrops(LivingDropsEvent e) {
  if(e.isCanceled() || !e.recentlyHit || !(e.entityLiving instanceof EntityCultistPortal))return;
  Entity portal=e.entityLiving;
  if(portal.field_70170_p==null || portal.field_70170_p.field_72995_K)return;
  NBTTagCompound data=portal.getEntityData();
  if(!data.func_74767_n(RITUAL) || data.func_74767_n(CLAIMED))return;
  // Beta 0.12.1 resource meta 3 is the white orb; give it only the Silver reward.
  ItemStack reward=new ItemStack(PGItems.resource,1,3);
  NBTTagCompound tag=new NBTTagCompound();
  tag.func_74768_a(SILVER,1);
  reward.func_77982_d(tag);
  e.drops.add(new EntityItem(portal.field_70170_p,portal.field_70165_t,portal.field_70163_u,portal.field_70161_v,reward));
  data.func_74757_a(CLAIMED,true);
 }
}
