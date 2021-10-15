package plc.project;

import java.io.PrintWriter;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        //TODO
        print("public class Main {");
        newline(0);

        if (!ast.getFields().isEmpty()) {
            newline(++indent);
            for (int i = 0; i < ast.getFields().size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(ast.getFields().get(i));
            }
            newline(--indent);
        }

        newline(++indent);
        print("public static void main(String[] args) {");
        newline(++indent);
        print("System.exit(new Main().main());");
        newline(--indent);
        print("}");
        newline(--indent);

        if (!ast.getMethods().isEmpty()) {
            newline(++indent);
            for (int i = 0; i < ast.getMethods().size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(ast.getMethods().get(i));
                newline(0);
            }
            newline(--indent);
        } else {
            newline(0);
        }

        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        print(ast.getVariable().getType().getJvmName(), " ", ast.getVariable().getJvmName());
        if (ast.getValue().isPresent()) {
            print(" = ", ast.getValue().get());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        //TODO
        print(ast.getFunction().getReturnType().getJvmName(), " ", ast.getName(), "(");
        for (int i = 0; i < ast.getParameters().size(); i++) {
            print(Environment.getType(ast.getParameterTypeNames().get(i)).getJvmName(), " ", ast.getParameters().get(i));
            if (i < ast.getParameters().size() - 1) {
                print(", ");
            }
        }
        print(") {");
        if (!ast.getStatements().isEmpty()) {
            newline(++indent);
            for (int i = 0; i < ast.getStatements().size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(ast.getStatements().get(i));
            }
            newline(--indent);
        }
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        //TODO
        print(ast.getExpression(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        print(ast.getVariable().getType().getJvmName(), " ", ast.getVariable().getJvmName());
        if (ast.getValue().isPresent()) {
            print(" = ", ast.getValue().get());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        //TODO
        print(ast.getReceiver(), " = ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        //TODO
        print("if (", ast.getCondition(), ") {");
        if (!ast.getThenStatements().isEmpty()) {
            newline(++indent);
            for (int i = 0; i < ast.getThenStatements().size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(ast.getThenStatements().get(i));
            }
            newline(--indent);
        }
        print("}");
        if (!ast.getElseStatements().isEmpty()) {
            print(" else {");
            newline(++indent);
            for (int i = 0; i < ast.getElseStatements().size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(ast.getElseStatements().get(i));
            }
            newline(--indent);
            print("}");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        //TODO
        print("for (int ", ast.getName(), " : ", ast.getValue(), ") {");
        if (!ast.getStatements().isEmpty()) {
            newline(++indent);
            for (int i = 0; i < ast.getStatements().size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(ast.getStatements().get(i));
            }
            newline(--indent);
        }
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        //TODO
        // print the while structure, including condition
        // determine if there are statements to process
        //     setup the next line
        //     handle all statements in the while statement body
        //         check if newline and indent are needed
        //             setup next line
        //         print the next statement
        //     setup the next line
        // close the while

        print("while (", ast.getCondition(), ") {");

        if (!ast.getStatements().isEmpty()) {
            newline(++indent);
            for (int i = 0; i < ast.getStatements().size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(ast.getStatements().get(i));
            }
            newline(--indent);
        }
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        //TODO
        print("return ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        //TODO
        if (ast.getType().equals(Environment.Type.STRING)) {
            print("\"", ast.getLiteral(), "\"");
        } else if (ast.getType().equals(Environment.Type.CHARACTER)) {
            print("'", ast.getLiteral(), "'");
        } else {
            print(ast.getLiteral());
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        //TODO
        print("(");
        print(ast.getExpression());
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        //TODO
        print(ast.getLeft());
        print(" ");
        switch (ast.getOperator()) {
            case "AND":
                print("&&");
                break;
            case "OR":
                print("||");
                break;
            default:
                print(ast.getOperator());
                break;
        }
        print(" ");
        print(ast.getRight());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        //TODO
        if (ast.getReceiver().isPresent()) {
            print(ast.getReceiver().get());
            print(".");
        }
        print(ast.getVariable().getJvmName());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        //TODO
        if (ast.getReceiver().isPresent()) {
            print(ast.getReceiver().get());
            print(".");
        }

        print(ast.getFunction().getJvmName(), "(");
        if (!ast.getArguments().isEmpty()) {
            for (int i = 0; i < ast.getArguments().size(); i++) {
                print(ast.getArguments().get(i));
                if (i < ast.getArguments().size() - 1) {
                    print(", ");
                }
            }
        }
        print(")");
        return null;
    }

}
