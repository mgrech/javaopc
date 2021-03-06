package dev.mgrech.javaopc;

public class CompileErrors
{
	public static <T> T noApplicableMethodFound(String name)
	{
		throw new RuntimeException(String.format("no applicable method found: %s", name));
	}

	public static <T> T ambiguousMethodCall()
	{
		throw new RuntimeException("ambiguous method call");
	}

	public static <T> T operatorMethodNotStatic(String className, String methodName)
	{
		throw new RuntimeException(String.format("operator method should be static: %s in %s", methodName, className));
	}

	public static <T> T operatorMethodInvalidParamCount(String className, String methodName, int minExpected, int maxExpected, int actual)
	{
		var message = maxExpected == -1
			? "operator method should have at least %s parameters, got %s: %s in %s"
			: "operator method should have between %s and %s parameters, got %s: %s in %s";

		if(maxExpected == -1)
			throw new RuntimeException(String.format(message, minExpected, actual, methodName, className));
		else
			throw new RuntimeException(String.format(message, minExpected, maxExpected, actual, methodName, className));
	}

	public static <T> T comparableTypeShouldImplementComparable(String className, String expectedClass)
	{
		throw new RuntimeException(String.format("type overloads comparison for %s, but does not implement Comparable<%s>: %s", expectedClass, expectedClass, className));
	}

	public static <T> T operandToOpSumMustNotBeString(String className)
	{
		throw new RuntimeException(String.format("type overloads '+' and operand has disallowed parameter of type String: %s", className));
	}

	public static <T> T allParamsBuiltIn(String className, String methodName)
	{
		throw new RuntimeException(String.format("overloaded operator has no parameters with user-defined type: %s in %s", methodName, className));
	}
}
