package de.kb1000.quiltmd.loader.entrypoint.mindustry;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.quiltmc.loader.impl.entrypoint.EntrypointPatch;
import org.quiltmc.loader.impl.entrypoint.EntrypointTransformer;
import org.quiltmc.loader.impl.launch.common.QuiltLauncher;

import java.io.IOException;
import java.util.ListIterator;
import java.util.function.Consumer;

import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

public class EntrypointPatchPlatform extends EntrypointPatch {
	private static final String TARGET = "mindustry.core.Platform";
	private static final String TARGET_DESCRIPTOR = "Lmindustry/core/Platform;";

	public EntrypointPatchPlatform(EntrypointTransformer transformer) {
		super(transformer);
	}

	@Override
	public void process(QuiltLauncher launcher, Consumer<ClassNode> classEmitter) {
		if (!classExists(launcher, TARGET)) {
			return;
		}
		try {
			boolean patched = false;
			final ClassNode node = loadClass(launcher, TARGET);
			if (node == null) {
				return;
			}
			final MethodNode methodNode = findMethod(node, method -> method.name.equals("loadJar"));
			if (methodNode == null) {
				return;
			}
			final ListIterator<AbstractInsnNode> it = methodNode.instructions.iterator();
			while (it.hasNext()) {
				AbstractInsnNode insn = it.next();
				if (insn instanceof MethodInsnNode) {
					final MethodInsnNode methodInsnNode = (MethodInsnNode) insn;
					if (methodInsnNode.owner.equals("java/lang/ClassLoader") && methodInsnNode.name.equals("getSystemClassLoader")) {
						debug("Patching " + TARGET + " to support Java mods under Fabric");
						patched = true;
						it.set(new LdcInsnNode(Type.getType(TARGET_DESCRIPTOR)));
						it.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;"));
						break;
					}
				}
			}
			if (patched)
			classEmitter.accept(node);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
