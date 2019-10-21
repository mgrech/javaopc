import java.math.BigInteger;

class Doubler
{
	private static final BigInteger TWO = BigInteger.valueOf(2);

	public static int opInvoke(Doubler doubler, int i)
	{
		return 2 * i;
	}

	public static BigInteger opInvoke(Doubler doubler, BigInteger i)
	{
		return TWO.multiply(i);
	}
}

public class Program
{
	public static void main(String[] args)
	{
		var double_ = new Doubler();
		System.out.println(double_(1));
		System.out.println(double_(BigInteger.ONE));
	}
}
