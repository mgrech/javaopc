package dev.mgrech.javaopc;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.VarType;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class OperatorVisitor implements ExprRewritingVisitor
{
	private int tempVarCounter = 0;

	private Expression rewriteCompoundAssignment(AssignExpr expr)
	{
		var binaryOp = expr.getOperator().toBinaryOperator().orElse(null);
		assert binaryOp != null;

		var leftType = expr.getTarget().calculateResolvedType();
		var rightType = expr.getValue().calculateResolvedType();

		var binaryExpr = new BinaryExpr(expr.getTarget(), expr.getValue(), binaryOp);
		var assignExpr = new AssignExpr(expr.getTarget(), binaryExpr, AssignExpr.Operator.ASSIGN);

		expr.replace(assignExpr);
		var finalBinaryExpr = rewriteBinaryOperator(binaryExpr, leftType, rightType);
		assignExpr.replace(expr);

		if(finalBinaryExpr == null)
			return null;

		assignExpr.replace(binaryExpr, finalBinaryExpr);
		return assignExpr;
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

	private Expression rewritePreAnycrement(UnaryExpr expr, ResolvedReferenceType argType)
	{
		var methodName = Operators.mapToMethodName(expr.getOperator());
		var args = NodeList.nodeList(expr.getExpression());

		var invocation = resolveOperatorInvocation(expr, methodName, args, List.of(argType));

		if(invocation == null)
			return null;

		return new AssignExpr(expr.getExpression(), invocation, AssignExpr.Operator.ASSIGN);
	}

	private void insertSiblingStmtBefore(Statement location, Statement insert)
	{
		var parent = location.findAncestor(Statement.class).orElse(null);

		if(!(parent instanceof BlockStmt))
		{
			assert parent != null;

			var block = new BlockStmt();
			block.addStatement(insert);
			block.addStatement(location);
			parent.replace(block);
		}
		else
		{
			var block = (BlockStmt)parent;
			var index = block.getStatements().indexOf(location);
			block.addStatement(index, insert);
		}
	}

	private Statement enclosingStmt(Expression expr)
	{
		var stmt = expr.findAncestor(Statement.class).orElse(null);
		assert stmt != null;
		return stmt;
	}

	private Expression hoistVar(Statement location, Expression expr, String tag)
	{
		var name = String.format("__temp%s__%s__", tempVarCounter++, tag);
		var decl = new VariableDeclarator(new VarType(), name, expr.clone());
		insertSiblingStmtBefore(location, new ExpressionStmt(new VariableDeclarationExpr(decl)));
		return new NameExpr(name);
	}

	private void removeSiblingStmtBefore(Statement location)
	{
		var block = (BlockStmt)location.getParentNode().orElse(null);
		assert block != null;
		var index = block.getStatements().indexOf(location) - 1;
		block.getStatements().remove(index);
	}

	private Expression rewritePostAnycrement(UnaryExpr expr, ResolvedReferenceType argType)
	{
		var stmt = enclosingStmt(expr);
		var oldExpr = hoistVar(stmt, expr.getExpression(), "old");

		var methodName = Operators.mapToMethodName(expr.getOperator());
		var args = List.of(expr.getExpression());
		var invocation = resolveOperatorInvocation(expr, methodName, args, List.of(argType));

		if(invocation == null)
		{
			// remove the hoisted var again if we don't do anything with it
			removeSiblingStmtBefore(stmt);
			return null;
		}

		var assignment = new AssignExpr(expr.getExpression(), invocation, AssignExpr.Operator.ASSIGN);
		insertSiblingStmtBefore(stmt, new ExpressionStmt(assignment));
		return oldExpr;
	}

	private Expression rewriteUnaryOperator(UnaryExpr expr, ResolvedType argType)
	{
		switch(expr.getOperator())
		{
		case LOGICAL_COMPLEMENT:
			// not overloadable: '!'
			return null;

		case PLUS:
		case MINUS:
		case BITWISE_COMPLEMENT:
			var methodName = Operators.mapToMethodName(expr.getOperator());
			var args = List.of(expr.getExpression());
			var argTypes = List.of(argType);
			var invocation = resolveOperatorInvocation(expr, methodName, args, argTypes);

			if(invocation == null)
				CompileErrors.noApplicableMethodFound(methodName);

			return invocation;

		case PREFIX_INCREMENT:
		case PREFIX_DECREMENT:
			{
				var type = expr.getExpression().calculateResolvedType();

				if(Types.isBuiltinType(type))
					return null;

				return rewritePreAnycrement(expr, type.asReferenceType());
			}

		case POSTFIX_INCREMENT:
		case POSTFIX_DECREMENT:
			{
				var type = expr.getExpression().calculateResolvedType();

				if(Types.isBuiltinType(type))
					return null;

				return rewritePostAnycrement(expr, type.asReferenceType());
			}

		default: throw new AssertionError("unknown unary operator: " + expr.getOperator());
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

	private Expression rewriteComparison(BinaryExpr expr, ResolvedType leftType, ResolvedType rightType)
	{
		var leftComparable = Types.implementsComparable(leftType);
		var rightComparable = Types.implementsComparable(rightType);

		if(leftComparable == null && rightComparable == null)
			return null;

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

		var methodName = Operators.mapToMethodName(expr.getOperator());
		assert methodName != null;
		var invocation = new MethodCallExpr(left, methodName, NodeList.nodeList(right));
		return new BinaryExpr(invocation, new IntegerLiteralExpr(0), op);
	}

	private Expression rewriteBinaryOperator(BinaryExpr expr, ResolvedType leftType, ResolvedType rightType)
	{
		switch(expr.getOperator())
		{
		case AND:
		case OR:
		case EQUALS:
		case NOT_EQUALS:
			// not overloadable: '&&', '||', '==', '!='
			return null;

		case LESS:
		case LESS_EQUALS:
		case GREATER:
		case GREATER_EQUALS:
			return rewriteComparison(expr, leftType, rightType);

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
				var methodName = Operators.mapToMethodName(expr.getOperator());
				var args = List.of(expr.getLeft(), expr.getRight());
				var argTypes = List.of(leftType, rightType);
				var invocation = resolveOperatorInvocation(expr, methodName, args, argTypes);

				if(invocation == null)
					CompileErrors.noApplicableMethodFound(methodName);

				return invocation;
			}

		default: throw new AssertionError("unknown binary operator: " + expr.getOperator());
		}
	}

	private Expression rewriteMethodInvocation(MethodCallExpr expr)
	{
		var nameExpr = new NameExpr(expr.getName());
		var type = typeof(expr, nameExpr);

		if(type == null)
			return null;

		if(Types.isFunctionalInterface(type))
		{
			// obj.method(args...)

			var methods = type.asReferenceType().getTypeDeclaration().getDeclaredMethods();
			var abstractMethods = methods.stream()
			                    .filter(ResolvedMethodDeclaration::isAbstract)
			                    .collect(Collectors.toList());

			assert abstractMethods.size() == 1;

			var method = abstractMethods.get(0);
			return new MethodCallExpr(nameExpr, method.getName(), expr.getArguments());
		}
		else // T.opInvoke(obj, args...)
		{
			var typeName = type.asReferenceType().getTypeDeclaration().getName();
			var allArgs = expr.getArguments();
			allArgs.add(0, nameExpr);
			var invocation = new MethodCallExpr(new NameExpr(typeName), Operators.INVOCATION, allArgs);

			if(isValidInvocation(expr, invocation))
				return invocation;

			return null;
		}
	}

	private Expression rewriteArrayAccessToSubscriptGet(ArrayAccessExpr expr, ResolvedReferenceType subscriptedType)
	{
		var name = subscriptedType.getTypeDeclaration().getName();
		var args = NodeList.nodeList(expr.getName(), expr.getIndex());
		var invocation = new MethodCallExpr(new NameExpr(name), Operators.SUBSCRIPT_GET, args);

		if(isValidInvocation(expr, invocation))
			return invocation;

		return null;
	}

	private Expression rewriteArrayAccessToSubscriptSet(AssignExpr expr)
	{
		var arrayAccessExpr = (ArrayAccessExpr)expr.getTarget();

		var op = expr.getOperator();
		var name = arrayAccessExpr.calculateResolvedType().asReferenceType().getTypeDeclaration().getName();
		var assignedValue = expr.getValue();

		// if we have a[i] @= v, rewrite to
		// T.opSubscriptSet(a, i, T.opSubscriptGet(a, i) @ v)
		if(op != AssignExpr.Operator.ASSIGN)
		{
			var binop = op.toBinaryOperator().orElse(null);
			assert binop != null;
			var arrayGetArgs = NodeList.nodeList(arrayAccessExpr.getName(), arrayAccessExpr.getIndex());
			var arrayGetExpr = new MethodCallExpr(new NameExpr(name), Operators.SUBSCRIPT_GET, arrayGetArgs);
			assignedValue = new BinaryExpr(arrayGetExpr, assignedValue, binop);
		}

		var args = NodeList.nodeList(arrayAccessExpr.getName(), arrayAccessExpr.getIndex(), assignedValue);
		var invocation = new MethodCallExpr(new NameExpr(name), Operators.SUBSCRIPT_SET, args);

		if(isValidInvocation(expr, invocation))
			return invocation;

		return null;
	}

	private Expression rewriteImplicitConversion(Expression expr, ResolvedType sourceType, ResolvedType targetType)
	{
		var args = List.of(expr);
		var argTypes = List.of(sourceType, targetType);
		var parent = expr.findAncestor(Expression.class).orElse(null);
		assert parent != null;
		return resolveOperatorInvocation(parent, Operators.CONVERSION, args, argTypes);
	}

	@Override
	public Expression visit(AssignExpr expr)
	{
		var leftType = expr.getTarget().calculateResolvedType();
		var rightType = expr.getValue().calculateResolvedType();

		// if we have an array access on the left side, we need to generate opSubscriptSet
		// also rewrites compound assignment if necessary
		if(expr.getTarget() instanceof ArrayAccessExpr && !Types.isBuiltinType(leftType))
			return rewriteArrayAccessToSubscriptSet(expr);

		if(expr.getOperator() == AssignExpr.Operator.ASSIGN)
		{
			// if the types match, we don't need to do anything ('=' is not overloadable)
			if(leftType == rightType)
				return null;

			// if the types don't match and at least one is user-defined,
			// there might be an implicit conversion to apply
			if(!Types.isBuiltinType(leftType) || !Types.isBuiltinType(rightType))
			{
				var conv = rewriteImplicitConversion(expr.getValue(), rightType, leftType);

				if(conv != null)
					return new AssignExpr(expr.getTarget(), conv, AssignExpr.Operator.ASSIGN);
			}

			return null;
		}

		// at least one argument must have user-defined type
		if(Types.isBuiltinType(leftType) && Types.isBuiltinType(rightType))
			return null;

		// we don't rewrite += if one argument is a String
		if(expr.getOperator() == AssignExpr.Operator.PLUS)
		{
			if(Types.isJavaLangString(leftType) || Types.isJavaLangString(rightType))
					return null;
		}

		return rewriteCompoundAssignment(expr);
	}

	@Override
	public Expression visit(UnaryExpr expr)
	{
		var argType = expr.getExpression().calculateResolvedType();

		if(!argType.isPrimitive())
			return rewriteUnaryOperator(expr, argType);

		return null;
	}

	@Override
	public Expression visit(BinaryExpr expr)
	{
		var leftType = expr.getLeft().calculateResolvedType();
		var rightType = expr.getRight().calculateResolvedType();

		// we're interested in all binary expressions where:
		// 1. at least one argument is a user-defined type, and
		// 2. if the operator is '+', neither of the arguments are a String (otherwise we have a string concat)
		if(!Types.isBuiltinType(leftType) || !Types.isBuiltinType(rightType))
		{
			if(expr.getOperator() == BinaryExpr.Operator.PLUS)
			{
				if(!Types.isJavaLangString(leftType) && !Types.isJavaLangString(rightType))
					return rewriteBinaryOperator(expr, leftType, rightType);
			}
			else
				return rewriteBinaryOperator(expr, leftType, rightType);
		}

		return null;
	}

	@Override
	public Expression visit(MethodCallExpr expr)
	{
		// if the invocation is already valid, it's not an overloaded invocation
		if(isValidInvocation(expr))
			return null;

		return rewriteMethodInvocation(expr);
	}

	@Override
	public Expression visit(ArrayAccessExpr expr)
	{
		var parent = expr.getParentNode().orElse(null);

		// if we hit a subscript with assignment, do nothing (rewriting happens in 'visit(AssignExpr)')
		if(parent instanceof AssignExpr)
			return null;

		var leftType = expr.getName().calculateResolvedType();

		// overloaded subscript only for user-defined types
		if(Types.isBuiltinType(leftType))
			return null;

		return rewriteArrayAccessToSubscriptGet(expr, leftType.asReferenceType());
	}

	@Override
	public Expression visit(CastExpr expr)
	{
		var sourceType = expr.getExpression().calculateResolvedType();
		var targetType = expr.getType().resolve();

		if(!Types.isBuiltinType(sourceType) || !Types.isBuiltinType(targetType))
		{
			var conv = rewriteImplicitConversion(expr.getExpression(), sourceType, targetType);

			if(conv != null)
				return new CastExpr(expr.getType(), conv);
		}

		return null;
	}

	@Override
	public Expression visit(VariableDeclarationExpr expr)
	{
		for(var variable : expr.getVariables())
		{
			var init = variable.getInitializer().orElse(null);

			// skip if the var is declared with 'var', has no initializer or is initialized with a lambda
			if(variable.getType().isUnknownType() || init == null || init instanceof LambdaExpr)
				continue;

			var varType = variable.getType().resolve();
			var initType = variable.getInitializer().map(Expression::calculateResolvedType).orElse(null);
			assert initType != null;

			// skip if we have a 'null' initializer
			if(initType.isNull())
				continue;

			// skip if the types match or not at least one of the types is user-defined
			if(varType == initType || Types.isBuiltinType(varType) && Types.isBuiltinType(initType))
				continue;

			var conv = rewriteImplicitConversion(init, initType, varType);

			if(conv != null)
				variable.setInitializer(conv);
		}

		return null;
	}
}
