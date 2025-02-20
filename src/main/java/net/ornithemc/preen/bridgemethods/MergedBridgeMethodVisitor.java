package net.ornithemc.preen.bridgemethods;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class MergedBridgeMethodVisitor extends MethodVisitor {

	protected final String owner;
	protected final String name;
	protected final String descriptor;

	protected final Type[] parameterTypes;
	protected final Type returnType;

	protected Stage stage = Stage.CHECK_CAST;
	protected boolean foundInvoke;

	protected MergedBridgeMethodVisitor(int api, String owner, String name, String descriptor) {
		this(api, null, owner, name, descriptor);
	}

	protected MergedBridgeMethodVisitor(int api, MethodVisitor methodVisitor, String owner, String name, String descriptor) {
		super(api, methodVisitor);

		this.owner = owner;
		this.name = name;
		this.descriptor = descriptor;

		Type desc = Type.getMethodType(descriptor);

		this.parameterTypes = desc.getArgumentTypes();
		this.returnType = desc.getReturnType();
	}

	@Override
	public void visitInsn(int opcode) {
		switch (opcode) {
		case Opcodes.IRETURN:
		case Opcodes.LRETURN:
		case Opcodes.FRETURN:
		case Opcodes.DRETURN:
		case Opcodes.ARETURN:
		case Opcodes.RETURN:
			break;
		default:
			this.specializedMethodBodyInsn();
		}

		super.visitInsn(opcode);
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		this.specializedMethodBodyInsn();
		super.visitIntInsn(opcode, operand);
	}

	@Override
	public void visitVarInsn(int opcode, int varIndex) {
		switch (opcode) {
		case Opcodes.ILOAD:
		case Opcodes.LLOAD:
		case Opcodes.FLOAD:
		case Opcodes.DLOAD:
		case Opcodes.ALOAD:
			if (this.stage != Stage.CHECK_CAST) {
				this.bridgeMethodBodyInsn();
			}

			break;
		default:
			this.specializedMethodBodyInsn();
		}

		super.visitVarInsn(opcode, varIndex);
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		if (this.stage != Stage.CHECK_CAST || opcode != Opcodes.CHECKCAST) {
			this.specializedMethodBodyInsn();
		}

		super.visitTypeInsn(opcode, type);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
		this.specializedMethodBodyInsn();
		super.visitFieldInsn(opcode, owner, name, descriptor);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		switch (opcode) {
		case Opcodes.INVOKEINTERFACE:
		case Opcodes.INVOKESPECIAL:
		case Opcodes.INVOKEVIRTUAL:
			this.bridgeMethodBodyInsn();

			if (this.stage == Stage.BRIDGE_METHOD_BODY && !this.foundInvoke) {
				if (owner.equals(this.owner) && !descriptor.equals(this.descriptor)) {
					this.foundInvoke = true;
				}
			}

			break;
		default:
			this.specializedMethodBodyInsn();
		}

		super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
		this.specializedMethodBodyInsn();
		super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		this.specializedMethodBodyInsn();
		super.visitJumpInsn(opcode, label);
	}

	@Override
	public void visitLdcInsn(Object value) {
		this.specializedMethodBodyInsn();
		super.visitLdcInsn(value);
	}

	@Override
	public void visitIincInsn(int varIndex, int increment) {
		this.specializedMethodBodyInsn();
		super.visitIincInsn(varIndex, increment);
	}

	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
		this.specializedMethodBodyInsn();
		super.visitTableSwitchInsn(min, max, dflt, labels);
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		this.specializedMethodBodyInsn();
		super.visitLookupSwitchInsn(dflt, keys, labels);
	}

	@Override
	public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
		this.specializedMethodBodyInsn();
		super.visitMultiANewArrayInsn(descriptor, numDimensions);
	}

	private void bridgeMethodBodyInsn() {
		if (this.stage == Stage.CHECK_CAST) {
			this.stage = Stage.BRIDGE_METHOD_BODY;
		}
		if (this.stage == Stage.BRIDGE_METHOD_BODY && this.foundInvoke) {
			this.specializedMethodBodyInsn();
		}
		if (this.stage == Stage.SPECIALIZED_METHOD_BODY) {
			this.specializedMethodBodyInsn();
		}
	}

	private void specializedMethodBodyInsn() {
		if (this.stage == Stage.CHECK_CAST) {
			this.stage = Stage.SPECIALIZED_METHOD_BODY;
		}
		if (this.stage == Stage.BRIDGE_METHOD_BODY) {
			this.stage = Stage.SPECIALIZED_METHOD_BODY;
		}
	}

	protected enum Stage {
		CHECK_CAST, BRIDGE_METHOD_BODY, SPECIALIZED_METHOD_BODY
	}
}
