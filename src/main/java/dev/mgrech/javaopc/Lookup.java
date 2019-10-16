package dev.mgrech.javaopc;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Lookup
{
	public static ResolvedType resolveType(Expression location, Expression expr)
	{
		location.replace(expr);

		try
		{
			return expr.calculateResolvedType();
		}
		catch(RuntimeException ex)
		{
			return null;
		}
		finally
		{
			expr.replace(location);
		}
	}

	public static ResolvedMethodDeclaration resolveMethodInvocationInplace(MethodCallExpr invocation)
	{
		assert invocation.findAncestor(CompilationUnit.class).isPresent();

		try
		{
			return invocation.resolve();
		}
		catch(RuntimeException ex)
		{
			return null;
		}
	}

	private static ResolvedMethodDeclaration resolveMethodInvocationAtLocation(Expression location, MethodCallExpr invocation)
	{
		// resolution requires that the node be inserted in a compilation unit
		// use 'location' as a temporary location for the node
		location.replace(invocation);

		var result = resolveMethodInvocationInplace(invocation);

		// restore the previous node at the given location
		invocation.replace(location);

		return result;
	}

	private enum InvocationType
	{
		STRICT,
		WEAK,
		VARARGS,
	}

	private static InvocationType classify(Expression expr, MethodCallExpr invocation)
	{
		var decl = resolveMethodInvocationAtLocation(expr, invocation);
		assert decl != null;

		if(decl.hasVariadicParameter())
			return InvocationType.VARARGS;

		var paramTypes = IntStream.rangeClosed(0, decl.getNumberOfParams() - 1)
		                          .mapToObj(decl::getParam)
		                          .map(ResolvedValueDeclaration::getType)
		                          .collect(Collectors.toList());

		var argTypes = invocation.getArguments()
		                         .stream()
		                         .map(e -> resolveType(expr, e))
		                         .collect(Collectors.toList());

		for(var i = 0; i != paramTypes.size(); ++i)
			if(!paramTypes.get(i).equals(argTypes.get(i)))
				return InvocationType.WEAK;

		return InvocationType.STRICT;
	}

	private static MethodCallExpr disambiguate(Expression expr, List<MethodCallExpr> candidates)
	{
		if(candidates.isEmpty())
			return null;

		var classification = new EnumMap<InvocationType, List<MethodCallExpr>>(InvocationType.class);

		for(var type : InvocationType.values())
			classification.put(type, new ArrayList<>());

		for(var candidate : candidates)
			classification.get(classify(expr, candidate)).add(candidate);

		for(var type : InvocationType.values())
		{
			var candidatesForType = classification.get(type);

			if(candidatesForType.size() == 1)
				return candidatesForType.get(0);

			if(!candidatesForType.isEmpty())
				return CompileErrors.ambiguousMethodCall();
		}

		throw new AssertionError("unreachable");
	}

	public static MethodCallExpr resolveOverloadedOperator(Expression expr, String opMethodName,
	                                                       List<Expression> args, List<ResolvedType> primaryTypes)
	{
		var typesDeduplicated = new HashSet<>(primaryTypes);
		var candidates = new ArrayList<MethodCallExpr>();

		for(var type : typesDeduplicated)
		{
			if(!type.isReferenceType())
				continue;

			var className = new NameExpr(type.asReferenceType().getTypeDeclaration().getName());
			var invocation = new MethodCallExpr(className, opMethodName, NodeList.nodeList(args));

			if(resolveMethodInvocationAtLocation(expr, invocation) != null)
				candidates.add(invocation);
		}

		var unqualified = new MethodCallExpr(opMethodName, args.toArray(new Expression[0]));

		if(resolveMethodInvocationAtLocation(expr, unqualified) != null)
			candidates.add(unqualified);

		return disambiguate(expr, candidates);
	}
}
