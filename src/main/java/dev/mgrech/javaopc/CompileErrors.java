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
}
