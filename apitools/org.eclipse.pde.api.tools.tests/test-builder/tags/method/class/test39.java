package a.b.c;


/**
 * Test supported @noinstantiate tag on constructors in outer / inner classes
 */
public class test39 {
	
	static class inner {
		/**
		 * Constructor
		 * @noinstantiate This constructor is not intended to be referenced by clients.
		 */
		public inner() {
			
		}
		
		/**
		 * Constructor
		 * @noinstantiate This constructor is not intended to be referenced by clients.
		 */
		protected inner(int i) {
			
		}
		static class inner2 {
			/**
			 * Constructor
			 * @noinstantiate This constructor is not intended to be referenced by clients.
			 */
			public inner2() {
				
			}
			
			/**
			 * Constructor
			 * @noinstantiate This constructor is not intended to be referenced by clients.
			 */
			protected inner2(int i) {
				
			}
		}
	}
}

class outer {
	/**
	 * Constructor
	 * @noinstantiate This constructor is not intended to be referenced by clients.
	 */
	public outer() {
		
	}
	
	/**
	 * Constructor
	 * @noinstantiate This constructor is not intended to be referenced by clients.
	 */
	protected outer(int i) {
		
	}
}
