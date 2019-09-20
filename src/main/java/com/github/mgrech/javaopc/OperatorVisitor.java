package com.github.mgrech.javaopc;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.TreeVisitor;
import com.github.javaparser.resolution.SymbolResolver;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class OperatorVisitor extends TreeVisitor
{
	private static final String JAVA_LANG_STRING = "java.lang.String";

	private static boolean isJavaLangString(ResolvedType type)
	{
		return type.isReferenceType() && type.asReferenceType().getQualifiedName().equals(JAVA_LANG_STRING);
	}

	private static boolean isBuiltinType(ResolvedType type)
	{
		if(type.isPrimitive() || type.isArray())
			return true;

		if(!type.isReferenceType())
			return false;

		var decl = type.asReferenceType().getTypeDeclaration();

		if(!decl.getPackageName().equals("java.lang"))
			return false;

		var name = decl.getName();

		return name.equals("Boolean")
		    || name.equals("Byte")
		    || name.equals("Character")
		    || name.equals("Double")
		    || name.equals("Float")
		    || name.equals("Integer")
		    || name.equals("Long")
		    || name.equals("Short")
		    || name.equals("String");
	}

	private final SymbolResolver resolver;

	public OperatorVisitor(SymbolResolver resolver)
	{
		this.resolver = resolver;
	}

	private void rewriteCompoundAssignment(AssignExpr expr)
	{
		var binaryOp = expr.getOperator().toBinaryOperator().get();
		var binaryExpr = new BinaryExpr(expr.getTarget(), expr.getValue(), binaryOp);
		var assignExpr = new AssignExpr(expr.getTarget(), binaryExpr, AssignExpr.Operator.ASSIGN);
		expr.replace(assignExpr);
		process(binaryExpr);

		// undo rewriting if this was not an overloaded operator for cleaner code output
		if(assignExpr.getValue() == binaryExpr)
			assignExpr.replace(expr);
	}

	private static boolean isValidInvocation(Expression location, MethodCallExpr invocation)
	{
		// insert for lookup (requires scope)
		location.replace(invocation);

		try
		{
			// lookup method
			invocation.resolve();
			return true;
		}
		catch(UnsolvedSymbolException ex)
		{
			return false;
		}
		finally
		{
			// undo insertion
			invocation.replace(location);
		}
	}

	private MethodCallExpr resolveOperatorInvocation(Expression expr, String opMethodName, List<Expression> args, List<ResolvedType> argTypes)
	{
		var typesDeduplicated = new HashSet<>(argTypes);
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

	private void visit(UnaryExpr expr, ResolvedType argType)
	{
		switch(expr.getOperator())
		{
		case LOGICAL_COMPLEMENT:
			break;

		case PLUS:
		case MINUS:
		case BITWISE_COMPLEMENT:
			var methodName = OperatorNames.mapToMethodName(expr.getOperator());
			var args = List.of(expr.getExpression());
			var argTypes = List.of(argType);
			var invocation = resolveOperatorInvocation(expr, methodName, args, argTypes);

			if(invocation == null)
				CompileErrors.noApplicableMethodFound();

			expr.replace(invocation);
			break;

		case PREFIX_INCREMENT:
		case PREFIX_DECREMENT:
		case POSTFIX_INCREMENT:
		case POSTFIX_DECREMENT:
			throw new AssertionError("nyi");
		}
	}

	private void visit(BinaryExpr expr, ResolvedType leftType, ResolvedType rightType)
	{
		switch(expr.getOperator())
		{
		case AND:
		case OR:
		case EQUALS:
		case NOT_EQUALS:
			break;

		case LESS:
		case LESS_EQUALS:
		case GREATER:
		case GREATER_EQUALS:
			throw new AssertionError("nyi");

		case PLUS:
		case MINUS:
		case MULTIPLY:
		case DIVIDE:
		case REMAINDER:
		case BINARY_AND:
		case BINARY_OR:
		case XOR:
		case LEFT_SHIFT:
		case SIGNED_RIGHT_SHIFT:
		case UNSIGNED_RIGHT_SHIFT:
			var methodName = OperatorNames.mapToMethodName(expr.getOperator());
			var args = List.of(expr.getLeft(), expr.getRight());
			var argTypes = List.of(leftType, rightType);
			var invocation = resolveOperatorInvocation(expr, methodName, args, argTypes);

			if(invocation == null)
				CompileErrors.noApplicableMethodFound();

			expr.replace(invocation);
			break;
		}
	}

	@Override
	public void process(Node node)
	{
		if(node instanceof AssignExpr)
		{
			var opnode = (AssignExpr)node;

			// we're interested compound assignment operators only, not simple assignment
			if(opnode.getOperator() != AssignExpr.Operator.ASSIGN)
				rewriteCompoundAssignment(opnode);
		}
		else if(node instanceof UnaryExpr)
		{
			var opnode = (UnaryExpr)node;
			var argType = resolver.calculateType(opnode.getExpression());

			if(!argType.isPrimitive())
				visit(opnode, argType);
		}
		else if(node instanceof BinaryExpr)
		{
			var opnode = (BinaryExpr)node;
			var leftType = resolver.calculateType(opnode.getLeft());
			var rightType = resolver.calculateType(opnode.getRight());

			// we're interested in all binary expressions where:
			// 1. at least one argument is a user-defined type, and
			// 2. if the operator is '+', neither of the arguments are a String (otherwise we have a string concat)
			if(!isBuiltinType(leftType) || !isBuiltinType(rightType))
			{
				if(opnode.getOperator() == BinaryExpr.Operator.PLUS)
				{
					if(!isJavaLangString(leftType) && !isJavaLangString(rightType))
						visit(opnode, leftType, rightType);
				}
				else
					visit(opnode, leftType, rightType);
			}
		}
	}
}
