package dev.mgrech.javaopc;

import com.github.javaparser.Providers;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main
{
	private static TypeSolver solverForClassPath() throws IOException
	{
		var classPath = System.getenv("CLASSPATH");

		if(classPath == null)
			return new CombinedTypeSolver(new JavaParserTypeSolver("."), new ReflectionTypeSolver());

		var solver = (TypeSolver)new ReflectionTypeSolver();

		for(var path : classPath.split(";"))
		{
			var nextSolver = path.endsWith(".jar") ? new JarTypeSolver(path) : new JavaParserTypeSolver(path);
			solver = new CombinedTypeSolver(solver, nextSolver);
		}

		return solver;
	}

	private static boolean compile(Path sourceFilePath) throws IOException
	{
		var cu = JavaOperatorCompiler.parse(Providers.provider(sourceFilePath), solverForClassPath());

		if(cu == null)
			return false;

		var packageName = cu.getPackageDeclaration().map(NodeWithName::getNameAsString).orElse("");
		var className = sourceFilePath.getFileName().toString().replace(".java", "");
		var source = new MemoryJavaFileObject(packageName, className, cu.toString());

		var javac = ToolProvider.getSystemJavaCompiler();
		var manager = javac.getStandardFileManager(null, null, null);
		var task = javac.getTask(null, manager, null, null, null, List.of(source));

		return task.call();
	}

	public static void main(String[] args) throws IOException
	{
		if(args.length != 1)
		{
			System.err.println("invalid arguments");
			System.exit(1);
		}

		var sourceFile = Paths.get(args[0]);

		if(!compile(sourceFile))
			System.exit(1);
	}
}
