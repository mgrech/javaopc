import java.math.BigInteger;

public class Program
{
	public static void main(String[] args)
	{
		var a = BigInteger.valueOf(1);
		var b = BigInteger.valueOf(2);
		System.out.println(a < b);
		System.out.println(a <= b);
		System.out.println(a > b);
		System.out.println(a >= b);
	}
}
