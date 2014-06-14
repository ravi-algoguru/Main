package il.ac.technion.cs.ssdl.spartan.refactoring;

import static il.ac.technion.cs.ssdl.spartan.utils.Funcs.countNodes;
import static org.eclipse.jdt.core.dom.ASTNode.BOOLEAN_LITERAL;
import static org.eclipse.jdt.core.dom.ASTNode.INFIX_EXPRESSION;
import static org.eclipse.jdt.core.dom.ASTNode.METHOD_INVOCATION;
import static org.eclipse.jdt.core.dom.ASTNode.NULL_LITERAL;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.AND;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.EQUALS;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.GREATER;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.GREATER_EQUALS;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.LESS;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.LESS_EQUALS;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.NOT_EQUALS;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.OR;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.PLUS;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.TIMES;
import static org.eclipse.jdt.core.dom.InfixExpression.Operator.XOR;
import il.ac.technion.cs.ssdl.spartan.utils.Range;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

/**
 * @author Ofir Elmakias <code><elmakias [at] outlook.com></code> (original /
 *         24.05.2014)
 * @author Tomer Zeltzer <code><tomerr90 [at] gmail.com></code> (original /
 *         24.05.2014)
 * @since 2014/05/24
 * TODO: Bug. Highlight should be on operator only. Otherwise it is too messy.
 * TODO: Bug. It supposes to switch concatenated strings, e.g., System.prinln("Value is "+ v)
 */
public class ShortestOperand extends Spartanization {
	/** Instantiates this class */
	public ShortestOperand() {
		super("Shortest operand first", "Make the shortest operand first in a binary commutative or semi-commutative operator");
	}
	@Override protected final void fillRewrite(final ASTRewrite r, final AST t, final CompilationUnit cu, final IMarker m) {
		cu.accept(new ASTVisitor() {
			@Override public boolean visit(final InfixExpression n) {
				if (invalid(n))
					return true;
				final AtomicBoolean hasChanged = new AtomicBoolean(false) ;
				final InfixExpression newNode = transpose(t, r, n, hasChanged);
				if (hasChanged.get())
					r.replace(n, newNode, null); // Replace old tree with
				return true;
			}

			private boolean invalid(final InfixExpression n) {
				return !inRange(m, n) || null == n.getLeftOperand() || null == n.getRightOperand();
			}
		});
	}
	/*public static InfixExpression commitTranspose(final AST ast, final ASTRewrite rewrite, final InfixExpression n){
		return transpose(ast, rewrite, n, new Boolean(true));
	}*/
	/**
	 * Transpose infix expressions recursively. Makes the shortest operand first
	 * on every subtree of the node.
	 *
	 * @param ast
	 *          The AST - for copySubTree.
	 * @param rewrite
	 *          The rewriter - to perform the change.
	 * @param n
	 *          The node.
	 * @param hasChanged
	 *          Indicates weather a change occurred.
	 *          reference to the passed value might be changed.
	 * @return Number of abstract syntax tree nodes under the parameter.
	 */
	public static InfixExpression transpose(final AST ast, final ASTRewrite rewrite, final InfixExpression n, final AtomicBoolean hasChanged) {
		final InfixExpression $ = (InfixExpression) ASTNode.copySubtree(ast, n);
		final Expression leftOperand = $.getLeftOperand();
		final Operator o = n.getOperator();
		if (isInfix(leftOperand))
			$.setLeftOperand(transpose(ast, rewrite, (InfixExpression) leftOperand, hasChanged));
		if (isInfix($.getRightOperand()))
			$.setRightOperand(transpose(ast, rewrite, (InfixExpression) $.getRightOperand(), hasChanged));
		final ASTNode newR = ASTNode.copySubtree(ast, n.getRightOperand());
		if (inRightOperandExceptions(newR, o))
			return $; 	// Prevents the following kind of swap:
		// "(a>0) == true" => "true == (a>0)"
		if (isFlipable(o) && longerFirst(n)){
			set($, (Expression) ASTNode.copySubtree(ast, n.getLeftOperand()), flipOperator(o), (Expression) newR);
			hasChanged.set(true);
		}

		return $;
	}

	@SuppressWarnings("boxing") // Justification: because ASTNode is a primitive int we can't use the generic "in" function on it without boxing into Integer. Any other solution will cause less readable/maintainable code.
	private static boolean inRightOperandExceptions (final ASTNode rN, final Operator o){
		final Integer t = new Integer(rN.getNodeType());
		if (isMethodInvocation(rN) && o == PLUS)
			return true;

		return  in(t, //
				BOOLEAN_LITERAL, //
				NULL_LITERAL, //
				null);

	}

