package a.b.c;


/**
 * Test supported @noextend tag on constructors in outer / inner classes
 */
public class test44 {
	
	static class inner {
		/**
		 * Constructor
		 * @noextend This constructor is not intended to be referenced by clients.
		 */
		public inner() {
			
		}
		
		/**
		 * Constructor
		 * @noextend This constructor is not intended to be referenced by clients.
		 */
		protected inner(int i) {
			
		}
		static class inner2 {
			/**
			 * Constructor
			 * @noextend This constructor is not intended to be referenced by clients.
			 */
			public inner2() {
				
			}
			
			/**
			 * Constructor
			 * @noextend This constructor is not intended to be referenced by clients.
			 */
			protected inner2(int i) {
				
			}
		}
	}
}

class outer {
	/**
	 * Constructor
	 * @noextend This constructor is not intended to be referenced by clients.
	 */
	public outer() {
		
	}
	
	/**
	 * Constructor
	 * @noextend This constructor is not intended to be referenced by clients.
	 */
	protected outer(int i) {
		
	}
}
