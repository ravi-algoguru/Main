package il.org.spartan.spartanizer.java;

import static il.org.spartan.Utils.*;
import static org.eclipse.jdt.core.dom.ASTNode.*;
import static org.eclipse.jdt.core.dom.PrefixExpression.Operator.*;

import org.eclipse.jdt.core.dom.*;

import static il.org.spartan.spartanizer.ast.navigate.step.*;
import static il.org.spartan.spartanizer.ast.navigate.step.fragments;
import static il.org.spartan.spartanizer.ast.navigate.step.statements;

import static il.org.spartan.spartanizer.ast.navigate.extract.*;

import il.org.spartan.spartanizer.ast.navigate.*;
import il.org.spartan.spartanizer.ast.safety.*;
import il.org.spartan.spartanizer.utils.*;

/** @author Yossi Gil
 * @since 2016 */
public enum sideEffects {
  MISSING_CASE;
  /** Determine whether the evaluation of an expression is guaranteed to be free
   * of any side effects.
   * @param e JD
   * @return <code><b>true</b></code> <i>iff</i> the parameter is an expression
   *         whose computation is guaranteed to be free of any side effects. */
  // VIM: /{/+,/}/-!sort -u
  private static final int[] alwaysFree = { //
      BOOLEAN_LITERAL, //
      CHARACTER_LITERAL, //
      EMPTY_STATEMENT, //
      FIELD_ACCESS, //
      LAMBDA_EXPRESSION, //
      NULL_LITERAL, //
      NUMBER_LITERAL, //
      PRIMITIVE_TYPE, //
      QUALIFIED_NAME, //
      SIMPLE_NAME, //
      SIMPLE_TYPE, //
      STRING_LITERAL, //
      SUPER_FIELD_ACCESS, //
      THIS_EXPRESSION, //
      TYPE_LITERAL, //
  };
  private static final int[] alwaysHave = { //
      SUPER_CONSTRUCTOR_INVOCATION, //
      SUPER_METHOD_INVOCATION, //
      METHOD_INVOCATION, //
      CLASS_INSTANCE_CREATION, //
      ASSIGNMENT, //
      POSTFIX_EXPRESSION, //
  };

  public static boolean deterministic(final Expression x) {
    if (!sideEffects.free(x))
      return false;
    final Bool $ = new Bool(true);
    x.accept(new ASTVisitor() {
      @Override public boolean visit(@SuppressWarnings("unused") final ArrayCreation __) {
        $.clear();
        return false;
      }
    });
    return $.get();
  }

  private static boolean free(final ArrayCreation ¢) {
    return free(step.dimensions(¢)) && free(step.expressions(¢.getInitializer()));
  }

  public static boolean free(final ASTNode ¢) {
    return ¢ == null || //
        (iz.expression(¢) ? sideEffects.free(az.expression(¢))
            : iz.expressionStatement(¢) ? sideEffects.free(step.expression(az.expressionStatement(¢)))
                : iz.isVariableDeclarationStatement(¢) ? sideEffects.free(az.variableDeclrationStatement(¢))
                    : iz.block(¢) && sideEffects.free(az.block(¢)));
  }

  public static boolean free(final Block b) {
    for (final Statement ¢ : statements(b))
      if (!free(¢))
        return false;
    return true;
  }

  private static boolean free(final ConditionalExpression ¢) {
    return free(expression(¢), then(¢), elze(¢));
  }

  public static boolean free(final Expression ¢) {
    if (¢ == null || iz.nodeTypeIn(¢, alwaysFree))
      return true;
    if (iz.nodeTypeIn(¢, alwaysHave))
      return false;
    switch (¢.getNodeType()) {
      case ARRAY_CREATION:
        return free((ArrayCreation) ¢);
      case ARRAY_ACCESS:
        return free(((ArrayAccess) ¢).getArray(), ((ArrayAccess) ¢).getIndex());
      case CAST_EXPRESSION:
        return sideEffects.free(step.expression(¢));
      case INSTANCEOF_EXPRESSION:
        return sideEffects.free(left(az.instanceofExpression(¢)));
      case PREFIX_EXPRESSION:
        return free(az.prefixExpression(¢));
      case PARENTHESIZED_EXPRESSION:
        return sideEffects.free(core(¢));
      case INFIX_EXPRESSION:
        return free(extract.allOperands(az.infixExpression(¢)));
      case CONDITIONAL_EXPRESSION:
        return free(az.conditionalExpression(¢));
      case ARRAY_INITIALIZER:
        return free(step.expressions(az.arrayInitializer(¢)));
      case VARIABLE_DECLARATION_EXPRESSION:
        return free(az.variableDeclarationExpression(¢));
      default:
        monitor.logProbableBug(//
            sideEffects.MISSING_CASE, new AssertionError("Missing 'case' in switch for class: " + ¢.getClass().getSimpleName()));
        return false;
    }
  }

  private static boolean free(final Expression... xs) {
    for (final Expression ¢ : xs)
      if (!sideEffects.free(¢))
        return false;
    return true;
  }

  public static boolean free(final Iterable<? extends Expression> xs) {
    if (xs == null)
      return true;
    for (final Expression ¢ : xs)
      if (!sideEffects.free(az.expression(¢)))
        return false;
    return true;
  }

  public static boolean free(final MethodDeclaration ¢) {
    return sideEffects.free(¢.getBody());
  }

  private static boolean free(final PrefixExpression ¢) {
    return in(¢.getOperator(), PLUS, MINUS, COMPLEMENT, NOT) && sideEffects.free(step.operand(¢));
  }

  private static boolean free(final VariableDeclarationExpression x) {
    for (final VariableDeclarationFragment ¢ : step.fragments(x))
      if (!sideEffects.free(initializer(¢)))
        return false;
    return true;
  }

  public static boolean free(final VariableDeclarationStatement s) {
    for (final VariableDeclarationFragment ¢ : fragments(s))
      if (!sideEffects.free(¢.getInitializer()))
        return false;
    return true;
  }
}