	private static boolean isInfix(final ASTNode e){
		return INFIX_EXPRESSION == e.getNodeType();
	}
	private static boolean isMethodInvocation(final ASTNode e){
		return METHOD_INVOCATION == e.getNodeType();
	}
	private static void set(final InfixExpression $, final Expression left, final Operator operator, final Expression right) {
		$.setRightOperand(left);
		$.setOperator(operator);
		$.setLeftOperand(right);
	}
	/**
	 * Makes an opposite operator from a given one, which keeps its logical
	 * operation after the node swapping. e.g. "&" is commutative, therefore no
	 * change needed. "<" isn't commutative, but it has its opposite: ">=".
	 *
	 * @param o
	 *          The operator to flip
	 * @return The correspond operator - e.g. "<=" will become ">", "+" will stay
	 *         "+".
	 */
	public static Operator flipOperator(final Operator o) {
		return !conjugate.containsKey(o) ? o : conjugate.get(o);
	}
	private static Map<Operator, Operator> conjugate = makeConjeguates();
	/**
	 * @param o
	 *          The operator to check
	 * @return True - if the operator have opposite one in terms of operands swap.
	 * @see ShortestOperand
	 */
	public static boolean isFlipable(final Operator o) {
		// TODO: - Check Fixed Bugs -
		// Done: add bit wise or and bit wise not | I believe you meant to "|" and "&" (because "bitwise not" is unary). if that's the case they are already implemented here as "OR" and "AND" (presented at test 7) - there are also CONDITINAL versions of them but they are not commutative - therefore not applicable for this list
		// Done: add testing for XOR; it does not show up right. | I'm sure that there are some problems, but it's hard to reproduce them as "1 test case" test due to the fact that they might occur on more complex trees.
		// Done: add test case for string concatenation which uses "+" as well. | Added them, and even found and treated an hidden bug.
		return in(o, //
				AND, //
				EQUALS, //
				GREATER, //
				GREATER_EQUALS, //
				LESS_EQUALS, //
				LESS, //
				NOT_EQUALS, //
				OR, //
				PLUS, //
				TIMES, //
				XOR, //
				null);
	}
	private static Map<Operator, Operator> makeConjeguates() {
		final Map<Operator, Operator> $ = new HashMap<Operator, Operator>();
		$.put(GREATER, LESS);
		$.put(LESS, GREATER);
		$.put(GREATER_EQUALS, LESS_EQUALS);
		$.put(LESS_EQUALS, GREATER_EQUALS);
		return $;
	}
	private static <T> boolean in(final T candidate, final T... ts) {
		for (final T t : ts)
			if (t != null && t.equals(candidate))
				return true;
		return false;
	}
	private static final int threshold = 1;
	/**
	 * Determine if the ranges are overlapping in a part of their range
	 *
	 * @param a
	 *          b Ranges to merge
	 * @return True - if such an overlap exists
	 * @see merge
	 */
	protected static boolean areOverlapped(final Range a, final Range b) {
		return !(a.from > b.to || b.from > a.to); // Negation of
		// "not overlapped"
	}
	/**
	 * @param a
	 *          b Ranges to merge
	 * @return A new merged range.
	 * @see areOverlapped
	 */
	protected static Range merge(final Range a, final Range b) {
		return new Range(a.from < b.from ? a.from : b.from, a.to > b.to ? a.to : b.to);
	}
	/**
	 * Tries to union the given range with one of the elements inside the given
	 * list.
	 *
	 * @param rangeList
	 *          The list of ranges to union with
	 * @param rNew
	 *          The new range to union
	 * @return True - if the list updated and the new range consumed False - the
	 *         list remained intact
	 *
	 * @see areOverlapped
	 * @see merge
	 */
	protected static boolean unionRangeWithList(final List<Range> rangeList, final Range rNew) {
		boolean $ = false;
		for (Range r : rangeList)
			if (areOverlapped(r, rNew)) {
				r = merge(r, rNew);
				$ = true;
			}
		return $;
	}
	@Override protected ASTVisitor fillOpportunities(final List<Range> opportunities) {
		return new ASTVisitor() {
			@Override public boolean visit(final InfixExpression n) {
				final AtomicBoolean hasChanged = new AtomicBoolean(false) ;
				final AST t = AST.newAST(AST.JLS4);

				transpose(t, ASTRewrite.create(t), n, hasChanged);

				if (!hasChanged.get())
					return true;

				final Range rN = new Range(n.getParent());
				if (!unionRangeWithList(opportunities, rN))
					opportunities.add(rN);
				return true;
			}
		};
	}
	static boolean longerFirst(final InfixExpression n) {
		return null != n.getLeftOperand() && null != n.getRightOperand()
				&& countNodes(n.getLeftOperand()) > threshold + countNodes(n.getRightOperand());
	}


}


