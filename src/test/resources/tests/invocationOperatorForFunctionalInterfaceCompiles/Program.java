import java.util.function.Function;

public class Program
{
	public static void main(String[] args)
	{
		Function<Integer, Integer> double_ = i -> 2 * i;
		System.out.println(double_(1));
	}
}
