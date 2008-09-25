package a.b.c;


/**
 * Test supported @noreference tag on methods in outer / inner classes
 */
public class test9 {
	
	static class inner {
		/**
		 * Constructor
		 * @noreference This constructor is not intended to be referenced by clients.
		 */
		public inner() {
			
		}
		
		/**
		 * Constructor
		 * @noreference This constructor is not intended to be referenced by clients.
		 */
		protected inner(int i) {
			
		}
		static class inner2 {
			/**
			 * Constructor
			 * @noreference This constructor is not intended to be referenced by clients.
			 */
			public inner2() {
				
			}
			
			/**
			 * Constructor
			 * @noreference This constructor is not intended to be referenced by clients.
			 */
			protected inner2(int i) {
				
			}
		}
	}
}

class outer {
	/**
	 * Constructor
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	public outer() {
		
	}
	
	/**
	 * Constructor
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	protected outer(int i) {
		
	}
}
