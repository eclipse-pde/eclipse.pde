package a.b.c;


/**
 * Test supported @noimplement tag on constructors in outer / inner classes
 */
public class test54 {
	
	static class inner {
		/**
		 * Constructor
		 * @noimplement This constructor is not intended to be referenced by clients.
		 */
		public inner() {
			
		}
		
		/**
		 * Constructor
		 * @noimplement This constructor is not intended to be referenced by clients.
		 */
		protected inner(int i) {
			
		}
		static class inner2 {
			/**
			 * Constructor
			 * @noimplement This constructor is not intended to be referenced by clients.
			 */
			public inner2() {
				
			}
			
			/**
			 * Constructor
			 * @noimplement This constructor is not intended to be referenced by clients.
			 */
			protected inner2(int i) {
				
			}
		}
	}
}

class outer {
	/**
	 * Constructor
	 * @noimplement This constructor is not intended to be referenced by clients.
	 */
	public outer() {
		
	}
	
	/**
	 * Constructor
	 * @noimplement This constructor is not intended to be referenced by clients.
	 */
	protected outer(int i) {
		
	}
}
