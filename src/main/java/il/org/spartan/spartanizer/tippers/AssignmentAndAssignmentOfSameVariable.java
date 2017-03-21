package il.org.spartan.spartanizer.tippers;

import static il.org.spartan.utils.Example.*;

import java.util.*;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Assignment.*;
import org.eclipse.jdt.core.dom.rewrite.*;
import org.eclipse.text.edits.*;

import il.org.spartan.spartanizer.ast.safety.*;
import il.org.spartan.spartanizer.dispatch.*;
import il.org.spartan.spartanizer.java.*;
import il.org.spartan.spartanizer.tipping.*;
import il.org.spartan.utils.*;

/** Removes redundant assignment- an assignment with same variable subsequent
 * assignment.
 * @author Ori Roth <tt>ori.rothh@gmail.com</tt>
 * @since 2017-03-20
 * @see issue #1110 */
public class AssignmentAndAssignmentOfSameVariable extends ReplaceToNextStatement<Assignment> //
    implements TipperCategory.CommnonFactoring {
  private static final long serialVersionUID = -2175075259560385549L;

  @Override public String description(@SuppressWarnings("unused") Assignment __) {
    return description();
  }

  @Override public String description() {
    return "eliminate redundant assignment";
  }

  @Override public Example[] examples() {
    return new Example[] { //
        convert("x = 1; x = 2;") //
            .to("x = 2;"), //
        convert("x.y = 1; x.y = 2;") //
            .to("x.y = 2;"), //
        ignores("x = f(); x = 2;"), //
        ignores("x = 1; x += 2;") //
    };
  }

  @Override protected ASTRewrite go(ASTRewrite $, Assignment a, Statement nextStatement, TextEditGroup g) {
    Assignment nextAssignment = Optional.of(nextStatement) //
        .map(λ -> az.expressionStatement(λ)) //
        .map(λ -> az.assignment(λ.getExpression())).orElse(null);
    if (nextAssignment == null || nextAssignment.getOperator() != Operator.ASSIGN)
      return null;
    Name left1 = az.name(a.getLeftHandSide());
    Expression right1 = a.getRightHandSide();
    if (left1 == null || right1 == null)
      return null;
    Name left2 = az.name(nextAssignment.getLeftHandSide());
    if (left2 == null //
        || !left1.getFullyQualifiedName().equals(left2.getFullyQualifiedName()) //
        || !sideEffects.sink(right1))
      return null;
    $.remove(a.getParent(), g);
    return $;
  }
}