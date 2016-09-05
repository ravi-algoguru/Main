package il.org.spartan.refactoring.java;

import static il.org.spartan.Utils.*;
import static org.eclipse.jdt.core.dom.ASTNode.*;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.*;
import static org.eclipse.jdt.core.dom.PrefixExpression.Operator.*;

import java.util.*;

import org.eclipse.jdt.core.dom.*;

import il.org.spartan.refactoring.ast.*;
import il.org.spartan.refactoring.utils.*;

/** <p>
 * Tells how much we know about the type of of a variable, function, or
 * expression. This should be conservative approximation to the real type of the
 * entity, what a rational, but prudent programmer would case about the type
 * <p>
 * Dispatching in this class should emulate the type inference of Java. It is
 * simple to that by hard coding constants.
 * <p>
 * This type should never be <code><b>null</b></code>. Don't bother to check
 * that it is. We want a {@link NullPointerException} thrown if this is the
 * case. or, you may as well write
 *
 * <pre>
 * Kind k = f();
 * assert k != null : //
 * "Implementation of Kind is buggy";
 * </pre>
 * 
 * 
 * @author Niv Shalmon
 * @since 2016-06-05
 */
public interface type {
  
  /** A dictionary containing all baptized types we encountered */
  static Map<String,type> dictionary = new HashMap<>();
  
  public enum PsuedoPrimitive implements type{
    // Those anonymous characters that known little or nothing about themselves
    NOTHING("none", "when nothing can be said, e.g., f(f(),f(f(f()),f()))"), //
    NONNULL("!null", "e.g., new Object() and that's about it"), //
    BAPTIZED("!double&!long&!int", "an object of some type, for which we have a name only"), //
    VOID("void", "nothing at all"),
    // Doubtful types, from four fold uncertainty down to bilalteral
    // schizophrenia" .
    ALPHANUMERIC("String|double|float|long|int|char|short|byte", "only in binary plus: f()+g(), 2 + f(), nor f() + null"), //
    NUMERIC("double|float|long|int|char|short|byte", "must be either f()*g(), 2L*f(), 2.*a(), not 2 %a(), nor 2"), //
    BOOLEANINTEGRAL("boolean|long|int|char|short|byte", "only in x^y,x&y,x|y"), //
    INTEGRAL("long|int|char|short|byte", "must be either int or long: f()%g()^h()<<f()|g()&h(), not 2+(long)f() "), //
    // Certain types
    NULL("null", "when it is certain to be null: null, (null), ((null)), etc. but nothing else"),//
    BYTE("byte", "must be byte: (byte)1, nothing else"), //
    SHORT("short", "must be short: (short)15, nothing else"), //
    CHAR("char", "must be char: 'a', (char)97, nothing else"), //
    INT("int", "must be int: 2, 2*(int)f(), 2%(int)f(), 'a'*2 , no 2*f()"), //
    LONG("long", "must be long: 2L, 2*(long)f(), 2%(long)f(), no 2*f()"), //
    FLOAT("float", "must be float: 2f, 2.3f+1, 2F-f()"), //
    DOUBLE("double", "must be double: 2.0, 2.0*a()-g(), no 2%a(), no 2*f()"), //
    BOOLEAN("boolean", "must be boolean: !f(), f() || g() "), //
    STRING("String", "must be string: \"\"+a, a.toString(), f()+null, not f()+g()"),//
    ;
    @SuppressWarnings("unused") static PsuedoPrimitive axiom(final boolean x) {
      return BOOLEAN;
    }

    // from here on is the axiom method used for testing of PrudentType. see issue
    // #105 for more details
    @SuppressWarnings("unused") static PsuedoPrimitive axiom(final byte x) {
      return BYTE;
    }

    @SuppressWarnings("unused") static PsuedoPrimitive axiom(final char x) {
      return CHAR;
    }

    @SuppressWarnings("unused") static PsuedoPrimitive axiom(final double x) {
      return DOUBLE;
    }

    @SuppressWarnings("unused") static PsuedoPrimitive axiom(final float x) {
      return FLOAT;
    }

    @SuppressWarnings("unused") static PsuedoPrimitive axiom(final int x) {
      return INT;
    }

    @SuppressWarnings("unused") static PsuedoPrimitive axiom(final long x) {
      return LONG;
    }

    @SuppressWarnings("unused") static PsuedoPrimitive axiom(final Object o) {
      return NOTHING;
    }

    @SuppressWarnings("unused") static PsuedoPrimitive axiom(final short x) {
      return SHORT;
    }

