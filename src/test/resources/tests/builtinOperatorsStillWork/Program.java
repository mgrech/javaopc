public class Program
{
	public static void main(String[] args)
	{
		var a = 1;
		var b = 2;
		System.out.println(a - b);
		b -= a;
		System.out.println(b);

		var c = (Integer)a;
		var d = (Integer)b;
		d -= c;
		System.out.println(d);

		++d;
		c--;
		System.out.println(c);
		System.out.println(d);
	}
}
