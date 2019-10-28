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

	private static List<List<Expression>> generateConversionPermutations(Expression expr, List<Expression> args, boolean permitConversions)
	{
		var result = new ArrayList<List<Expression>>();

		// we generate all the permutations of applying conversions to all given arguments by iterating over
		// the respective integer range and using the bit pattern as a mask for which arguments to try converting
		// if we don't permit conversions, we only do the first iteration with the all-zeroes mask (i.e. range [0..1[)
		// each arg gets a 2-bit value: 0 = no conversion, 1 = unqualified conversion, 2 = qualified conversion, 3 = undefined
		var limit = permitConversions ? 1 << (2 * args.size()) : 1;

		// potential optimization: try conversions for arguments in advance,
		// then only iterate over the masks for which conversions are valid
		for(var i = 0; i != limit; ++i)
		{
			var convArgs = new ArrayList<Expression>();

			for(var j = 0; j != args.size(); ++j)
			{
				var arg = args.get(j);
				var argType = resolveType(expr, arg);

				// apply conversions according to mask
				switch((i >> (2 * j)) & 3)
				{
				case 0:
					convArgs.add(arg);
					break;

				case 1:
					convArgs.add(new MethodCallExpr(Operators.CONVERSION, arg));
					break;

				case 2:
					if(argType == null || !argType.isReferenceType())
						break;

					var classQualifier = new NameExpr(argType.asReferenceType().getTypeDeclaration().getName());
					convArgs.add(new MethodCallExpr(classQualifier, Operators.CONVERSION, NodeList.nodeList(arg)));
					break;

				case 3:
					break;
				}
			}

			if(convArgs.size() == args.size())
				result.add(convArgs);
		}

		return result;
	}

	public static MethodCallExpr resolveOverloadedOperator(Expression expr, String opMethodName,
	                                                       List<Expression> args, List<ResolvedType> primaryTypes,
	                                                       boolean permitConversions, boolean invokeStatic)
	{
		var candidates = new ArrayList<MethodCallExpr>();

		if(invokeStatic)
		{
			var perms = generateConversionPermutations(expr, args, permitConversions);

			// qualified invocation
			for(var type : new HashSet<>(primaryTypes)) // deduplicate type list
			{
				if(!type.isReferenceType())
					continue;

				var classQualifier = new NameExpr(type.asReferenceType().getTypeDeclaration().getName());

				for(var perm : perms)
				{
					var invocation = new MethodCallExpr(classQualifier, opMethodName, NodeList.nodeList(perm));

					if(resolveMethodInvocationAtLocation(expr, invocation) != null)
						candidates.add(invocation);
				}
			}

			// unqualified invocation
			for(var perm : perms)
			{
				var invocation = new MethodCallExpr(opMethodName, perm.toArray(new Expression[0]));

				if(resolveMethodInvocationAtLocation(expr, invocation) != null)
					candidates.add(invocation);
			}
		}
		else
		{
			var invokeArgs = args.subList(1, args.size());
			var perms = generateConversionPermutations(expr, invokeArgs, permitConversions);

			for(var perm : perms)
			{
				var invocation = new MethodCallExpr(args.get(0), opMethodName, NodeList.nodeList(perm));

				if(resolveMethodInvocationAtLocation(expr, invocation) != null)
					candidates.add(invocation);
			}
		}

		return disambiguate(expr, candidates);
	}
}
