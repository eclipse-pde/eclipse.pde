import java.io.Serializable;

public class X {
	public <U extends Object & Serializable, V extends Exception> void foo(U u, V v) {}
}