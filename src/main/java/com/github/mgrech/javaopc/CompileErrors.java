package com.github.mgrech.javaopc;

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
}
