package dev.mgrech.javaopc;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;

public class JavaOperatorCompilerPostProcessor implements ParseResult.PostProcessor
{
	@Override
	public void process(ParseResult<? extends Node> result, ParserConfiguration configuration)
	{
		if(!result.isSuccessful())
			return;

		assert result.getResult().isPresent();

		var cu = (CompilationUnit)result.getResult().get();
		cu.accept(new OperatorDefinitionCheckingVisitor(), null);
		cu.accept(new ExprRewritingVisitorAdapter(new OperatorVisitor()), null);
	}
}
