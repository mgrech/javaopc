package com.github.mgrech.javaopc.test;

import com.github.javaparser.Providers;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.mgrech.javaopc.JavaOperatorCompiler;
import org.junit.Assert;
import org.junit.Test;
import org.mdkt.compiler.InMemoryJavaCompiler;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class Driver
{
	private static final InMemoryJavaCompiler COMPILER = InMemoryJavaCompiler.newInstance();

	private static void runTest(String name) throws Exception
	{
		var input = Driver.class.getResourceAsStream(String.format("/tests/%s/Program.java.op", name));
		var compilationUnit = JavaOperatorCompiler.process(Providers.provider(input), new ReflectionTypeSolver(false));
		compilationUnit.setPackageDeclaration(String.format("tests.%s", name));

		var program = COMPILER.compile(String.format("tests.%s.Program", name), compilationUnit.toString());
		var main = program.getDeclaredMethod("main", String[].class);

		var oldOut = System.out;
		var baos = new ByteArrayOutputStream();
		System.setOut(new PrintStream(baos));

		try
		{
			main.invoke(null, new Object[]{new String[0]});
		}
		finally
		{
			System.setOut(oldOut);
		}

		var expectedOutput = Driver.class.getResourceAsStream(String.format("/tests/%s/expected.txt", name));
		Assert.assertArrayEquals(expectedOutput.readAllBytes(), baos.toByteArray());
	}

	@Test
	public void add() throws Exception
	{
		runTest("add");
	}

	@Test
	public void nested() throws Exception
	{
		runTest("nested");
	}
}
