package dev.mgrech.javaopc.test;

import com.github.javaparser.Providers;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import dev.mgrech.javaopc.JavaOperatorCompiler;
import org.junit.Assert;
import org.mdkt.compiler.CompilationException;
import org.mdkt.compiler.InMemoryJavaCompiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class Driver
{
	private static void runTest(String testName) throws Exception
	{
		var sourceDir = new File(Driver.class.getResource("/tests").toURI()).toPath();
		var sourceFile = sourceDir.resolve(String.format("%s/Program.java", testName));

		var source = Files.readString(sourceFile);
		var cu = JavaOperatorCompiler.parse(Providers.provider(source), new ReflectionTypeSolver(false));

		if(cu == null)
			throw new RuntimeException("failed to process source:\n" + source);

		var compiler = InMemoryJavaCompiler.newInstance();
		Class<?> program = null;

		try
		{
			program = compiler.compile("Program", cu.toString());
		}
		catch(CompilationException ex)
		{
			System.err.println(source);
			throw ex;
		}

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

		var expectedOutput = Driver.class.getResourceAsStream(String.format("/tests/%s/expected.txt", testName));
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