    @SuppressWarnings("unused") static PsuedoPrimitive axiom(final String x) {
      return STRING;
    }

    private static PsuedoPrimitive conditionalWithNoInfo(final PsuedoPrimitive t) {
      switch (t) {
        case BYTE:
        case SHORT:
        case CHAR:
        case INT:
        case INTEGRAL:
        case LONG:
        case FLOAT:
        case NUMERIC:
          return NUMERIC;
        case DOUBLE:
          return DOUBLE;
        case STRING:
          return STRING;
        case BOOLEAN:
          return BOOLEAN;
        case BOOLEANINTEGRAL:
          return BOOLEANINTEGRAL;
        default:
          return NOTHING;
      }
    }

    /**@param x JD
     * @return The most specific Type information that can be deduced about the
     *         expression, or {@link #NOTHING} if it cannot decide. Will never
     *         return null */
    private static PsuedoPrimitive prudent(final Expression x) {
      return prudent(x, null, null);
    }
    
    @Override public PsuedoPrimitive asPrudentType(){
      return this;
    }

    /** A version of {@link #prudent(Expression)} that receives the a list of the
     * operands' type for all operands of an expression. To be used for
     * InfixExpression that has extended operand. The order of the type's should
     * much the order of the operands returned by extract.allOperands(), and for
     * any operand whose type is unknown, there should be a null. The list won't
     * be used if the size of the list doesn't match that of
     * extract.allOperands().
     * @param ts list of types of operands. Must be at least of size 2 */
    static PsuedoPrimitive prudent(final Expression x, final List<PsuedoPrimitive> ts) {
      assert ts.size() >= 2;
      switch (x.getNodeType()) {
        case NULL_LITERAL:
          return NULL;
        case CHARACTER_LITERAL:
          return CHAR;
        case STRING_LITERAL:
          return STRING;
        case BOOLEAN_LITERAL:
          return BOOLEAN;
        case NUMBER_LITERAL:
          return prudentType((NumberLiteral) x);
        case CAST_EXPRESSION:
          return prudentType((CastExpression) x);
        case PREFIX_EXPRESSION:
          return prudentType((PrefixExpression) x, lisp.first(ts));
        case INFIX_EXPRESSION:
          return prudentType((InfixExpression) x, ts);
        case POSTFIX_EXPRESSION:
          return prudentType((PostfixExpression) x, lisp.first(ts));
        case PARENTHESIZED_EXPRESSION:
          return prudentType((ParenthesizedExpression) x, lisp.first(ts));
        case CLASS_INSTANCE_CREATION:
          return prudentType((ClassInstanceCreation) x);
        case METHOD_INVOCATION:
          return prudentType((MethodInvocation) x);
        case CONDITIONAL_EXPRESSION:
          return prudentType((ConditionalExpression) x, lisp.first(ts), lisp.second(ts));
        case ASSIGNMENT:
          return prudentType((Assignment) x, lisp.first(ts));
        default:
          return NOTHING;
      }
    }

    /** A version of {@link #prudent(Expression)} that receives the operand's type
     * for a single operand expression. The call kind(e,null) is equivalent to
     * kind(e) */
    static PsuedoPrimitive prudent(final Expression x, final PsuedoPrimitive t) {
      return prudent(x, t, null);
    }

    /** A version of {@link #prudent(Expression)} that receives the operands' type
     * for a two operand expression. The call kind(e,null,null) is equivalent to
     * kind(e)
     * @param t1 the type of the left hand operand of the expression, the type of
     *        the then expression of the conditional, or null if unknown
     * @param t2 the type of the left hand operand of the expression, the type of
     *        the else expression of the conditional, or null if unknown */
    static PsuedoPrimitive prudent(final Expression x, final PsuedoPrimitive t1, final PsuedoPrimitive t2) {
      final List<PsuedoPrimitive> ¢ = new ArrayList<>();
      ¢.add(t1);
      ¢.add(t2);
      return prudent(x, ¢);
    }

    private static PsuedoPrimitive prudentType(final Assignment x, final PsuedoPrimitive t) {
      final PsuedoPrimitive $ = t != null ? t : prudent(x.getLeftHandSide());
      return !$.isNoInfo() ? $ : prudent(x.getRightHandSide()).isNumeric() ? NUMERIC : prudent(x.getRightHandSide());
    }

    private static PsuedoPrimitive prudentType(final CastExpression x) {
      return typeSwitch("" + step.type(x), BAPTIZED);
    }

    private static PsuedoPrimitive prudentType(final ClassInstanceCreation c) {
      return typeSwitch("" + c.getType(), NONNULL);
    }

