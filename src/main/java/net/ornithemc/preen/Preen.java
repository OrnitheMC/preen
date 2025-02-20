package net.ornithemc.preen;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import net.ornithemc.preen.bridgemethods.MergedBridgeMethodsAccessModifier;
import net.ornithemc.preen.bridgemethods.MergedBridgeMethodsCollector;
import net.ornithemc.preen.bridgemethods.MergedBridgeMethodsSplitter;
import net.ornithemc.preen.bridgemethods.SpecializedMethods;

public class Preen {

	public static void splitMergedBridgeMethods(Path jar) throws IOException {
		new Preen(jar).splitMergedBridgeMethods();
	}

	public static void modifyMergedBridgeMethodsAccess(Path jar) throws IOException {
		new Preen(jar).modifyMergedBridgeMethodsAccess();
	}

	private final Path jar;

	private Preen(Path jar) {
		this.jar = jar;
	}

	private void splitMergedBridgeMethods() throws IOException {
		SpecializedMethods specializedMethods = new SpecializedMethods();

		this.collectMergedBridgeMethods(specializedMethods);
		this.splitMergedBridgeMethods(specializedMethods);
	}

	private void modifyMergedBridgeMethodsAccess() throws IOException {
		SpecializedMethods specializedMethods = new SpecializedMethods();

		this.collectMergedBridgeMethods(specializedMethods);
		this.modifyMergedBridgeMethodsAccess(specializedMethods);
	}

	private void collectMergedBridgeMethods(SpecializedMethods specializedMethods) throws IOException {
		iterateClasses(jar, (file, reader) -> {
			reader.accept(new MergedBridgeMethodsCollector(Opcodes.ASM9, specializedMethods),  0);
		});
	}

	private void splitMergedBridgeMethods(SpecializedMethods specializedMethods) throws IOException {
		iterateClasses(jar, (file, reader) -> {
			if (!specializedMethods.has(reader.getClassName())) {
				return;
			}

			ClassWriter writer = new ClassWriter(Opcodes.ASM9);
			ClassVisitor splitter = new MergedBridgeMethodsSplitter(Opcodes.ASM9, writer, specializedMethods);

			reader.accept(splitter,  0);

			try {
				Files.write(file, writer.toByteArray());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	private void modifyMergedBridgeMethodsAccess(SpecializedMethods specializedMethods) throws IOException {
		iterateClasses(jar, (file, reader) -> {
			if (!specializedMethods.has(reader.getClassName())) {
				return;
			}

			ClassWriter writer = new ClassWriter(Opcodes.ASM9);
			ClassVisitor modifier = new MergedBridgeMethodsAccessModifier(Opcodes.ASM9, writer, specializedMethods);

			reader.accept(modifier,  0);

			try {
				Files.write(file, writer.toByteArray());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	private static void iterateClasses(Path jar, BiConsumer<Path, ClassReader> action) throws IOException {
		try (FileSystem fs = FileSystems.newFileSystem(jar, (ClassLoader) null)) {
			for (Path root : fs.getRootDirectories()) {
				try (Stream<Path> classFiles = Files.find(root, Integer.MAX_VALUE, (p, a) -> a.isRegularFile() && p.toString().endsWith(".class"))) {
					classFiles.forEach(classFile -> {
						try (InputStream is = Files.newInputStream(classFile)) {
							action.accept(classFile, new ClassReader(is));
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					});
				} catch (UncheckedIOException e) {
					throw e.getCause();
				}
			}
		}
	}
}
