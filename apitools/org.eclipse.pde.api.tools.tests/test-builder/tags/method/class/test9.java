package a.b.c;


/**
 * Test supported @noextend tag on methods in outer / inner classes
 */
public class test9 {
	
	static class inner {
		/**
		 * @noextend
		 * @return
		 */
		public int m1() {
			return 0;
		}
		
		/**
		 * @noextend
		 * @return
		 */
		public final char m2() {
			return 's';
		}
		
		/**
		 * @noextend
		 */
		protected void m3() {
			
		}
		
		/**
		 * @noextend
		 * @return
		 */
		protected static Object m4() {
			return null;
		}
		static class inner2 {
			/**
			 * @noextend
			 * @return
			 */
			public int m1() {
				return 0;
			}
			
			/**
			 * @noextend
			 * @return
			 */
			public final char m2() {
				return 's';
			}
			
			/**
			 * @noextend
			 */
			protected void m3() {
				
			}
			
			/**
			 * @noextend
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
	 * @noextend
	 * @return
	 */
	public int m1() {
		return 0;
	}
	
	/**
	 * @noextend
	 * @return
	 */
	public final char m2() {
		return 's';
	}
	
	/**
	 * @noextend
	 */
	protected void m3() {
		
	}
	
	/**
	 * @noextend
	 * @return
	 */
	protected static Object m4() {
		return null;
	}
}