    private static PsuedoPrimitive prudentType(final ConditionalExpression x, final PsuedoPrimitive t1, final PsuedoPrimitive t2) {
      final PsuedoPrimitive $ = t1 != null ? t1 : prudent(x.getThenExpression());
      final PsuedoPrimitive ¢2 = t2 != null ? t2 : prudent(x.getElseExpression());
      // If we don't know much about one operand but do know enough about the
      // other, we can still learn something
      return $ == ¢2 ? $
          : $.isNoInfo() || ¢2.isNoInfo() ? conditionalWithNoInfo($.isNoInfo() ? ¢2 : $) //
              : $.isIntegral() && ¢2.isIntegral() ? $.underIntegersOnlyOperator(¢2) //
                  : $.isNumeric() && ¢2.isNumeric() ? $.underNumericOnlyOperator(¢2)//
                      : NOTHING; //
    }

    private static PsuedoPrimitive prudentType(final InfixExpression x, final List<PsuedoPrimitive> ts) {
      final InfixExpression.Operator o = x.getOperator();
      final List<Expression> es = extract.allOperands(x);
      assert es.size() >= 2;
      final List<PsuedoPrimitive> ¢ = new ArrayList<>();
      if (ts.size() != es.size())
        for (int i = 0; i < es.size(); ++i)
          ¢.add(i, prudent(es.get(i)));
      else
        for (int i = 0; i < ts.size(); ++i)
          ¢.add(i, ts.get(i) != null ? ts.get(i) : prudent(es.get(i)));
      PsuedoPrimitive $ = lisp.first(¢).underBinaryOperator(o, lisp.second(¢));
      lisp.chop(lisp.chop(¢));
      while (!¢.isEmpty()) {
        $ = $.underBinaryOperator(o, lisp.first(¢));
        lisp.chop(¢);
      }
      return $;
    }

    private static PsuedoPrimitive prudentType(final MethodInvocation i) {
      return "toString".equals(i.getName() + "") && i.arguments().isEmpty() ? STRING : NOTHING;
    }

    private static PsuedoPrimitive prudentType(final NumberLiteral l) {
      // TODO: Dor use TypeLiteral instead.
      final String ¢ = l.getToken();
      return ¢.matches("[0-9]+") ? INT
          : ¢.matches("[0-9]+[l,L]") ? LONG
              : ¢.matches("[0-9]+\\.[0-9]*[f,F]") || ¢.matches("[0-9]+[f,F]") ? FLOAT
                  : ¢.matches("[0-9]+\\.[0-9]*[d,D]?") || ¢.matches("[0-9]+[d,D]") ? DOUBLE : NUMERIC;
    }

    private static PsuedoPrimitive prudentType(final ParenthesizedExpression x, final PsuedoPrimitive t) {
      return t != null ? t : prudent(extract.core(x));
    }

    private static PsuedoPrimitive prudentType(final PostfixExpression x, final PsuedoPrimitive t1) {
      return (t1 != null ? t1 : prudent(x.getOperand())).asNumeric(); // see
                                                                      // testInDecreamentSemantics
    }

    private static PsuedoPrimitive prudentType(final PrefixExpression x, final PsuedoPrimitive t1) {
      return (t1 != null ? t1 : prudent(x.getOperand())).under(x.getOperator());
    }

    private static PsuedoPrimitive typeSwitch(final String s, final PsuedoPrimitive $) {
      switch (s) {
        case "byte":
        case "Byte":
          return BYTE;
        case "short":
        case "Short":
          return SHORT;
        case "char":
        case "Character":
          return CHAR;
        case "int":
        case "Integer":
          return INT;
        case "long":
        case "Long":
          return LONG;
        case "float":
        case "Float":
          return FLOAT;
        case "double":
        case "Double":
          return DOUBLE;
        case "boolean":
        case "Boolean":
          return BOOLEAN;
        case "String":
          return STRING;
        default:
          return $;
      }
    }

    final String description;
    final String name;

    PsuedoPrimitive(final String name, final String description) {
      this.name = name;
      this.description = description;
    }

    /** @return one of {@link #INT}, {@link #LONG}, {@link #CHAR}, {@link BYTE},
     *         {@link SHORT} or {@link #INTEGRAL}, in case it cannot decide */
    private PsuedoPrimitive asIntegral() {
      return isIntegral() ? this : INTEGRAL;
    }

    /** @return one of {@link #INT}, {@link #LONG}, or {@link #INTEGRAL}, in case
     *         it cannot decide */
    private PsuedoPrimitive asIntegralUnderOperation() {
      return isIntUnderOperation() ? INT : asIntegral();
    }

