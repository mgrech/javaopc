class Asymmetric implements Comparable<Integer>
{
	private final int i;

	public Asymmetric(int i)
	{
		this.i = i;
	}

	@Override
	public int compareTo(Integer i)
	{
		return this.i - i;
	}
}

public class Program
{
	public static void main(String[] args)
	{
		var a = new Asymmetric(1);
		var b = 2;
		System.out.println(a < b);
		System.out.println(a <= b);
		System.out.println(a > b);
		System.out.println(a >= b);
	}
}
