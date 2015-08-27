package org.spartan.refactoring.wring;

import static org.spartan.refactoring.utils.Funcs.elze;
import static org.spartan.refactoring.utils.Funcs.not;
import static org.spartan.refactoring.wring.Wrings.*;

import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.spartan.refactoring.utils.Subject;

/**
 * A {@link Wring} to convert <code>if (x) ; else  {a;}</code> into
 * <code>if (!x) a;</code>.
 *
 * @author Yossi Gil
 * @since 2015-08-26
 */
public final class IfEmptyThen extends Wring.OfIfStatement {
  @Override Statement _replacement(final IfStatement s) {
    return Subject.pair(elze(s), null).toIf(not(s.getExpression()));
  }
  @Override boolean scopeIncludes(final IfStatement s) {
    return s != null && emptyThen(s) && !emptyElse(s);
  }
}