    /** @return one of {@link #INT}, {@link #LONG},, {@link #CHAR}, {@link BYTE},
     *         {@link SHORT}, {@link FLOAT}, {@link #DOUBLE}, {@link #INTEGRAL} or
     *         {@link #NUMERIC}, in case no further information is available */
    private PsuedoPrimitive asNumeric() {
      return isNumeric() ? this : NUMERIC;
    }

    /** @return one of {@link #INT}, {@link #LONG}, {@link #FLOAT},
     *         {@link #DOUBLE}, {@link #INTEGRAL} or {@link #NUMERIC}, in case no
     *         further information is available */
    private PsuedoPrimitive asNumericUnderOperation() {
      return !isNumeric() ? NUMERIC : isIntUnderOperation() ? INT : this;
    }

    public final String fullName() {
      return this + "=" + name + " (" + description + ")";
    }

    /** @return true if one of {@link #INT}, {@link #LONG}, {@link #CHAR},
     *         {@link BYTE}, {@link SHORT}, {@link FLOAT}, {@link #DOUBLE},
     *         {@link #INTEGRAL} or {@link #NUMERIC}, {@link #STRING},
     *         {@link #ALPHANUMERIC} or false otherwise */
    public boolean isAlphaNumeric() {
      return in(this, INT, LONG, CHAR, BYTE, SHORT, FLOAT, DOUBLE, INTEGRAL, NUMERIC, STRING, ALPHANUMERIC);
    }

    /** @return true if one of {@link #INT}, {@link #LONG}, {@link #CHAR},
     *         {@link BYTE}, {@link SHORT}, {@link #INTEGRAL} or false
     *         otherwise */
    public boolean isIntegral() {
      return in(this, LONG, INT, CHAR, BYTE, SHORT, INTEGRAL);
    }

    /** used to determine whether an integral type behaves as itself under
     * operations or as an INT.
     * @return true if one of {@link #CHAR}, {@link BYTE}, {@link SHORT} or false
     *         otherwise. */
    private boolean isIntUnderOperation() {
      return in(this, CHAR, BYTE, SHORT);
    }

    /** @return true if one of {@link #NOTHING}, {@link #BAPTIZED},
     *         {@link #NONNULL}, {@link #VOID}, {@link #NULL} or false
     *         otherwise */
    private boolean isNoInfo() {
      return in(this, NOTHING, BAPTIZED, NONNULL, VOID, NULL);
    }

    /** @return true if one of {@link #INT}, {@link #LONG}, {@link #CHAR},
     *         {@link BYTE}, {@link SHORT}, {@link FLOAT}, {@link #DOUBLE},
     *         {@link #INTEGRAL}, {@link #NUMERIC} or false otherwise */
    public boolean isNumeric() {
      return in(this, INT, LONG, CHAR, BYTE, SHORT, FLOAT, DOUBLE, INTEGRAL, NUMERIC);
    }

    /** @return one of {@link #BOOLEAN} , {@link #INT} , {@link #LONG} ,
     *         {@link #DOUBLE} , {@link #INTEGRAL} or {@link #NUMERIC} , in case
     *         it cannot decide */
    private PsuedoPrimitive under(final PrefixExpression.Operator o) {
      assert o != null;
      return o == NOT ? BOOLEAN : in(o, DECREMENT, INCREMENT) ? asNumeric() : o != COMPLEMENT ? asNumericUnderOperation() : asIntegralUnderOperation();
    }

    /** @return one of {@link #BOOLEAN} , {@link #INT} , {@link #LONG} ,
     *         {@link #DOUBLE} , {@link #STRING} , {@link #INTEGRAL} ,
     *         {@link BOOLEANINTEGRAL} {@link #NUMERIC} , or {@link #ALPHANUMERIC}
     *         , in case it cannot decide */
    private PsuedoPrimitive underBinaryOperator(final InfixExpression.Operator o, final PsuedoPrimitive k) {
      if (o == wizard.PLUS2)
        return underPlus(k);
      if (wizard.isComparison(o))
        return BOOLEAN;
      if (wizard.isBitwiseOperator(o))
        return underBitwiseOperation(k);
      if (o == REMAINDER)
        return underIntegersOnlyOperator(k);
      if (in(o, LEFT_SHIFT, RIGHT_SHIFT_SIGNED, RIGHT_SHIFT_UNSIGNED))
        return asIntegralUnderOperation();
      if (!in(o, TIMES, DIVIDE, wizard.MINUS2))
        throw new IllegalArgumentException("o=" + o + " k=" + k.fullName() + "this=" + this);
      return underNumericOnlyOperator(k);
    }

