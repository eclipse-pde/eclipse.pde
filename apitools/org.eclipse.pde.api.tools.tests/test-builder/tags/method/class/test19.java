package a.b.c;


/**
 * Test supported @nooverride tag on private methods in outer / inner classes
 */
public class test19 {
	
	static class inner {
		/**
		 * @nooverride This method is not intended to be re-implemented or extended by clients.
		 * @return
		 */
		private int m1() {
			return 0;
		}
		static class inner2 {
			/**
			 * @nooverride This method is not intended to be re-implemented or extended by clients.
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
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @return
	 */
	private int m1() {
		return 0;
	}
}
