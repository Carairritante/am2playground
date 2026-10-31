"""Compile the new helper; API signatures are compile-only, never shipped."""
from pathlib import Path
import argparse, shutil, subprocess

STUBS = {'airburn/am2playground/compat/PGCompat.java': 'package airburn.am2playground.compat; public class '
                                               'PGCompat { public static boolean bloodmagicLoaded; '
                                               '}',
 'airburn/am2playground/items/PGItems.java': 'package airburn.am2playground.items; public class '
                                             'PGItems {public static net.minecraft.item.ItemStack '
                                             'whiteOrbBloodBoss;}',
 'net/minecraft/entity/Entity.java': 'package net.minecraft.entity; public class Entity { public '
                                     'net.minecraft.world.World field_70170_p; public double '
                                     'field_70165_t,field_70163_u,field_70161_v; public '
                                     'net.minecraft.nbt.NBTTagCompound getEntityData(){return '
                                     'null;} }',
 'net/minecraft/entity/EntityLivingBase.java': 'package net.minecraft.entity; public class '
                                               'EntityLivingBase extends Entity {}',
 'net/minecraft/entity/item/EntityItem.java': 'package net.minecraft.entity.item; public class '
                                              'EntityItem extends net.minecraft.entity.Entity { '
                                              'public EntityItem(net.minecraft.world.World '
                                              'w,double x,double y,double '
                                              'z,net.minecraft.item.ItemStack s){} }',
 'net/minecraft/item/ItemStack.java': 'package net.minecraft.item; public class ItemStack { public '
                                      'ItemStack func_77946_l(){return null;} }',
 'net/minecraft/nbt/NBTTagCompound.java': 'package net.minecraft.nbt; public class NBTTagCompound '
                                          '{ public boolean func_74767_n(String k){return false;} '
                                          'public void func_74757_a(String k,boolean b){} }',
 'net/minecraft/world/World.java': 'package net.minecraft.world; public class World { public '
                                   'boolean field_72995_K; }',
 'net/minecraftforge/event/entity/living/LivingDropsEvent.java': 'package '
                                                                 'net.minecraftforge.event.entity.living; '
                                                                 'public class LivingDropsEvent { '
                                                                 'public '
                                                                 'net.minecraft.entity.EntityLivingBase '
                                                                 'entityLiving; public boolean '
                                                                 'recentlyHit; public '
                                                                 'java.util.ArrayList<net.minecraft.entity.item.EntityItem> '
                                                                 'drops; public boolean '
                                                                 'isCanceled(){return false;} }',
 'thaumcraft/common/entities/monster/boss/EntityCultistPortal.java': 'package '
                                                                     'thaumcraft.common.entities.monster.boss; '
                                                                     'public class '
                                                                     'EntityCultistPortal extends '
                                                                     'net.minecraft.entity.EntityLivingBase '
                                                                     '{}'}

STUBS.update({
 'net/minecraftforge/common/util/FakePlayer.java': 'package net.minecraftforge.common.util; public class FakePlayer extends net.minecraft.entity.player.EntityPlayer {}',
 'net/minecraft/entity/player/EntityPlayer.java': 'package net.minecraft.entity.player; public class EntityPlayer extends net.minecraft.entity.EntityLivingBase { public void func_145747_a(net.minecraft.util.IChatComponent message){} }',
 'net/minecraft/item/ItemStack.java': 'package net.minecraft.item; public class ItemStack { public int field_77994_a; public ItemStack func_77946_l(){return null;} public boolean func_77942_o(){return false;} public net.minecraft.nbt.NBTTagCompound func_77978_p(){return null;} }',
 'net/minecraft/nbt/NBTTagCompound.java': 'package net.minecraft.nbt; public class NBTTagCompound { public boolean func_74767_n(String k){return false;} public void func_74757_a(String k,boolean b){} public void func_74768_a(String k,int v){} public int func_74762_e(String k){return 0;} }',
 'net/minecraft/util/IChatComponent.java': 'package net.minecraft.util; public interface IChatComponent {String func_150260_c();}',
 'net/minecraft/util/ChatComponentTranslation.java': 'package net.minecraft.util; public class ChatComponentTranslation implements IChatComponent {public ChatComponentTranslation(String k,Object... args){} public String func_150260_c(){return null;} }',
 'am2/api/spell/enums/SkillPointTypes.java': 'package am2.api.spell.enums; public enum SkillPointTypes {BLUE,GREEN,RED,SILVER}',
 'am2/playerextensions/ExtendedProperties.java': 'package am2.playerextensions; public class ExtendedProperties {public static ExtendedProperties For(net.minecraft.entity.EntityLivingBase e){return null;} public int getMagicLevel(){return 0;} }',
 'am2/playerextensions/SkillData.java': 'package am2.playerextensions; public class SkillData {public static SkillData For(net.minecraft.entity.player.EntityPlayer p){return null;} public void incrementSpellPoints(am2.api.spell.enums.SkillPointTypes t){} public void forceSync(){} }',
 'org/lwjgl/input/Keyboard.java': 'package org.lwjgl.input; public class Keyboard {public static boolean isKeyDown(int key){return false;} }',
})

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--javac', default='javac')
    args = parser.parse_args()
    root = Path(__file__).resolve().parent
    build = root / 'build'
    stubs = build / 'compile-only-stubs'
    sources = []
    for name, text in STUBS.items():
        target = stubs / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text, encoding='utf-8')
        sources.append(str(target))
    helper = Path('airburn/am2playground/compat/thaumcraft/PGRitualPortalRewards')
    sources.append(str(root / 'src' / helper.with_suffix('.java')))
    item_helper = Path('airburn/am2playground/items/PGSilverOrbRewards')
    sources.append(str(root / 'src' / item_helper.with_suffix('.java')))
    classes = build / 'compile-only-classes'
    subprocess.run([args.javac, '--release', '8', '-d', str(classes), *sources], check=True)
    payload = build / 'payload' / helper.with_suffix('.class')
    payload.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(classes / helper.with_suffix('.class'), payload)
    item_payload = build / 'payload' / item_helper.with_suffix('.class')
    item_payload.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(classes / item_helper.with_suffix('.class'), item_payload)
    print('Payload:', payload)
    print('Payload:', item_payload)
    print('Only these payload classes may be packaged; do not ship compile-only API stubs.')

if __name__ == '__main__':
    main()
