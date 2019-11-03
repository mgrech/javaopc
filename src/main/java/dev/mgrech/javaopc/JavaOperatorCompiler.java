package dev.mgrech.javaopc;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseStart;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Provider;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;

public class JavaOperatorCompiler
{
	public static CompilationUnit parse(Provider input, TypeSolver solver)
	{
		var config = new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_12)
		                                      .setSymbolResolver(new JavaSymbolSolver(solver));

		config.getPostProcessors().add(new JavaOperatorCompilerPostProcessor());

		var parser = new JavaParser(config);
		var parse = parser.parse(ParseStart.COMPILATION_UNIT, input);

		if(!parse.isSuccessful())
		{
			for(var problem : parse.getProblems())
				System.err.println(problem);

			throw new RuntimeException("failed to compile file");
		}

		var cu = parse.getResult().orElse(null);
		assert cu != null;
		return cu;
	}
}
