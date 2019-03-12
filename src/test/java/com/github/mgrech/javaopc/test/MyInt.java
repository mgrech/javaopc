package com.github.mgrech.javaopc.test;

@SuppressWarnings("unused")
public class MyInt
{
	private final int i;

	public MyInt(int i)
	{
		this.i = i;
	}

	public MyInt opAdd(MyInt other)
	{
		return new MyInt(i + other.i);
	}

	public String toString()
	{
		return String.format("MyInt(%s)", i);
	}
}
