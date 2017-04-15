package il.org.spartan.utils.fluent;

import java.util.function.*;

/** TODO Yossi Gil: document class
 * @author Yossi Gil
 * @since 2017-04-14 */
public interface Selfie<Self extends Selfie<Self>> {
  default <F> Self self(Supplier<F> t) {
    change(t.get());
    return self();
  }

  Self self();

  default <U> void change(U ¢) {
    ¢.hashCode();
  }

}
