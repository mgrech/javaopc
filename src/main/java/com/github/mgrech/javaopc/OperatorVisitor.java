package com.github.mgrech.javaopc;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.TreeVisitor;
import com.github.javaparser.resolution.SymbolResolver;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class OperatorVisitor extends TreeVisitor
{
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

	private static ResolvedType typeof(Expression location, Expression expr)
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

	private static boolean isValidInvocation(MethodCallExpr invocation)
	{
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

	private BinaryExpr.Operator flip(BinaryExpr.Operator op)
	{
		switch(op)
		{
		case LESS:           return BinaryExpr.Operator.GREATER;
		case LESS_EQUALS:    return BinaryExpr.Operator.GREATER_EQUALS;
		case GREATER:        return BinaryExpr.Operator.LESS;
		case GREATER_EQUALS: return BinaryExpr.Operator.LESS_EQUALS;
		default: break;
		}

		throw new AssertionError("unreachable");
	}

	private void rewriteComparison(BinaryExpr expr, ResolvedType leftType, ResolvedType rightType)
	{
		var leftComparable = Types.implementsComparable(leftType);
		var rightComparable = Types.implementsComparable(rightType);

		if(leftComparable == null && rightComparable == null)
			return;

		var left = expr.getLeft();
		var right = expr.getRight();
		var op = expr.getOperator();

		// fallback: flip around if left operand does not support comparable
		if(leftComparable == null)
		{
			var tmp = left;
			left = right;
			right = tmp;
			op = flip(op);
		}

		var methodName = OperatorNames.mapToMethodName(expr.getOperator());
		var invocation = new MethodCallExpr(left, methodName, NodeList.nodeList(right));
		expr.replace(new BinaryExpr(invocation, new IntegerLiteralExpr(0), op));
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
			rewriteComparison(expr, leftType, rightType);
			break;

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
			{
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
	}

	private void visit(MethodCallExpr expr)
	{
		var nameExpr = new NameExpr(expr.getName());
		var type = typeof(expr, nameExpr);

		if(type == null)
			return;

		if(Types.isFunctionalInterface(type))
		{
			// obj.method(args...)

			var methods = type.asReferenceType().getTypeDeclaration().getDeclaredMethods();
			var abstractMethods = methods.stream()
			                    .filter(m -> m.isAbstract())
			                    .collect(Collectors.toList());

			assert abstractMethods.size() == 1;

			var method = abstractMethods.get(0);
			var invocation = new MethodCallExpr(nameExpr, method.getName(), expr.getArguments());
			expr.replace(invocation);
		}
		else // T.opInvoke(obj, args...)
		{
			var typeName = type.asReferenceType().getTypeDeclaration().getName();
			var allArgs = expr.getArguments();
			allArgs.add(0, nameExpr);
			var invocation = new MethodCallExpr(new NameExpr(typeName), OperatorNames.INVOCATION, allArgs);

			if(isValidInvocation(expr, invocation))
				expr.replace(invocation);
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
			if(!Types.isBuiltinType(leftType) || !Types.isBuiltinType(rightType))
			{
				if(opnode.getOperator() == BinaryExpr.Operator.PLUS)
				{
					if(!Types.isJavaLangString(leftType) && !Types.isJavaLangString(rightType))
						visit(opnode, leftType, rightType);
				}
				else
					visit(opnode, leftType, rightType);
			}
		}
		else if(node instanceof MethodCallExpr)
		{
			var opnode = (MethodCallExpr)node;

			if(!isValidInvocation(opnode))
				visit(opnode);
		}
	}
}
