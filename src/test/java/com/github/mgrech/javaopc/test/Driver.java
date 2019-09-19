package com.github.mgrech.javaopc.test;

import com.github.javaparser.Providers;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.mgrech.javaopc.JavaOperatorCompiler;
import org.junit.Assert;
import org.mdkt.compiler.InMemoryJavaCompiler;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class Driver
{
	private static final InMemoryJavaCompiler COMPILER = InMemoryJavaCompiler.newInstance();

	private static void runTest(String relativePath) throws Exception
	{
		var input = Driver.class.getResourceAsStream(String.format("/tests/%s/Program.java.op", relativePath));
		assert input != null : "test not found";

		var compilationUnit = JavaOperatorCompiler.process(Providers.provider(input), new ReflectionTypeSolver(false));
		assert compilationUnit != null : "invalid source file";

		System.out.println(compilationUnit);

		var packageName = String.format("tests.%s", relativePath.replaceAll("/", "."));
		var className = String.format("%s.Program", packageName);
		compilationUnit.setPackageDeclaration(packageName);

		var program = COMPILER.compile(className, compilationUnit.toString());
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

		var expectedOutput = Driver.class.getResourceAsStream(String.format("/tests/%s/expected.txt", relativePath));
		var expected = new String(expectedOutput.readAllBytes(), StandardCharsets.UTF_8);
		var actual = new String(baos.toByteArray(), StandardCharsets.UTF_8);
		Assert.assertEquals(expected, actual);
	}

	public static void runTest()
	{
		try
		{
			var callerFrame = new Exception().getStackTrace()[1];
			var testName = callerFrame.getMethodName();
			runTest(testName);
		}
		catch(RuntimeException ex)
		{
			throw ex;
		}
		catch(Exception ex)
		{
			throw new RuntimeException(ex);
		}
	}
}
