package a.b.c;


/**
 * Test unsupported @noreference tag on private constructors in outer / inner classes
 */
public class test34 {
	
	static class inner {
		/**
		 * Constructor
		 * @noreference This constructor is not intended to be referenced by clients.
		 */
		private inner() {
			
		}
		
		/**
		 * Constructor
		 * @noreference This constructor is not intended to be referenced by clients.
		 */
		private inner(int i) {
			
		}
		class inner2 {
			/**
			 * Constructor
			 * @noreference This constructor is not intended to be referenced by clients.
			 */
			private inner2() {
				
			}
			
			/**
			 * Constructor
			 * @noreference This constructor is not intended to be referenced by clients.
			 */
			private inner2(int i) {
				
			}
		}
	}
}

class outer {
	/**
	 * Constructor
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	private outer() {
		
	}
	
	/**
	 * Constructor
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	private outer(int i) {
		
	}
}
