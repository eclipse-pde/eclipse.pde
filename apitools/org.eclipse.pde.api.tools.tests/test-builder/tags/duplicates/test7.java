package a.b.c;

/**
 */
public interface test7 {

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 * @noreference This method is not intended to be referenced by clients. 
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void m1();
	
	/**
	 */
	interface Inner {
		/**
		 * @noreference This method is not intended to be referenced by clients.
		 * @noreference This method is not intended to be referenced by clients.
		 * @noreference This method is not intended to be referenced by clients.
		 */
		public void m1();
	}
	
}

/**
 */
interface outer {
	/**
	 * @noreference This method is not intended to be referenced by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void m1();
}
