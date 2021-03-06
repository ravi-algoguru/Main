package il.org.spartan.spartanizer.java.namespace.tables;

import java.util.*;

import org.eclipse.jdt.core.dom.*;

import il.org.spartan.spartanizer.ast.navigate.*;
import il.org.spartan.tables.*;

/** @author Dor Ma'ayan
 * @since 2017-05-18 */
public class TestsVectorFeature {
  static Table table;
  static String tableName;

  static boolean isJunit4(CompilationUnit c) {
    List<ImportDeclaration> imports = extract.imports(c);
    for (ImportDeclaration i : imports) {
      if (i.getName().getFullyQualifiedName().contains("org.junit") && !isJunit5(c)) {
        return true;
      }
    }
    return false;
  }
  static boolean isJunit5(CompilationUnit c) {
    List<ImportDeclaration> imports = extract.imports(c);
    for (ImportDeclaration i : imports) {
      if (i.getName().getFullyQualifiedName().contains("org.junit.jupiter")) {
        return true;
      }
    }
    return false;
  }
}
