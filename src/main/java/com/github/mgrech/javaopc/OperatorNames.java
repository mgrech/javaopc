package com.github.mgrech.javaopc;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.UnaryExpr;

public class OperatorNames
{
	public static String mapToMethodName(UnaryExpr.Operator op)
	{
		switch(op)
		{
		case PLUS:  return "opNeutral";
		case MINUS: return "opNegate";
		case BITWISE_COMPLEMENT: return "opBitNot";

		case PREFIX_INCREMENT:
		case POSTFIX_INCREMENT:
			return "opIncrement";

		case PREFIX_DECREMENT:
		case POSTFIX_DECREMENT:
			return "opDecrement";

		default: break;
		}

		throw new AssertionError("unknown operator: " + op);
	}

	public static String mapToMethodName(BinaryExpr.Operator op)
	{
		switch(op)
		{
		case PLUS:      return "opSum";
		case MINUS:     return "opDifference";
		case MULTIPLY:  return "opProduct";
		case DIVIDE:    return "opQuotient";
		case REMAINDER: return "opRemainder";

		case BINARY_AND: return "opBitAnd";
		case BINARY_OR:  return "opBitOr";
		case XOR:        return "opBitXor";
		case LEFT_SHIFT:           return "opShiftLeft";
		case UNSIGNED_RIGHT_SHIFT: return "opShiftRight";
		case SIGNED_RIGHT_SHIFT:   return "opShiftRightArithmetic";

		case LESS:
		case LESS_EQUALS:
		case GREATER:
		case GREATER_EQUALS:
			return "compareTo";

		case EQUALS:
		case NOT_EQUALS:
			return null;

		default: break;
		}

		throw new AssertionError("unknown operator: " + op);
	}
}
