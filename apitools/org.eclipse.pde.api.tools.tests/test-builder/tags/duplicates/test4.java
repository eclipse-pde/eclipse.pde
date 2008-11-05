package a.b.c;

/**
 */
public interface test4 {

	/**
	 * @noreference This field is not intended to be referenced by clients.
	 * @noreference This field is not intended to be referenced by clients.
	 */
	public Object o = null;
	
	/**
	 */
	interface Inner {
		/**
		 * @noreference This field is not intended to be referenced by clients.
		 * @noreference This field is not intended to be referenced by clients.
		 */
		public String s = null;
	}
	
}

/**
 */
interface outer {
	/**
	 * @noreference This field is not intended to be referenced by clients.
	 * @noreference This field is not intended to be referenced by clients.
	 */
	public int i = -1;
}
