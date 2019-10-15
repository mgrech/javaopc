package dev.mgrech.javaopc;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.UnaryExpr;

import java.util.HashMap;
import java.util.Map;

public class Operators
{
	public static final String INVOCATION = "opInvoke";
	public static final String SUBSCRIPT_GET = "opSubscriptGet";
	public static final String SUBSCRIPT_SET = "opSubscriptSet";
	public static final String CONVERSION = "opConvert";
	public static final String COMPARISON = "compareTo";

	public static class OperatorInfo
	{
		public final String methodName;
		public final int minArity;
		public final int maxArity;
		public final boolean mustBeStatic;

		public OperatorInfo(String methodName, int minArity, int maxArity, boolean mustBeStatic)
		{
			this.methodName = methodName;
			this.minArity = minArity;
			this.maxArity = maxArity;
			this.mustBeStatic = mustBeStatic;
		}
	}

	private static final Map<String, OperatorInfo> OPERATORS_BY_NAME = new HashMap<>();
	private static final Map<Object, OperatorInfo> OPERATORS_BY_NODE = new HashMap<>();

	private static void defineUnaryOperator(UnaryExpr.Operator op, String name)
	{
		var info = new OperatorInfo(name, 1, 1, true);
		OPERATORS_BY_NAME.put(name, info);
		OPERATORS_BY_NODE.put(op, info);
	}

	private static void defineBinaryOperator(BinaryExpr.Operator op, String name)
	{
		var info = new OperatorInfo(name, 2, 2, true);
		OPERATORS_BY_NAME.put(name, info);
		OPERATORS_BY_NODE.put(op, info);
	}

	static
	{
		defineUnaryOperator(UnaryExpr.Operator.PLUS, "opNeutral");
		defineUnaryOperator(UnaryExpr.Operator.MINUS, "opNegate");
		defineUnaryOperator(UnaryExpr.Operator.BITWISE_COMPLEMENT, "opBitNot");

		var incr = new OperatorInfo("opIncrement", 1, 1, true);
		OPERATORS_BY_NAME.put(incr.methodName, incr);
		OPERATORS_BY_NODE.put(UnaryExpr.Operator.PREFIX_INCREMENT, incr);
		OPERATORS_BY_NODE.put(UnaryExpr.Operator.POSTFIX_INCREMENT, incr);

		var decr = new OperatorInfo("opDecrement", 1, 1, true);
		OPERATORS_BY_NAME.put(decr.methodName, decr);
		OPERATORS_BY_NODE.put(UnaryExpr.Operator.PREFIX_DECREMENT, decr);
		OPERATORS_BY_NODE.put(UnaryExpr.Operator.POSTFIX_DECREMENT, decr);

		defineBinaryOperator(BinaryExpr.Operator.PLUS, "opSum");
		defineBinaryOperator(BinaryExpr.Operator.MINUS, "opDifference");
		defineBinaryOperator(BinaryExpr.Operator.MULTIPLY, "opProduct");
		defineBinaryOperator(BinaryExpr.Operator.DIVIDE, "opQuotient");
		defineBinaryOperator(BinaryExpr.Operator.REMAINDER, "opRemainder");
		defineBinaryOperator(BinaryExpr.Operator.BINARY_AND, "opBitAnd");
		defineBinaryOperator(BinaryExpr.Operator.BINARY_OR, "opBitOr");
		defineBinaryOperator(BinaryExpr.Operator.XOR, "opBitXor");
		defineBinaryOperator(BinaryExpr.Operator.LEFT_SHIFT, "opShiftLeft");
		defineBinaryOperator(BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT, "opShiftRight");
		defineBinaryOperator(BinaryExpr.Operator.SIGNED_RIGHT_SHIFT, "opShiftRightArithmetic");

		var compareTo = new OperatorInfo(COMPARISON, 1, 1, false);
		OPERATORS_BY_NAME.put(compareTo.methodName, compareTo);
		OPERATORS_BY_NODE.put(BinaryExpr.Operator.LESS, compareTo);
		OPERATORS_BY_NODE.put(BinaryExpr.Operator.LESS_EQUALS, compareTo);
		OPERATORS_BY_NODE.put(BinaryExpr.Operator.GREATER, compareTo);
		OPERATORS_BY_NODE.put(BinaryExpr.Operator.GREATER_EQUALS, compareTo);

		OPERATORS_BY_NAME.put(CONVERSION, new OperatorInfo(CONVERSION, 1, 1, true));
		OPERATORS_BY_NAME.put(SUBSCRIPT_GET, new OperatorInfo(SUBSCRIPT_GET, 2, 2, true));
		OPERATORS_BY_NAME.put(SUBSCRIPT_SET, new OperatorInfo(SUBSCRIPT_SET, 3, 3, true));
		OPERATORS_BY_NAME.put(INVOCATION, new OperatorInfo(INVOCATION, 1, -1, true));
	}

	public static OperatorInfo lookup(String name)
	{
		return OPERATORS_BY_NAME.get(name);
	}

	public static String mapToMethodName(UnaryExpr.Operator op)
	{
		var info = OPERATORS_BY_NODE.get(op);
		assert info != null;
		return info.methodName;
	}

	public static String mapToMethodName(BinaryExpr.Operator op)
	{
		var info = OPERATORS_BY_NODE.get(op);
		assert info != null;
		return info.methodName;
	}

	public static BinaryExpr.Operator flip(BinaryExpr.Operator op)
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
}
