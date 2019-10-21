import java.util.Arrays;

class MyList
{
	private int[] array = new int[]{1, 2, 3};

	public String toString()
	{
		return Arrays.toString(array);
	}

	public static int[] opSubscriptGet(MyList list, int index)
	{
		return list.array;
	}

	public static void opSubscriptSet(MyList list, int index, int[] value)
	{
		list.array = value;
	}
}

public class Program
{
	public static void main(String[] args)
	{
		var a = new MyList[1];
		var l = new MyList();
		a[0] = l;
		System.out.println(Arrays.toString(a[0][0]));
		a[0][0][0] = 42;
		System.out.println(a[0][0][0]);
	}
}
