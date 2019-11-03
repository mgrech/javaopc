package dev.mgrech.javaopc;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.util.function.Function;

public class JavaopcProxyFileObject extends SimpleJavaFileObject
{
	final JavaFileObject object;
	private final Function<String, String> process;

	private String source = null;

	public JavaopcProxyFileObject(JavaFileObject object, Function<String, String> process)
	{
		super(object.toUri(), Kind.SOURCE);
		this.object = object;
		this.process = process;
	}

	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException
	{
		if(source == null)
			source = process.apply(object.getCharContent(ignoreEncodingErrors).toString());

		return source;
	}
}
