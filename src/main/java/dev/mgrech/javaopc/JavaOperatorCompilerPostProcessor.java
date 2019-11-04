package dev.mgrech.javaopc;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

public class JavaOperatorCompilerPostProcessor implements ParseResult.PostProcessor
{
	private static final boolean BENCH = System.getenv("JAVAOPC_BENCH") != null;

	@Override
	public void process(ParseResult<? extends Node> result, ParserConfiguration configuration)
	{
		if(!result.isSuccessful())
			return;

		assert result.getResult().isPresent();

		if(BENCH)
		{
			var className = result.getResult().get().findFirst(ClassOrInterfaceDeclaration.class).get().getNameAsString();
			System.out.print("processing class " + className + " ... ");
		}

		var start = System.currentTimeMillis();

		var cu = (CompilationUnit)result.getResult().get();
		cu.accept(new OperatorDefinitionCheckingVisitor(), null);
		cu.accept(new ExprRewritingVisitorAdapter(new OperatorVisitor()), null);

		if(BENCH)
		{
			var end = System.currentTimeMillis();
			var sec = ((double)(end - start)) / 1000;
			System.out.println(sec + " s");
		}
	}
}
