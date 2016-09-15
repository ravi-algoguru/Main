package il.org.spartan.spartanizer.wring;

import static il.org.spartan.spartanizer.ast.extract.*;
import static il.org.spartan.spartanizer.ast.step.*;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.*;
import org.eclipse.text.edits.*;

import il.org.spartan.spartanizer.assemble.*;
import il.org.spartan.spartanizer.ast.*;
import il.org.spartan.spartanizer.wring.dispatch.*;
import il.org.spartan.spartanizer.wring.strategies.*;

/** convert
 *
 * <pre>
 * a = 3;
 * return a;
 * </pre>
 *
 * to
 *
 * <pre>
 * return a = 3;
 * </pre>
 *
 * @author Yossi Gil
 * @since 2015-08-28 */
// TODO: Dan/Alex: add an issue for the following: if a is a local variable, you
// convert to simply a=3. Assignment to local variables should not be preserved.
// To make this happen, we need a wring that removes dead assignments to local
// variables. Attach the "new-wring" label to this one.
//
// To clarify, if 'a' is a local variable, you would like the new wring to change
// 'return a=3' to 'return 3'? Also, I understand the new wring should only work on return statements?
// Or did you have something more extensive in mind? Is this the same as issue 194?
public final class AssignmentAndReturn extends ReplaceToNextStatement<Assignment> implements Kind.Collapse {
  @Override public String description(final Assignment ¢) {
    return "Inline assignment to " + to(¢) + " with its subsequent 'return'";
  }

  @Override protected ASTRewrite go(final ASTRewrite r, final Assignment a, final Statement nextStatement, final TextEditGroup g) {
    final Statement parent = az.asStatement(a.getParent());
    if (parent == null || parent instanceof ForStatement)
      return null;
    final ReturnStatement s = az.returnStatement(nextStatement);
    if (s == null || !wizard.same(to(a), core(s.getExpression())))
      return null;
    r.remove(parent, g);
    r.replace(s, subject.operand(a).toReturn(), g);
    return r;
  }
}
