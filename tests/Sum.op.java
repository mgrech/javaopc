class MyInt
{
	private final int i;

	public MyInt(int i)
	{
		this.i = i;
	}

	public MyInt opSum(MyInt other)
	{
		return new MyInt(i + other.i);
	}

	public String toString()
	{
		return i + "";
	}

	public static MyInt opSum(MyInt left, MyInt right)
	{
		return new MyInt(left.i + right.i);
	}
}

public class Test
{
	public static void main(String[] args)
	{
		var a = new MyInt(1);
		var b = new MyInt(2);
		System.out.println((a + b) + b);
		System.out.println(a + "");
		System.out.println("" + a);
		System.out.println("" + "");
	}
}