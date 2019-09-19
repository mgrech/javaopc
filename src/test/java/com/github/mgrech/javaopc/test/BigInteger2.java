package com.github.mgrech.javaopc.test;

import java.math.BigInteger;

@SuppressWarnings("unused")
public class BigInteger2
{
	private final BigInteger value;

	public BigInteger2(long value)
	{
		this.value = BigInteger.valueOf(value);
	}

	public BigInteger2(BigInteger value)
	{
		this.value = value;
	}

	public String toString()
	{
		return value.toString();
	}

	public static BigInteger2 opNegate(BigInteger2 bi)
	{
		return new BigInteger2(bi.value.negate());
	}

	public static BigInteger2 opSum(BigInteger2 left, BigInteger2 right)
	{
		return new BigInteger2(left.value.add(right.value));
	}

	public static BigInteger2 opDifference(BigInteger2 left, BigInteger2 right)
	{
		return new BigInteger2(left.value.subtract(right.value));
	}
}
