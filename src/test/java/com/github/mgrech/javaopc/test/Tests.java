package com.github.mgrech.javaopc.test;

import org.junit.Test;

public class Tests
{
	@Test
	public void builtinOperatorsAreUnaffected()
	{
		Driver.runTest();
	}

	@Test
	public void lookupFindsOverloadsInArgumentClass()
	{
		Driver.runTest();
	}

	@Test
	public void lookupFindsOverloadsInCurrentClass()
	{
		Driver.runTest();
	}

	@Test
	public void lookupFindsOverloadsFromImports()
	{
		Driver.runTest();
	}

	@Test
	public void opSumNotInvokedForStrings()
	{
		Driver.runTest();
	}

	@Test
	public void nestedOverloadedOperatorUsageCompiles()
	{
		Driver.runTest();
	}

	@Test
	public void simpleCompoundAssignmentRewritingCompiles()
	{
		Driver.runTest();
	}

	@Test
	public void simpleOverloadedBinaryOperatorUsageCompiles()
	{
		Driver.runTest();
	}

	@Test
	public void simpleOverloadedUnaryOperatorUsageCompiles()
	{
		Driver.runTest();
	}
}