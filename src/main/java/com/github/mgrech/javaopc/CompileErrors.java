package com.github.mgrech.javaopc;

public class CompileErrors
{
	public static <T> T noApplicableMethodFound()
	{
		throw new RuntimeException("no applicable method found");
	}

	public static <T> T ambiguousMethodCall()
	{
		throw new RuntimeException("ambiguous method call");
	}
}
