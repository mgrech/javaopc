package dev.mgrech.javaopc;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.types.ResolvedType;

public class OperatorDefinitionCheckingVisitor extends VoidVisitorAdapter<Void>
{
	@Override
	public void visit(MethodDeclaration decl, Void arg)
	{
		var classDecl = decl.findAncestor(ClassOrInterfaceDeclaration.class).get();
		var className = classDecl.getNameAsString();
		var methodName = decl.getNameAsString();
		var op = Operators.lookup(methodName);

		if(op == null)
			return;

		if(op.mustBeStatic && !decl.isStatic())
			CompileErrors.operatorMethodNotStatic(className, methodName);

		var paramCount = decl.getParameters().size();

		if(paramCount < op.minArity || (op.maxArity != -1 && paramCount > op.maxArity))
			CompileErrors.operatorMethodInvalidParamCount(className, methodName, op.minArity, op.maxArity, paramCount);

		if(methodName.equals(Operators.COMPARISON))
		{
			ResolvedType implementedType = null;

			for(var type : classDecl.getImplementedTypes())
			{
				var paramType = Types.isComparable(type.resolve());

				if(paramType != null)
				{
					implementedType = paramType;
					break;
				}
			}

			var argType = decl.getParameter(0).getType().resolve();

			if(!argType.equals(implementedType))
				CompileErrors.comparableTypeShouldImplementComparable(className, argType.describe());
		}
	}
}
