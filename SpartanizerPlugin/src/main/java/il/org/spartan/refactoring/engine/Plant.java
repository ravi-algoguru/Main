package il.org.spartan.refactoring.engine;

import static il.org.spartan.refactoring.ast.iz.*;

import org.eclipse.jdt.core.dom.*;

import il.org.spartan.refactoring.ast.*;
import il.org.spartan.refactoring.java.*;

/** A fluent API class that wraps an {@link Expression} with parenthesis, if the
 * location in which this expression occurs requires such wrapping.
 * <p>
 * Typical usage is in the form <code>new Plan(expression).in(host)</code> where
 * <code>location</code> is the parent under which the expression is to be
 * placed.
 * @author Yossi Gil
 * @since 2015-08-20 */
public class Plant {
  private final Expression inner;

  /** Instantiates this class, recording the expression that might be wrapped.
   * @param inner JD */
  Plant(final Expression inner) {
    this.inner = inner;
  }

  /** Factory method recording the expression that might be wrapped.
   * @param inner JD */
  public static Plant plant(final Expression inner) {
    return new Plant(inner);
  }

  /** Executes conditional wrapping in parenthesis.
   * @param host the destined parent
   * @return either the expression itself, or the expression wrapped in
   *         parenthesis, depending on the relative precedences of the
   *         expression and its host. */
  public Expression into(final ASTNode host) {
    return noParenthesisRequiredIn(host) || isSimple(inner) ? inner : parenthesize(inner);
  }

  private boolean noParenthesisRequiredIn(final ASTNode host) {
    return Precedence.greater(host, inner) || Precedence.equal(host, inner) && !Precedence.nonAssociative(host);
  }

  private ParenthesizedExpression parenthesize(final Expression e) {
    final ParenthesizedExpression $ = inner.getAST().newParenthesizedExpression();
    $.setExpression(wizard.duplicate(e));
    return $;
  }

  public Expression intoLeft(final InfixExpression host) {
    return Precedence.greater(host, inner) || Precedence.equal(host, inner) || isSimple(inner) ? inner : parenthesize(inner);
  }
}