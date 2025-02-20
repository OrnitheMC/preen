package net.ornithemc.preen;

import java.io.IOException;
import java.nio.file.Paths;

public class Main {

	public static void main(String... args) throws Exception {
		if (args.length < 1) {
			printUsageAndExit("No command given!");
		}

		String command = args[0];

		switch (command) {
		case "split-merged-bridge-methods":
			splitMergedBridgeMethods(args);
			break;
		case "modify-merged-bridge-methods-access":
			modifyMergedBridgeMethodsAccess(args);
			break;
		default:
			printUsageAndExit("Unknown command " + command);
		}
	}

	private static void checkArgumentCount(String[] args, int expectedCount) {
		if (args.length != expectedCount) {
			printUsageAndExit("Incorrect number of arguments! Expected " + (expectedCount - 1) + ", got " + (args.length - 1) + "...");
		}
	}

	private static void printUsageAndExit(String message) {
		System.out.println(message);
		System.out.println("Correct usage:");
		System.out.println("  split-merged-bridge-methods <jar>");
		System.out.println("  modify-merged-bridge-methods-access <jar>");

		System.exit(1);
	}

	private static void splitMergedBridgeMethods(String[] args) throws IOException {
		checkArgumentCount(args, 2);
		Preen.splitMergedBridgeMethods(Paths.get(args[1]));
	}

	private static void modifyMergedBridgeMethodsAccess(String[] args) throws IOException {
		checkArgumentCount(args,2);
		Preen.modifyMergedBridgeMethodsAccess(Paths.get(args[1]));
	}
}
