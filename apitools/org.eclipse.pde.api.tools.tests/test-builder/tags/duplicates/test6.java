package a.b.c;

/**
 */
public class test6 {
	
	/**
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void m1() {
		
	}
	/**
	 */
	public class Inner {
		/**
		 * @nooverride This method is not intended to be re-implemented or extended by clients.
		 * @noreference This method is not intended to be referenced by clients.
		 * @nooverride This method is not intended to be re-implemented or extended by clients.
		 * @noreference This method is not intended to be referenced by clients.
		 */
		public void m1() {
			
		}
	}
}

/**
 */
class outer {
	/**
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void m1() {
		
	}
}
