package il.org.spartan.refactoring.utils;

import java.util.*;
import org.eclipse.jdt.core.dom.*;

/** An empty <code><b>enum</b></code> for fluent programming. The name should say
 * it all: The name, followed by a dot, followed by a method name, should read
 * like a sentence phrase.
 * @author Yossi Gil
 * @since 2015-07-16 */
@SuppressWarnings("unchecked")//
public enum expose {
  ;
  /** Expose the list of arguments in a {@link ClassInstanceCreation}
   * @param ¢ JD
   * @return reference to the list of arguments in the argument */
  public static List<Expression> arguments(final ClassInstanceCreation ¢) {
    return ¢.arguments();
  }
  /** Expose the list of arguments in a {@link MethodInvocation}
   * @param ¢ JD
   * @return reference to the list of arguments in the argument */
  public static List<Expression> arguments(final MethodInvocation ¢) {
    return ¢.arguments();
  }
  /** Expose the list of arguments in a {@link SuperMethodInvocation}
   * @param ¢ JD
   * @return reference to the list of arguments in the argument */
  public static List<Expression> arguments(final SuperMethodInvocation ¢) {
    return ¢.arguments();
  }
  public static List<BodyDeclaration> bodyDeclarations(final AbstractTypeDeclaration ¢) {
    return ¢.bodyDeclarations();
  }
  public static List<BodyDeclaration> bodyDeclarations(final AnonymousClassDeclaration ¢) {
    return ¢.bodyDeclarations();
  }
  public static List<CatchClause> catchClauses(final TryStatement ¢) {
    return ¢.catchClauses();
  }
  /** Expose the list of extended operands in an {@link InfixExpression}
   * @param ¢ JD
   * @return reference to the list of extended operands contained in the
   *         parameter */
  public static List<Expression> extendedOperands(final InfixExpression ¢) {
    return ¢.extendedOperands();
  }
  /** Expose the list of fragments in a {@link FieldDeclaration}
   * @param ¢ JD
   * @return reference to the list of fragments in the argument */
  public static List<VariableDeclarationFragment> fragments(final FieldDeclaration ¢) {
    return ¢.fragments();
  }
  /** Expose the list of fragments in a {@link VariableDeclarationExpression}
   * @param ¢ JD
   * @return reference to the list of fragments in the argument */
  public static List<VariableDeclarationFragment> fragments(final VariableDeclarationExpression ¢) {
    return ¢ != null ? ¢.fragments() : new ArrayList<>();
  }
  /** Expose the list of fragments in a {@link VariableDeclarationStatement}
   * @param ¢ JD
   * @return reference to the list of fragments in the argument */
  public static List<VariableDeclarationFragment> fragments(final VariableDeclarationStatement ¢) {
    return ¢.fragments();
  }
  /** Expose the list of initializers contained in a {@link ForStatement}
   * @param ¢ JD
   * @return reference to the list of initializers contained in the argument */
  public static List<Expression> initializers(final ForStatement ¢) {
    return ¢.initializers();
  }
  public static List<IExtendedModifier> modifiers(final VariableDeclarationStatement ¢) {
    return ¢.modifiers();
  }
  public static List<IExtendedModifier> modifiers(final BodyDeclaration ¢) {
    return ¢.modifiers();
  }
  /** Expose the list of parameters in a {@link MethodDeclaration}
   * @param ¢ JD
   * @return result of method {@link MethodDeclaration#parameters} downcasted to
   *         its correct type */
  public static List<SingleVariableDeclaration> parameters(final MethodDeclaration ¢) {
    return ¢.parameters();
  }
  /** Expose the list of resources contained in a {@link TryStatement}
   * @param ¢ JD
   * @return reference to the list of resources contained in the argument */
  public static List<VariableDeclarationExpression> resources(final TryStatement ¢) {
    return ¢.resources();
  }
  /** Expose the list of statements contained in a {@link Block}
   * @param ¢ JD
   * @return reference to the list of statements contained in the argument */
  public static List<Statement> statements(final Block ¢) {
    return ¢.statements();
  }
  public static List<TagElement> tags(final Javadoc ¢) {
    return ¢.tags();
  }
  public static List<ParameterizedType> typeArguments(final ParameterizedType ¢) {
    return ¢.typeArguments();
  }
}
