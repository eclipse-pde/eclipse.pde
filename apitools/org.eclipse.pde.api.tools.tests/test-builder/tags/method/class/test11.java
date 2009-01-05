package a.b.c;


/**
 * Test supported @noreference tag on private methods in outer / inner classes
 */
public class test11 {
	/**
	 * @noreference 
	 * @return
	 */
	private int m1() {
		return 0;
	}
	static class inner {
		/**
		 * @noreference 
		 * @return
		 */
		private int m1() {
			return 0;
		}
		static class inner2 {
			/**
			 * @noreference 
			 * @return
			 */
			private int m1() {
				return 0;
			}
		}
	}
}

class outer {
	/**
	 * @noreference 
	 * @return
	 */
	private int m1() {
		return 0;
	}
}
