import java.util.Arrays;

class MyList
{
	private int[] array = new int[]{1, 2, 3};

	public String toString()
	{
		return Arrays.toString(array);
	}

	public static int opSubscriptGet(MyList list, int index)
	{
		return list.array[index];
	}

	public static void opSubscriptSet(MyList list, int index, int value)
	{
		list.array[index] = value;
	}
}

public class Program
{
	public static void main(String[] args)
	{
		var l = new MyList();
		l[0] += 42;
		System.out.println(l);
	}
}
