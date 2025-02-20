package net.ornithemc.preen.bridgemethods;

import java.util.LinkedHashSet;
import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

public class MergedBridgeMethodsSplitter extends ClassVisitor {

	private final SpecializedMethods specializedMethods;
	private final Set<MethodNode> specializedMethodNodes;

	private String className;
	private boolean isInterface;

	public MergedBridgeMethodsSplitter(int api, ClassWriter writer, SpecializedMethods specializedMethods) {
		super(api, writer);

		this.specializedMethods = specializedMethods;
		this.specializedMethodNodes = new LinkedHashSet<>();
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.className = name;
		this.isInterface = (access & Opcodes.ACC_INTERFACE) != 0;

		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if ((access & Opcodes.ACC_BRIDGE) != 0) {
			SpecializedMethod specializedMethod = this.specializedMethods.get(this.className, name, descriptor);

			if (specializedMethod != null) {
				int specializedAccess = access & ~Opcodes.ACC_FINAL & ~Opcodes.ACC_SYNTHETIC & ~Opcodes.ACC_BRIDGE;
				MethodNode specializedMethodNode = new MethodNode(specializedAccess, specializedMethod.name, specializedMethod.descriptor, null, exceptions);

				// The specialized type(s) may have been unused in the original
				// source, in which case the casts are stripped in the merged
				// bridge method. In that case, only write the 'specialized'
				// method since it has the same descriptor as the bridge method.
				if (this.specializedMethodNodes.add(specializedMethodNode)) {
					MethodVisitor writer = specializedMethod.descriptor.equals(descriptor)
						? null
						: super.visitMethod(access, name, descriptor, signature, exceptions);

					return new MergedBridgeMethodSplitter(this.api, this.className, name, descriptor, writer, specializedMethod, specializedMethodNode);
				} else {
					return null; // duplicate method (won't happen but just in case)
				}
			}
		}

		return super.visitMethod(access, name, descriptor, signature, exceptions);
	}

	@Override
	public void visitEnd() {
		for (MethodNode specializedMethod : this.specializedMethodNodes) {
			specializedMethod.accept(this.cv);
		}

		super.visitEnd();
	}

	private class MergedBridgeMethodSplitter extends MergedBridgeMethodVisitor {

		private final MethodVisitor bridgeMethodWriter;
		private final SpecializedMethod specializedMethod;

		public MergedBridgeMethodSplitter(int api, String owner, String name, String descriptor, MethodVisitor bridgeMethodWriter, SpecializedMethod specializedMethod, MethodNode specializedMethodWriter) {
			super(api, specializedMethodWriter, owner, name, descriptor);

			this.specializedMethod = specializedMethod;
			this.bridgeMethodWriter = bridgeMethodWriter;
		}

		@Override
		public void visitCode() {
			super.visitCode();

			if (this.bridgeMethodWriter != null) {
				// all code visits are passed to the specialized method
				// write the bridge method manually
				this.bridgeMethodWriter.visitCode();

				// load current class instance var
				this.bridgeMethodWriter.visitVarInsn(Opcodes.ALOAD, 0);

				// then load the method parameters, and cast the  relevant
				// ones to their specialized types

				int index = 0;
				int varIndex = 1;

				for (Type parameterType : this.parameterTypes) {
					this.bridgeMethodWriter.visitVarInsn(parameterType.getOpcode(Opcodes.ILOAD), varIndex);

					if (this.specializedMethod.parameterTypes[index] != null) {
						Type specializedParameterType = this.specializedMethod.parameterTypes[index];
						String specializedParameterTypeName = specializedParameterType.getInternalName();
						
						this.bridgeMethodWriter.visitTypeInsn(Opcodes.CHECKCAST, specializedParameterTypeName);
					}

					index++;
					varIndex += parameterType.getSize();
				}

				// call the specialized method
				this.bridgeMethodWriter.visitMethodInsn(Opcodes.INVOKEVIRTUAL, this.owner, this.specializedMethod.name, this.specializedMethod.descriptor, MergedBridgeMethodsSplitter.this.isInterface);

				// and lastly, return
				this.bridgeMethodWriter.visitInsn(this.returnType.getOpcode(Opcodes.IRETURN));

				// find max stack size and max locals
				// varIndex counts up the size of all vars
				// no local vars are created
				this.bridgeMethodWriter.visitMaxs(varIndex, varIndex);

				this.bridgeMethodWriter.visitEnd();
			}
		}
	}
}
