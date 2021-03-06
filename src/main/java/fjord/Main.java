package fjord;

import static me.qmx.jitescript.CodeBlock.newCodeBlock;
import static me.qmx.jitescript.util.CodegenUtils.ci;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import me.qmx.jitescript.JiteClass;

import java.lang.reflect.Method;
import java.io.Console;

import fjord.compiler.Compiler;
import fjord.ast.*;

public class Main {

  public static void main(String[] args) throws Exception {
    banner();

    Environment env = new Environment();

    while (!env.isHalted()) {
      con.printf("> ");

      String input = con.readLine();
      if (input == null)
        break;

      eval(env, input);
    }
  }

  private static void eval(final Environment env, String input) throws Exception {
    Compiler compiler = new Compiler();

    Node node = compiler.compile(input);
    if (node == null)
      return;

    node.accept(new DefaultNodeVisitor() {
      @Override public void visit(CompilerDirectiveDecl decl) {
        if (decl.getIdent().equals("help"))
          help();
        else if (decl.getIdent().equals("quit"))
          env.halt();
        else
          con.printf("Invalid directive '%s'\n", decl);
      }
      @Override public void visit(ValueDefn defn) {
        con.printf("val %s = %s\n", defn.pattern(), eval(defn));
      }
    });
  }

  private static Object eval(final ValueDefn defn) {
    try {
      JiteClass jiteClass = new JiteClass(defn.pattern()) {{
        defineMethod("apply", ACC_PUBLIC | ACC_STATIC, sig(Object.class),
          newCodeBlock()
            .ldc(defn.expr())
            .areturn()
        );
      }};

      Class<?> klass = new JiteClassLoader().define(jiteClass);
      Method applyMethod = klass.getMethod("apply");

      return applyMethod.invoke(null);
    } catch (Exception e) {
      return null;
    }
  }

  private static void banner() {
    con.printf("Fjord\n");
    con.printf("\n");
    con.printf("For help type #help\n");
  }

  private static void help() {
    con.printf("\n");
    con.printf("  Directives:\n");
    con.printf("\n");
    con.printf("    #help                Display help\n");
    con.printf("    #quit                Exit\n");
    con.printf("\n");
  }

  private static Console con = System.console();

  private static class Environment {
    private boolean halted;

    public void halt() {
      this.halted = true;
    }

    public boolean isHalted() {
      return halted;
    }
  }

  private static class JiteClassLoader extends ClassLoader {
    public Class<?> define(JiteClass jiteClass) {
      byte[] classBytes = jiteClass.toBytes();
      return super.defineClass(jiteClass.getClassName(), classBytes, 0, classBytes.length);
    }
  }
}
