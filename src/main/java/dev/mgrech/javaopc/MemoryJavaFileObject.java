package dev.mgrech.javaopc;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;

public class MemoryJavaFileObject extends SimpleJavaFileObject
{
	private final String source;

	public MemoryJavaFileObject(String packageName, String className, String source)
	{
		super(URI.create(String.format("memory:///%s/%s.java", packageName, className)), Kind.SOURCE);
		this.source = source;
	}

	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors)
	{
		return source;
	}
}
