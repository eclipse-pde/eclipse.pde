package a.b.c;

public enum test8 {

	A;
	
	/**
	 * @noreference This enum method is not intended to be referenced by clients.
	 * @noreference This enum method is not intended to be referenced by clients.
	 * @noreference This enum method is not intended to be referenced by clients.
	 */
	public void m1() {
		
	}
	
	public enum inner {
		A;
		
		/**
		 * @noreference This enum method is not intended to be referenced by clients.
		 * @noreference This enum method is not intended to be referenced by clients.
		 * @noreference This enum method is not intended to be referenced by clients.
		 */
		public void m1() {
			
		}
	}
}

enum outer {
	B;
	
	/**
	 * @noreference This enum method is not intended to be referenced by clients.
	 * @noreference This enum method is not intended to be referenced by clients.
	 * @noreference This enum method is not intended to be referenced by clients.
	 */
	public void m1() {
		
	}
}
