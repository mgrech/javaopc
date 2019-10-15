package dev.mgrech.javaopc;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Lookup
{
	public static boolean isValidInvocation(MethodCallExpr invocation)
	{
		assert invocation.findAncestor(CompilationUnit.class).isPresent();

		try
		{
			invocation.resolve();
			return true;
		}
		catch(RuntimeException ex)
		{
			return false;
		}
	}

	private static boolean isValidInvocation(Expression location, MethodCallExpr invocation)
	{
		// insert for lookup (requires scope)
		location.replace(invocation);

		// lookup method
		var valid = isValidInvocation(invocation);

		// undo insertion
		invocation.replace(location);

		return valid;
	}

	public static MethodCallExpr resolveOverloadedOperator(Expression expr, String opMethodName, List<Expression> args, List<ResolvedType> primaryTypes)
	{
		var typesDeduplicated = new HashSet<>(primaryTypes);
		var candidates = new ArrayList<MethodCallExpr>();

		for(var type : typesDeduplicated)
		{
			if(!type.isReferenceType())
				continue;

			var className = new NameExpr(type.asReferenceType().getTypeDeclaration().getName());
			var invocation = new MethodCallExpr(className, opMethodName, NodeList.nodeList(args));

			if(isValidInvocation(expr, invocation))
				candidates.add(invocation);
		}

		var unqualified = new MethodCallExpr(opMethodName, args.toArray(new Expression[0]));

		if(isValidInvocation(expr, unqualified))
			candidates.add(unqualified);

		switch(candidates.size())
		{
		case 0: return null;
		case 1: return candidates.get(0);
		default: break;
		}

		return CompileErrors.ambiguousMethodCall();
	}
}
