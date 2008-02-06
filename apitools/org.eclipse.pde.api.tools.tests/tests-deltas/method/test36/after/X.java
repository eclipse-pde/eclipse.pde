import java.io.Serializable;

public class X {
	public <V extends Exception, U extends Object & Serializable> void foo(U u, V v) {}
}