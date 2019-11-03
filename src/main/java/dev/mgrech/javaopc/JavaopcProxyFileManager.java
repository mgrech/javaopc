package dev.mgrech.javaopc;

import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.io.IOException;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class JavaopcProxyFileManager extends ForwardingJavaFileManager<StandardJavaFileManager>
{
	private final Function<String, String> process;

	public JavaopcProxyFileManager(StandardJavaFileManager fileManager, Function<String, String> process)
	{
		super(fileManager);
		this.process = process;
	}

	@Override
	public String inferBinaryName(Location location, JavaFileObject file)
	{
		if(file instanceof JavaopcProxyFileObject)
			return super.inferBinaryName(location, ((JavaopcProxyFileObject)file).object);

		return super.inferBinaryName(location, file);
	}

	@Override
	public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException
	{
		Iterable<JavaFileObject> list = super.list(location, packageName, kinds, recurse);

		return StreamSupport.stream(list.spliterator(), false)
		                    .map(o -> o.getKind() == JavaFileObject.Kind.SOURCE ? new JavaopcProxyFileObject(o, process) : o)
		                    .collect(Collectors.toList());
	}
}
