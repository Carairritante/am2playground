import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Add reward hooks without redistributing the original item implementation. */
public final class PatchSilverOrb implements Opcodes {
 public static void main(String[] args) throws Exception {
  ClassNode c=new ClassNode();
  new ClassReader(Files.readAllBytes(Paths.get(args[0]))).accept(c,0);
  String helper="airburn/am2playground/items/PGSilverOrbRewards";
  int useHooks=0,tooltipHooks=0;
  for(MethodNode m:(List<MethodNode>)c.methods) {
   for(AbstractInsnNode i:m.instructions.toArray())
    if(i instanceof MethodInsnNode && ((MethodInsnNode)i).owner.equals(helper))
     throw new IllegalStateException("Item is already patched");
   if(m.name.equals("func_77659_a")) {
    // The first false local is the original 'should consume this orb' flag.
    for(AbstractInsnNode i:m.instructions.toArray())
     if(i.getOpcode()==ISTORE && i.getPrevious()!=null && i.getPrevious().getOpcode()==ICONST_0) {
      InsnList hook=new InsnList();
      hook.add(new VarInsnNode(ALOAD,1));hook.add(new VarInsnNode(ALOAD,3));
      hook.add(new MethodInsnNode(INVOKESTATIC,helper,"grantSilver",
       "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;)Z",false));
      hook.add(new VarInsnNode(ISTORE,((VarInsnNode)i).var));
      m.instructions.insert(i,hook);useHooks++;break;
     }
   }
   if(m.name.equals("addText")) {
    // Insert once, before the original checks; the helper checks level, NBT and Shift.
    InsnList hook=new InsnList();
    hook.add(new VarInsnNode(ALOAD,1));hook.add(new VarInsnNode(ALOAD,2));hook.add(new VarInsnNode(ALOAD,3));
    hook.add(new MethodInsnNode(INVOKESTATIC,helper,"addSilverTooltip",
     "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;Ljava/util/List;)V",false));
    m.instructions.insert(hook);tooltipHooks++;
   }
  }
  if(useHooks!=1 || tooltipHooks!=1)throw new IllegalStateException("Unexpected item layout: "+useHooks+", "+tooltipHooks);
  ClassWriter out=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(out);
  Files.write(Paths.get(args[1]),out.toByteArray());
 }
}
