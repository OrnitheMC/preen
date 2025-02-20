package net.ornithemc.preen.bridgemethods;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MergedBridgeMethodsAccessModifier extends ClassVisitor {

	private final SpecializedMethods specializedMethods;

	private String className;

	public MergedBridgeMethodsAccessModifier(int api, ClassWriter writer, SpecializedMethods specializedMethods) {
		super(api, writer);

		this.specializedMethods = specializedMethods;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.className = name;

		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if ((access & Opcodes.ACC_BRIDGE) != 0) {
			SpecializedMethod specializedMethod = this.specializedMethods.get(this.className, name, descriptor);

			if (specializedMethod != null) {
				access &= ~Opcodes.ACC_FINAL & ~Opcodes.ACC_SYNTHETIC & ~Opcodes.ACC_BRIDGE;
			}
		}

		return super.visitMethod(access, name, descriptor, signature, exceptions);
	}
}
