import java.nio.file.*;import java.util.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public class PatchPortal implements Opcodes {
 public static void main(String[] a)throws Exception {
  ClassNode c=new ClassNode();new ClassReader(Files.readAllBytes(Paths.get(a[0]))).accept(c,0);
  String helper="airburn/am2playground/compat/thaumcraft/PGRitualPortalRewards";
  if(a[2].equals("factory")) {
   int n=0;for(MethodNode m:(List<MethodNode>)c.methods)if(m.name.equals("getCrimsonPortal"))for(AbstractInsnNode i:m.instructions.toArray())if(i.getOpcode()==ARETURN){m.instructions.insertBefore(i,new MethodInsnNode(INVOKESTATIC,helper,"mark","(Lnet/minecraft/entity/Entity;)Lnet/minecraft/entity/Entity;",false));n++;}
   if(n!=1)throw new IllegalStateException("Expected one factory return, found "+n);
  }else {
   for(MethodNode m:(List<MethodNode>)c.methods)if(m.name.equals("onRitualPortalDrops"))throw new IllegalStateException("Already patched");
   MethodNode m=new MethodNode(ACC_PUBLIC,"onRitualPortalDrops","(Lnet/minecraftforge/event/entity/living/LivingDropsEvent;)V",null,null);
   AnnotationNode ann=new AnnotationNode("Lcpw/mods/fml/common/eventhandler/SubscribeEvent;");ann.values=new ArrayList<Object>();ann.values.add("priority");ann.values.add(new String[]{"Lcpw/mods/fml/common/eventhandler/EventPriority;","LOWEST"});
   m.visibleAnnotations=new ArrayList<AnnotationNode>();m.visibleAnnotations.add(ann);
   m.instructions.add(new VarInsnNode(ALOAD,1));m.instructions.add(new MethodInsnNode(INVOKESTATIC,helper,"onDrops",m.desc,false));m.instructions.add(new InsnNode(RETURN));c.methods.add(m);
  }
  ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);Files.write(Paths.get(a[1]),w.toByteArray());
 }
}
