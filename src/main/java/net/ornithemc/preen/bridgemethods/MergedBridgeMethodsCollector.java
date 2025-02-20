package net.ornithemc.preen.bridgemethods;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class MergedBridgeMethodsCollector extends ClassVisitor {

	private final SpecializedMethods specializedMethods;

	private String className;

	public MergedBridgeMethodsCollector(int api, SpecializedMethods specializedMethods) {
		super(api);

		this.specializedMethods = specializedMethods;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.className = name;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if ((access & Opcodes.ACC_BRIDGE) != 0) {
			return new MergedBridgeMethodCollector(this.api, this.className, name, descriptor);
		}

		return null;
	}

	private class MergedBridgeMethodCollector extends MergedBridgeMethodVisitor {

		private final Type[] specializedParams;

		private int lastLoadedVar = -1;

		public MergedBridgeMethodCollector(int api, String owner, String name, String descriptor) {
			super(api, owner, name ,descriptor);

			this.specializedParams = new Type[2 * this.parameterTypes.length + 1];
		}

		@Override
		public void visitVarInsn(int opcode, int varIndex) {
			this.lastLoadedVar = varIndex;
			super.visitVarInsn(opcode, varIndex);
		}

		@Override
		public void visitTypeInsn(int opcode, String type) {
			if (this.stage == Stage.CHECK_CAST && opcode == Opcodes.CHECKCAST) {
				if (this.lastLoadedVar != -1) {
					this.specializedParams[this.lastLoadedVar] = Type.getObjectType(type);
				}
			}

			super.visitTypeInsn(opcode, type);
		}

		@Override
		public void visitEnd() {
			if (this.stage == Stage.SPECIALIZED_METHOD_BODY) {
				Type[] parameterTypes = new Type[this.parameterTypes.length];
				Type[] specializedParameterTypes = new Type[this.parameterTypes.length];
				Type returnType = this.returnType;

				int index = 0;
				int varIndex = 1;

				for (Type parameterType : this.parameterTypes) {
					if (this.specializedParams[varIndex] == null) {
						parameterTypes[index] = parameterType;
					} else {
						parameterTypes[index] = specializedParameterTypes[index] = this.specializedParams[varIndex];
					}

					index++;
					varIndex += parameterType.getSize();
				}

				Type specializedDesc = Type.getMethodType(returnType, parameterTypes);
				String specializedDescriptor = specializedDesc.getDescriptor();

				MergedBridgeMethodsCollector.this.specializedMethods.put(
					MergedBridgeMethodsCollector.this.className,
					this.name,
					this.descriptor,
					new SpecializedMethod(this.name, specializedDescriptor, specializedParameterTypes, returnType)
				);
			}

			super.visitEnd();
		}
	}
}
