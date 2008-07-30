package a.b.c;


/**
 * Test supported @noinstantiate tag on methods in outer / inner classes
 */
public class test14 {
	
	static class inner {
		/**
		 * @noinstantiate
		 * @return
		 */
		public int m1() {
			return 0;
		}
		
		/**
		 * @noinstantiate
		 * @return
		 */
		public final char m2() {
			return 's';
		}
		
		/**
		 * @noinstantiate
		 */
		protected void m3() {
			
		}
		
		/**
		 * @noinstantiate
		 * @return
		 */
		protected static Object m4() {
			return null;
		}
		static class inner2 {
			/**
			 * @noinstantiate
			 * @return
			 */
			public int m1() {
				return 0;
			}
			
			/**
			 * @noinstantiate
			 * @return
			 */
			public final char m2() {
				return 's';
			}
			
			/**
			 * @noinstantiate
			 */
			protected void m3() {
				
			}
			
			/**
			 * @noinstantiate
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
	 * @noinstantiate
	 * @return
	 */
	public int m1() {
		return 0;
	}
	
	/**
	 * @noinstantiate
	 * @return
	 */
	public final char m2() {
		return 's';
	}
	
	/**
	 * @noinstantiate
	 */
	protected void m3() {
		
	}
	
	/**
	 * @noinstantiate
	 * @return
	 */
	protected static Object m4() {
		return null;
	}
}
