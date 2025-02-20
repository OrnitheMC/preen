package net.ornithemc.preen.bridgemethods;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SpecializedMethods {

	private final Map<String, Map<String, SpecializedMethod>> specializedMethods;

	public SpecializedMethods() {
		this.specializedMethods = new HashMap<>();
	}

	public void put(String owner, String name, String descriptor, SpecializedMethod specializedMethod) {
		this.specializedMethods.computeIfAbsent(owner, key -> new HashMap<>()).put(name + descriptor, specializedMethod);
	}

	public SpecializedMethod get(String owner, String name, String descriptor) {
		return this.specializedMethods.getOrDefault(owner, Collections.emptyMap()).get(name + descriptor);
	}

	public boolean has(String owner) {
		return this.specializedMethods.containsKey(owner);
	}
}
