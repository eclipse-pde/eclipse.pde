package a.b.c;


/**
 * Test supported @noimplement tag on methods in outer / inner classes
 */
public class test4 {
	
	static class inner {
		/**
		 * @noimplement
		 * @return
		 */
		public int m1() {
			return 0;
		}
		
		/**
		 * @noimplement
		 * @return
		 */
		public final char m2() {
			return 's';
		}
		
		/**
		 * @noimplement
		 */
		protected void m3() {
			
		}
		
		/**
		 * @noimplement
		 * @return
		 */
		protected static Object m4() {
			return null;
		}
		static class inner2 {
			/**
			 * @noimplement
			 * @return
			 */
			public int m1() {
				return 0;
			}
			
			/**
			 * @noimplement
			 * @return
			 */
			public final char m2() {
				return 's';
			}
			
			/**
			 * @noimplement
			 */
			protected void m3() {
				
			}
			
			/**
			 * @noimplement
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
	 * @noimplement
	 * @return
	 */
	public int m1() {
		return 0;
	}
	
	/**
	 * @noimplement
	 * @return
	 */
	public final char m2() {
		return 's';
	}
	
	/**
	 * @noimplement
	 */
	protected void m3() {
		
	}
	
	/**
	 * @noimplement
	 * @return
	 */
	protected static Object m4() {
		return null;
	}
}
