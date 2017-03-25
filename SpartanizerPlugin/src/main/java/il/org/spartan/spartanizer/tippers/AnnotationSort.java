package il.org.spartan.spartanizer.tippers;

import static java.util.stream.Collectors.*;

import static il.org.spartan.spartanizer.ast.navigate.step.*;

import java.util.*;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.*;
import org.eclipse.text.edits.*;
import org.jetbrains.annotations.*;

import il.org.spartan.*;
import il.org.spartan.spartanizer.ast.factory.*;
import il.org.spartan.spartanizer.ast.navigate.*;
import il.org.spartan.spartanizer.ast.safety.*;
import il.org.spartan.spartanizer.dispatch.*;
import il.org.spartan.spartanizer.engine.*;
import il.org.spartan.spartanizer.tipping.*;

/** TODO: kobybs please add a description
 * @author kobybs
 * @author Dan Abramovich
 * @since 20-11-2016 */
public class AnnotationSort<N extends BodyDeclaration> extends EagerTipper<N>//
    implements TipperCategory.Sorting {
  private static final long serialVersionUID = -3384979771292763464L;
  private static final HashSet<String>[] rankTable = as.array(//
      new HashSet<>(as.list("Deprecated")), //
      new HashSet<>(as.list("Override")), //
      new HashSet<>(as.list("Documented", "FunctionalInterface", "Inherited", "Retention", "Repeatable", "SafeVarargs", "Target")),
      new HashSet<>(as.list("$USER_DEFINED_ANNOTATION$")),
      new HashSet<>(as.list("Action", "Addressing", "BindingType", "ConstructorProperties", "DescriptorKey", "FaultAction", "Generated",
          "HandlerChain", "InitParam", "MTOM", "MXBean", "Oneway", "PostConstruct", "PreDestroy", "RequestWrapper", "Resource", "Resources",
          "RespectBinding", "ResponseWrapper", "ServiceMode", "SOAPBinding", "SOAPMessageHandler", "SOAPMessageHandlers", "SupportedAnnotationTypes",
          "SupportedOptions", "SupportedSourceVersion", "Transient", "WebEndpoint", "WebFault", "WebMethod", "WebParam", "WebResult", "WebService",
          "WebServiceClient", "WebServiceFeatureAnnotation", "WebServiceProvider", "WebServiceRef", "WebServiceRefs", "XmlAccessorOrder",
          "XmlAccessorType", "XmlAnyAttribute", "XmlAnyElement", "XmlAttachmentRef", "XmlAttribute", "XmlElement", "XmlElementDecl", "XmlElementRef",
          "XmlElementRefs", "XmlElements", "XmlElementWrapper", "XmlEnum", "XmlEnumValue", "XmlID", "XmlIDREF", "XmlInlineBinaryData",
          "XmlJavaTypeAdapter", "XmlJavaTypeAdapters", "XmlList", "XmlMimeType", "XmlMixed", "XmlNs", "XmlRegistry", "XmlRootElement", "XmlSchema",
          "XmlSchemaType", "XmlSchemaTypes", "XmlSeeAlso", "XmlTransient", "XmlType", "XmlValue")),
      new HashSet<>(as.list("SuppressWarnings")), //
      new HashSet<>(as.list("NonNull", "Nullable")));

  private static int rankAnnotation(final IExtendedModifier ¢) {
    return rankAnnotation(identifier(typeName(az.annotation(¢))));
  }

  private static int rankAnnotation(final String annotationName) {
    int $ = 0;
    for (@NotNull final HashSet<String> ¢ : rankTable) {
      ++$;
      if (¢.contains(annotationName))
        return $;
    }
    return rankAnnotation("$USER_DEFINED_ANNOTATION$");
  }

  private static final Comparator<IExtendedModifier> comp = (m1, m2) -> rankAnnotation(m1) - rankAnnotation(m2) == 0 ? (m1 + "").compareTo(m2 + "")
      : rankAnnotation(m1) - rankAnnotation(m2);

  public static int compare(@NotNull final String annotation1, @NotNull final String annotation2) {
    return rankAnnotation(annotation1) - rankAnnotation(annotation2) == 0 ? annotation1.compareTo(annotation2)
        : rankAnnotation(annotation1) - rankAnnotation(annotation2);
  }

  private static List<? extends IExtendedModifier> sort(@NotNull final Collection<? extends IExtendedModifier> ¢) {
    return ¢.stream().sorted(comp).collect(toList());
  }

  @Override @Nullable public Tip tip(@NotNull final N n) {
    final List<Annotation> $ = extract.annotations(n);
    if ($ == null || $.isEmpty())
      return null;
    @NotNull final List<Annotation> myCopy = new ArrayList<>($);
    myCopy.sort(comp);
    return myCopy.equals($) ? null : new Tip(description(n), n, getClass()) {
      @Override public void go(@NotNull final ASTRewrite r, final TextEditGroup g) {
        final ListRewrite l = r.getListRewrite(n, n.getModifiersProperty());
        for (int i = 0; i < $.size(); ++i) {
          List<Annotation> sorted = (List<Annotation>) copy.of(myCopy);
          final ASTNode oldNode = $.get(i), newNode = sorted.get(i);
          if (!wizard.same(oldNode,newNode)) {
            l.replace(oldNode, newNode, g);
//            l.replace(newNode, copy.of(oldNode), g);
//            break;
          }
        }
      }
    };
  }

  @Override @NotNull public String description(@NotNull final N ¢) {
    return "Sort annotations of " + extract.category(¢) + " " + extract.name(¢) + " (" + extract.annotations(¢) + "->" + sort(extract.annotations(¢))
        + ")";
  }
}