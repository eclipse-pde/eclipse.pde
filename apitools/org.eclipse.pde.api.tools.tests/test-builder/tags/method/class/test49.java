package a.b.c;


/**
 * Test supported @nooverride tag on constructors in outer / inner classes
 */
public class test49 {
	
	static class inner {
		/**
		 * Constructor
		 * @nooverride This constructor is not intended to be referenced by clients.
		 */
		public inner() {
			
		}
		
		/**
		 * Constructor
		 * @nooverride This constructor is not intended to be referenced by clients.
		 */
		protected inner(int i) {
			
		}
		static class inner2 {
			/**
			 * Constructor
			 * @nooverride This constructor is not intended to be referenced by clients.
			 */
			public inner2() {
				
			}
			
			/**
			 * Constructor
			 * @nooverride This constructor is not intended to be referenced by clients.
			 */
			protected inner2(int i) {
				
			}
		}
	}
}

class outer {
	/**
	 * Constructor
	 * @nooverride This constructor is not intended to be referenced by clients.
	 */
	public outer() {
		
	}
	
	/**
	 * Constructor
	 * @nooverride This constructor is not intended to be referenced by clients.
	 */
	protected outer(int i) {
		
	}
}
