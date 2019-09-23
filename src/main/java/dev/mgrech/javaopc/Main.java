package dev.mgrech.javaopc;

import com.github.javaparser.Providers;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main
{
	public static void main(String[] args) throws IOException
	{
		if(args.length != 1)
		{
			System.err.println("invalid arguments");
			System.exit(1);
		}

		var inputPath = Paths.get(args[0]);
		var outputPath = inputPath.getParent().resolve(inputPath.getFileName().toString().replace(".op", ""));
		var compilationUnit = JavaOperatorCompiler.process(Providers.provider(inputPath), new ReflectionTypeSolver());

		if(compilationUnit != null)
			Files.write(outputPath, compilationUnit.toString().getBytes(StandardCharsets.UTF_8));
	}
}
