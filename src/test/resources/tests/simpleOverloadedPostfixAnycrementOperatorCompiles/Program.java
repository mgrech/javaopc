import static dev.mgrech.javaopc.test.BigIntegerExtensions.*;

import java.math.BigInteger;

public class Program
{
	public static void main(String[] args)
	{
		var i = BigInteger.ZERO;
		System.out.println(i++);
		System.out.println(i--);
		System.out.println(i);
	}
}
