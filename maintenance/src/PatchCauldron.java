import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

// Surgical change: reuse the addon's existing idempotent ingredient registrar.
public class PatchCauldron implements Opcodes {
  public static void main(String[] args) throws Exception {
    ClassNode c = new ClassNode();
    new ClassReader(Files.readAllBytes(Paths.get(args[0]))).accept(c,0);
    int changes=0;
    for (MethodNode m : (List<MethodNode>)c.methods) if(m.name.equals("brews") && m.desc.equals("()V")) {
      InsnList a=new InsnList();
      a.add(new InsnNode(ICONST_4));
      a.add(new TypeInsnNode(ANEWARRAY,"net/minecraft/item/ItemStack"));
      int[] metas={0,7,9,15};
      for(int i=0;i<4;i++) {
        a.add(new InsnNode(DUP)); a.add(new LdcInsnNode(i));
        a.add(new TypeInsnNode(NEW,"net/minecraft/item/ItemStack")); a.add(new InsnNode(DUP));
        a.add(new FieldInsnNode(GETSTATIC,"am2/items/ItemsCommonProxy","rune","Lam2/items/ItemRune;"));
        a.add(new InsnNode(ICONST_1)); a.add(new LdcInsnNode(metas[i]));
        a.add(new MethodInsnNode(INVOKESPECIAL,"net/minecraft/item/ItemStack","<init>","(Lnet/minecraft/item/Item;II)V",false));
        a.add(new InsnNode(AASTORE));
      }
      a.add(new MethodInsnNode(INVOKESTATIC,c.name,"ensureIngredients","([Lnet/minecraft/item/ItemStack;)V",false));
      m.instructions.insert(a); changes++;
    }
    if(changes!=1) throw new IllegalStateException("Expected exactly one brews method");
    ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS); c.accept(w);
    Files.write(Paths.get(args[1]),w.toByteArray());
    System.out.println("Patched one method; four rune ingredients registered via ensureIngredients");
  }
}
