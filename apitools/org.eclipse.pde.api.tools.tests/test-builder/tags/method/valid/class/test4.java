package a.b.c;


/**
 * Test supported @noreference tag on methods in outer / inner classes
 */
public class test4 {
	
	static class inner {
		/**
		 * @noreference This method is not intended to be referenced by clients.
		 * @nooverride This method is not intended to be re-implemented or extended by clients.
		 * @return
		 */
		public int m1() {
			return 0;
		}
		
		/**
		 * @noreference This method is not intended to be referenced by clients.
		 * @return
		 */
		public final char m2() {
			return 's';
		}
		
		/**
		 * @nooverride This method is not intended to be re-implemented or extended by clients.
		 * @noreference This method is not intended to be referenced by clients.
		 */
		protected void m3() {
			
		}
		
		/**
		 * @noreference This method is not intended to be referenced by clients.
		 * @return
		 */
		protected static Object m4() {
			return null;
		}
		static class inner2 {
			/**
			 * @noreference This method is not intended to be referenced by clients.
			 * @nooverride This method is not intended to be re-implemented or extended by clients.
			 * @return
			 */
			public int m1() {
				return 0;
			}
			
			/**
			 * @noreference This method is not intended to be referenced by clients.
			 * @return
			 */
			public final char m2() {
				return 's';
			}
			
			/**
			 * @nooverride This method is not intended to be re-implemented or extended by clients.
			 * @noreference This method is not intended to be referenced by clients.
			 */
			protected void m3() {
				
			}
			
			/**
			 * @noreference This method is not intended to be referenced by clients.
			 * @return
			 */
			protected static Object m4() {
				return null;
			}
		}
	}
}

class outer {
	/**
	 * @noreference This method is not intended to be referenced by clients.
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @return
	 */
	public int m1() {
		return 0;
	}
	
	/**
	 * @noreference This method is not intended to be referenced by clients.
	 * @return
	 */
	public final char m2() {
		return 's';
	}
	
	/**
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected void m3() {
		
	}
	
	/**
	 * @noreference This method is not intended to be referenced by clients.
	 * @return
	 */
	protected static Object m4() {
		return null;
	}
}
