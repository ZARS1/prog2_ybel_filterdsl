package filter.ast.builder;

import filter.FilterLexer;
import filter.FilterParser;
import filter.ast.nodes.Expr;
import java.util.function.Function;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class AstBuilders {

  public static Expr fromQuery(String query, Function<FilterParser.QueryContext, Expr> translator) {
    return simplify(translator.apply(parse(query)));
  }

  public static Expr simplify(Expr e) {
    if (e instanceof Expr.Not not) {
      Expr inner = simplify(not.inner());

      if (inner instanceof Expr.Not doubleNot) {
        return simplify(doubleNot.inner());
      }

      return new Expr.Not(inner);
    }

    if (e instanceof Expr.And and) {
      return new Expr.And(simplify(and.left()), simplify(and.right()));
    }

    if (e instanceof Expr.Or or) {
      return new Expr.Or(simplify(or.left()), simplify(or.right()));
    }

    return e;
  }

  public static FilterParser.QueryContext parse(String query) {
    var cs = CharStreams.fromString(query);
    var lexer = new FilterLexer(cs);
    var tokens = new CommonTokenStream(lexer);
    var parser = new FilterParser(tokens);

    var ctx = parser.query();
    if (parser.getNumberOfSyntaxErrors() > 0) {
      throw new IllegalStateException("Syntax errors in query: " + query);
    }

    return ctx;
  }
}
