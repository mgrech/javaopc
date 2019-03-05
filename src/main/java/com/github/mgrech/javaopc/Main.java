package com.github.mgrech.javaopc;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseStart;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Providers;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main
{
	public static void main(String[] args) throws IOException
	{
		var path = Paths.get("tests/Sum.op.java");
		var config = new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_11)
		                                      .setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver()));

		var parser = new JavaParser(config);
		var parse = parser.parse(ParseStart.COMPILATION_UNIT, Providers.provider(path));

		if(!parse.isSuccessful())
		{
			for(var problem : parse.getProblems())
				System.out.println(problem);

			System.exit(1);
		}

		var cu = parse.getResult().get();
		var solver = config.getSymbolResolver().get();
		new OperatorVisitor(solver).visitLeavesFirst(cu);

		var outputPath = path.getParent().resolve(path.getFileName().toString().replace(".op", ""));
		Files.write(outputPath, cu.toString().getBytes(StandardCharsets.UTF_8));
	}
}
