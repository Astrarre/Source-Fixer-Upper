package io.github.astrarre.sfu;

import java.util.*;

/**
 * {@link java.lang.Package}
 *
 * @param <C>
 */
class Test<C extends List<?>> {

	{
		System.out.println("cope");
		yes("", null);
	}

	class Inner {
		void init(Object o) {}
	}

	private <T extends String & java.io.Serializable> void yes(T t, List<C> cope) {
		var e = new ArrayList<String>() {
			void foo() {}
		}; // e		e.foo();
		var inner = new Inner(); // auybdawuidaubiw
		inner.init(new Object()); //aaaaa cope adawdawadawda awdawdawdgytt
		{
			class Local extends ArrayList<String> {
				void crab() {}
			}
			Local local = new Local();
			local.crab();
		}

		class Local extends ArrayList<String> {
			void crab() {}
		}

		class Local3 extends ArrayList<String> {
			void crab() {}
		}
	}
}