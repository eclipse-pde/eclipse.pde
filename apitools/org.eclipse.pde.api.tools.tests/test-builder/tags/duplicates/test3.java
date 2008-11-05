package a.b.c;

/**
 */
public class test3 {

	/**
	 * @noreference This field is not intended to be referenced by clients.
	 * @noreference This field is not intended to be referenced by clients.
	 */
	public String f1 = null;
	
	/**
	 */
	public class Inner {
		/**
		 * @noreference This field is not intended to be referenced by clients.
		 * @noreference This field is not intended to be referenced by clients.
		 */
		protected Object o = null;
	}
}

/**
 */
class outer {
	/**
	 * @noreference This field is not intended to be referenced by clients.
	 * @noreference This field is not intended to be referenced by clients.
	 */
	protected int i = -1;
}
