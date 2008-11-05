package a.b.c;

public enum test5 {

	A;
	
	/**
	 * @noreference This enum field is not intended to be referenced by clients.
	 * @noreference This enum field is not intended to be referenced by clients.
	 * @noreference This enum field is not intended to be referenced by clients.
	 */
	public String s = null;
	
	public enum inner {
		A;
		
		/**
		 * @noreference This enum field is not intended to be referenced by clients.
		 * @noreference This enum field is not intended to be referenced by clients.
		 */
		public Object o = null;
	}
}

enum outer {
	B;
	
	/**
	 * @noreference This enum field is not intended to be referenced by clients.
	 * @noreference This enum field is not intended to be referenced by clients.
	 */
	protected int i = -1;
}
