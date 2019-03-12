package com.github.mgrech.javaopc;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.TreeVisitor;
import com.github.javaparser.resolution.SymbolResolver;
import com.github.javaparser.resolution.types.ResolvedType;

public class OperatorVisitor extends TreeVisitor
{
	private static final String JAVA_LANG_STRING = "java.lang.String";

	private static boolean isJavaLangString(ResolvedType type)
	{
		return type.isReferenceType() && type.asReferenceType().getQualifiedName().equals(JAVA_LANG_STRING);
	}

	private static String operatorToMethodName(BinaryExpr.Operator operator)
	{
		switch(operator)
		{
		// arithmetic operators
		case PLUS:      return "opAdd";
		case MINUS:     return "opSubtract";
		case MULTIPLY:  return "opMultiply";
		case DIVIDE:    return "opDivide";
		case REMAINDER: return "opRemainder";

		// bitwise operators
		case BINARY_AND:           return "opAnd";
		case BINARY_OR:            return "opOr";
		case XOR:                  return "opXor";
		case LEFT_SHIFT:           return "opShiftLeft";
		case SIGNED_RIGHT_SHIFT:   return "opShiftRightSigned";
		case UNSIGNED_RIGHT_SHIFT: return "opShiftRightUnsigned";

		default: break;
		}

		throw new AssertionError("unreachable");
	}

	private final SymbolResolver resolver;

	public OperatorVisitor(SymbolResolver resolver)
	{
		this.resolver = resolver;
	}

	// rewrite 'a @= b' to 'a = (a @ b)'
	private void visit(AssignExpr expr, ResolvedType leftType, ResolvedType rightType)
	{
		var tokenRange = expr.getTokenRange().get();
		var binaryOp = expr.getOperator().toBinaryOperator().get();
		var binaryExpr = new BinaryExpr(tokenRange, expr.getTarget(), expr.getValue(), binaryOp);
		visit(binaryExpr, leftType, rightType);
		expr.replace(new AssignExpr(tokenRange, expr.getTarget(), binaryExpr, AssignExpr.Operator.ASSIGN));
	}

	private void visit(BinaryExpr expr, ResolvedType leftType, ResolvedType rightType)
	{
		if(expr.getOperator() == BinaryExpr.Operator.LESS
		|| expr.getOperator() == BinaryExpr.Operator.LESS_EQUALS
		|| expr.getOperator() == BinaryExpr.Operator.GREATER
		|| expr.getOperator() == BinaryExpr.Operator.GREATER_EQUALS)
		{
			throw new AssertionError("nyi");
		}
		else // rewrite 'a @ b' to 'a.opName(b)'
		{
			var methodName = operatorToMethodName(expr.getOperator());
			expr.replace(new MethodCallExpr(expr.getLeft(), methodName, NodeList.nodeList(expr.getRight())));
		}
	}

	@Override
	public void process(Node node)
	{
		if(node instanceof AssignExpr)
		{
			var opnode = (AssignExpr)node;
			var leftType = resolver.calculateType(opnode.getTarget());
			var rightType = resolver.calculateType(opnode.getValue());

			// we're interested in all compound assignment operators
			if(opnode.getOperator() != AssignExpr.Operator.ASSIGN)
				visit(opnode, leftType, rightType);
		}
		else if(node instanceof BinaryExpr)
		{
			var opnode = (BinaryExpr)node;
			var leftType = resolver.calculateType(opnode.getLeft());
			var rightType = resolver.calculateType(opnode.getRight());

			// we're interested in all binary expressions where:
			// 1. at least one argument is a reference type, and
			// 2. if the operator is '+', neither of the arguments are a String
			// (otherwise '+' represents string concat)
			if(leftType.isReferenceType() || rightType.isReferenceType())
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
