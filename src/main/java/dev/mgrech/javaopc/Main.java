package dev.mgrech.javaopc;

import com.github.javaparser.Providers;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import javax.tools.ToolProvider;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Main
{
	private static TypeSolver solverForClassPath(String paths)
	{
		if(paths == null)
			return new CombinedTypeSolver(new JavaParserTypeSolver("."), new ReflectionTypeSolver());

		var solver = (TypeSolver)new ReflectionTypeSolver();

		try
		{
			for(var path : paths.split(";"))
			{
				var nextSolver = path.endsWith(".jar") ? new JarTypeSolver(path) : new JavaParserTypeSolver(path);
				solver = new CombinedTypeSolver(solver, nextSolver);
			}
		}
		catch(IOException ex)
		{
			throw new RuntimeException(ex);
		}

		return solver;
	}

	private static String process(String source, TypeSolver solver)
	{
		var cu = JavaOperatorCompiler.parse(Providers.provider(source), solver);

		if(cu == null)
			return null;

		return cu.toString();
	}

	public static void main(String[] args)
	{
		String classPath = "";

		for(int i = 0; i != args.length - 1; ++i)
			if(args[i].equals("-cp") || args[i].equals("-classpath"))
				classPath = args[i + 1];

		var solver = solverForClassPath(classPath);

		var sourceFileArgs = Arrays.stream(args).filter(a ->  a.endsWith(".java")).collect(Collectors.toList());
		var javacArgs = Arrays.stream(args).filter(a -> !a.endsWith(".java")).collect(Collectors.toList());

		var javac = ToolProvider.getSystemJavaCompiler();
		var manager = javac.getStandardFileManager(null, null, null);

		var sourceFiles = StreamSupport.stream(manager.getJavaFileObjectsFromStrings(sourceFileArgs).spliterator(), false)
		                               .map(o -> new JavaopcProxyFileObject(o, s -> process(s, solver)))
		                               .collect(Collectors.toList());

		var javaopcFileManager = new JavaopcProxyFileManager(manager, s -> process(s, solver));
		var task = javac.getTask(null, javaopcFileManager, null, javacArgs, null, sourceFiles);

		if(!task.call())
			System.exit(1);
	}
}