    /** @return one of {@link #BOOLEAN}, {@link #INT}, {@link #LONG},
     *         {@link #INTEGRAL} or {@link BOOLEANINTEGRAL}, in case it cannot
     *         decide */
    private PsuedoPrimitive underBitwiseOperation(final PsuedoPrimitive k) {
      return k == this ? k
          : isIntegral() && k.isIntegral() ? underIntegersOnlyOperator(k)
              : isNoInfo() ? k.underBitwiseOperationNoInfo() //
                  : k.isNoInfo() ? underBitwiseOperationNoInfo() //
                      : BOOLEANINTEGRAL;
    }

    /** @return one of {@link #BOOLEAN}, {@link #INT}, {@link #LONG},
     *         {@link #INTEGRAL} or {@link BOOLEANINTEGRAL}, in case it cannot
     *         decide */
    private PsuedoPrimitive underBitwiseOperationNoInfo() {
      return this == BOOLEAN ? BOOLEAN : !isIntegral() ? BOOLEANINTEGRAL : this == LONG ? LONG : INTEGRAL;
    }

    private PsuedoPrimitive underIntegersOnlyOperator(final PsuedoPrimitive k) {
      final PsuedoPrimitive ¢1 = asIntegralUnderOperation();
      final PsuedoPrimitive ¢2 = k.asIntegralUnderOperation();
      return in(LONG, ¢1, ¢2) ? LONG : !in(INTEGRAL, ¢1, ¢2) ? INT : INTEGRAL;
    }

    /** @return one of {@link #INT}, {@link #LONG}, {@link #INTEGRAL},
     *         {@link #DOUBLE}, or {@link #NUMERIC}, in case it cannot decide */
    private PsuedoPrimitive underNumericOnlyOperator(final PsuedoPrimitive k) {
      if (!isNumeric())
        return asNumericUnderOperation().underNumericOnlyOperator(k);
      assert k != null;
      assert this != ALPHANUMERIC : "Don't confuse " + NUMERIC + " with " + ALPHANUMERIC;
      assert isNumeric() : this + ": is for some reason not numeric ";
      final PsuedoPrimitive $ = k.asNumericUnderOperation();
      assert $ != null;
      assert $.isNumeric() : this + ": is for some reason not numeric ";
      // Double contaminates Numeric
      // Numeric contaminates Float
      // FLOAT contaminates Integral
      // LONG contaminates INTEGRAL
      // INTEGRAL contaminates INT
      // Everything else is INT after an operation
      return in(DOUBLE, $, this) ? DOUBLE
          : in(NUMERIC, $, this) ? NUMERIC //
              : in(FLOAT, $, this) ? FLOAT //
                  : in(LONG, $, this) ? LONG : //
                      !in(INTEGRAL, $, this) ? INT : INTEGRAL;
    }

    /** @return one of {@link #INT}, {@link #LONG}, {@link #DOUBLE},
     *         {@link #STRING}, {@link #INTEGRAL}, {@link #NUMERIC} or
     *         {@link #ALPHANUMERIC}, in case it cannot decide */
    private PsuedoPrimitive underPlus(final PsuedoPrimitive k) {
      // addition with NULL or String must be a String
      // unless both operands are numric, the result may be a String
      return in(STRING, this, k) || in(NULL, this, k) ? STRING : !isNumeric() || !k.isNumeric() ? ALPHANUMERIC : underNumericOnlyOperator(k);
    }
  }
  
  /**@param e JD
   * @return The most specific Type information that can be deduced about the
   *         expression, or {@link #NOTHING} if it cannot decide. Will never
   *         return null */
  //TODO: synthetic-access since this is incomplete. Should not happen once get is done 
  @SuppressWarnings("synthetic-access") static type get(Expression e){
    return PsuedoPrimitive.prudent(e);
  }
  
 default PsuedoPrimitive asPrudentType(){
   return PsuedoPrimitive.BAPTIZED;
 }
 
 @SuppressWarnings("unused") default boolean canBe(PsuedoPrimitive e){
   return false;
 }
 
 static type Baptize(String name){
   if (dictionary.containsKey(name))
     return dictionary.get(name);
   return store(name,new type() {
     //TODO: override implementation of type's function's if neccecary. Using default function may have made this obsolete.
   });
 }
 
 static type store(String s,type $){
   dictionary.put(s, $);
   return $;
 }

}
