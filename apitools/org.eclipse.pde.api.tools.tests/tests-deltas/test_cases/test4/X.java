import java.io.Serializable;

public class X<T extends Serializable & Cloneable, U extends Exception> {
		T t;
    public static <U extends Object> void foo(U u) {}
    public static <U extends Object & Serializable> void foo(String s, int i, U u) {}
    public static void foo(String s, int i, String[] tab) {}
    
    public void bar() {
    	foo("", 0, null);
    }
}