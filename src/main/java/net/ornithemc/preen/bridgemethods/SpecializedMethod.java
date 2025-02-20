package net.ornithemc.preen.bridgemethods;

import org.objectweb.asm.Type;

public class SpecializedMethod {

	public final String name;
	public final String descriptor;
	public final Type[] parameterTypes;
	public final Type returnType;

	public SpecializedMethod(String name, String descriptor, Type[] parameterTypes, Type returnType) {
		this.name = name;
		this.descriptor = descriptor;
		this.parameterTypes = parameterTypes;
		this.returnType = returnType;
	}
}
