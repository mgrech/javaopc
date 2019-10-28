package dev.mgrech.javaopc.test;

import java.math.BigInteger;

@SuppressWarnings("unused")
public class BigIntegerExtensions
{
	public static BigInteger opConvert(long l)
	{
		return BigInteger.valueOf(l);
	}

	public static BigInteger opSum(BigInteger left, BigInteger right)
	{
		return left.add(right);
	}

	public static BigInteger opIncrement(BigInteger i)
	{
		return i.add(BigInteger.ONE);
	}

	public static BigInteger opDecrement(BigInteger i)
	{
		return i.subtract(BigInteger.ONE);
	}
}
