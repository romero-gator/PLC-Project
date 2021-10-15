package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Method method;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        for (Ast.Field field : ast.getFields()) {
            visit(field);
        }
        for (Ast.Method method : ast.getMethods()) {
            visit(method);
        }

        if (!(scope.lookupFunction("main", 0).getReturnType().equals(Environment.Type.INTEGER))) {
            throw new RuntimeException("Return type of main function is not Integer.");
        }

        return null;
    } // TODO test

    @Override
    public Void visit(Ast.Field ast) {
        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            requireAssignable(Environment.getType(ast.getTypeName()), ast.getValue().get().getType());
        }
        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName()), Environment.NIL));

        return null;
    } // TODO test

    @Override
    public Void visit(Ast.Method ast) {
        List<Environment.Type> typeList = new ArrayList<>();
        for (String name : ast.getParameterTypeNames()) {
            typeList.add(Environment.getType(name));
        }
        Environment.Type retType = Environment.Type.NIL;
        if (ast.getReturnTypeName().isPresent()) {
            retType = Environment.getType(ast.getReturnTypeName().get());
        }

        ast.setFunction(scope.defineFunction(ast.getName(), ast.getName(), typeList, retType, args -> Environment.NIL));

        try {
            scope = new Scope(scope);
            scope.defineVariable("returnType", "returnType", Environment.getType(retType.getName()), Environment.NIL);
            for (String name : ast.getParameters()) {
                scope.defineVariable(name, Environment.NIL);
            }
            for (Ast.Stmt stmt : ast.getStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }

        return null;
    } // TODO test

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        if (!(ast.getExpression() instanceof Ast.Expr.Function)) {
            throw new RuntimeException("Expression is not a Function.");
        }

        return null;
    } // TODO test

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        if (!ast.getTypeName().isPresent() && !ast.getValue().isPresent()) {
            throw new RuntimeException("Declaration must have type or value to infer type.");
        }

        Environment.Type type = null;

        if (ast.getTypeName().isPresent()) {
            type = Environment.getType(ast.getTypeName().get());
        }

        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            if (type == null) {
                type = ast.getValue().get().getType();
            }
            requireAssignable(type, ast.getValue().get().getType());
        }

        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), type, Environment.NIL));

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        visit(ast.getValue());
        visit(ast.getReceiver());
        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        visit(ast.getCondition());
        if (!ast.getCondition().getType().equals(Environment.Type.BOOLEAN)) {
            throw new RuntimeException("Condition is not a Boolean.");
        }

        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("List of Then Statements is empty.");
        }

        try {
            scope = new Scope(scope);
            for (Ast.Stmt stmt : ast.getThenStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }

        try {
            scope = new Scope(scope);
            for (Ast.Stmt stmt : ast.getElseStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }

        return null;
    } // TODO test

    @Override
    public Void visit(Ast.Stmt.For ast) {
        visit(ast.getValue());
        if (!ast.getValue().getType().equals(Environment.Type.INTEGER_ITERABLE)) {
            throw new RuntimeException("Value is not Integer Iterable");
        }

        if (ast.getStatements().isEmpty()) {
            throw new RuntimeException("Statement list is empty.");
        }

        try {
            scope = new Scope(scope);
            scope.defineVariable(ast.getName(), ast.getName(), Environment.Type.INTEGER, Environment.NIL);
            for (Ast.Stmt stmt : ast.getStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }

        return null;
    } // TODO test

    @Override
    public Void visit(Ast.Stmt.While ast) {
        visit(ast.getCondition());
        if (!ast.getCondition().getType().equals(Environment.Type.BOOLEAN)) {
            throw new RuntimeException("Condition is not Boolean.");
        }

        try {
            scope = new Scope(scope);
            for (Ast.Stmt stmt : ast.getStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }

        return null;
    } // TODO test

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        visit(ast.getValue());
        requireAssignable(scope.lookupVariable("returnType").getType(), ast.getValue().getType());
        return null;
    } // TODO test

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        if (ast.getLiteral() instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        } else if (ast.getLiteral() instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
        } else if (ast.getLiteral() instanceof String) {
            ast.setType(Environment.Type.STRING);
        } else if (ast.getLiteral() == null) {
            ast.setType(Environment.Type.NIL);
        } else if (ast.getLiteral() instanceof BigInteger) {
            if (((BigInteger) ast.getLiteral()).compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0 || ((BigInteger) ast.getLiteral()).compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0) {
                throw new RuntimeException("Value is out of range of 32-bit signed int.");
            }
            ast.setType(Environment.Type.INTEGER);
        } else if (ast.getLiteral() instanceof BigDecimal) {
            if (((BigDecimal) ast.getLiteral()).doubleValue() == Double.NEGATIVE_INFINITY || ((BigDecimal) ast.getLiteral()).doubleValue() == Double.POSITIVE_INFINITY) {
                throw new RuntimeException("Value is out of range of 64-bit signed float.");
            }
            ast.setType(Environment.Type.DECIMAL);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        if (!(ast.getExpression() instanceof Ast.Expr.Binary)) {
            throw new RuntimeException("Contained expression is not binary.");
        }
        visit(ast.getExpression());
        ast.setType(ast.getExpression().getType());

        return null;
    } // TODO test, added visit

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        if (ast.getOperator().equals("AND") || ast.getOperator().equals("OR")) {
            visit(ast.getLeft());
            requireAssignable(ast.getLeft().getType(), Environment.Type.BOOLEAN);
            visit(ast.getRight());
            requireAssignable(ast.getRight().getType(), Environment.Type.BOOLEAN);
            ast.setType(Environment.Type.BOOLEAN);
        } else if (ast.getOperator().equals("<") || ast.getOperator().equals("<=") || ast.getOperator().equals(">") || ast.getOperator().equals(">=") || ast.getOperator().equals("==") || ast.getOperator().equals("!=")) {
            visit(ast.getLeft());
            requireAssignable(ast.getLeft().getType(), Environment.Type.COMPARABLE);
            visit(ast.getRight());
            requireAssignable(ast.getRight().getType(), Environment.Type.COMPARABLE);
            ast.setType(Environment.Type.BOOLEAN);
        } else if (ast.getOperator().equals("+")) {
            visit(ast.getLeft());
            visit(ast.getRight());
            if (ast.getLeft().getType().equals(Environment.Type.STRING) || ast.getRight().getType().equals(Environment.Type.STRING)) {
                ast.setType(Environment.Type.STRING);
            } else if (ast.getLeft().getType().equals(Environment.Type.INTEGER)) {
                if (ast.getRight().getType().equals(Environment.Type.INTEGER)) {
                    ast.setType(Environment.Type.INTEGER);
                } else {
                    throw new RuntimeException("RHS of binary expression is not an Integer.");
                }
            } else if (ast.getLeft().getType().equals(Environment.Type.DECIMAL)) {
                if (ast.getRight().getType().equals(Environment.Type.DECIMAL)) {
                    ast.setType(Environment.Type.DECIMAL);
                } else {
                    throw new RuntimeException("RHS of binary expression is not a Decimal.");
                }
            } else {
                throw new RuntimeException("Invalid binary expression.");
            }
        } else if (ast.getOperator().equals("-") || ast.getOperator().equals("*") || ast.getOperator().equals("/")) {
            visit(ast.getLeft());
            visit(ast.getRight());
            if (ast.getLeft().getType().equals(Environment.Type.INTEGER)) {
                if (ast.getRight().getType().equals(Environment.Type.INTEGER)) {
                    ast.setType(Environment.Type.INTEGER);
                } else {
                    throw new RuntimeException("RHS of binary expression is not an Integer.");
                }
            } else if (ast.getLeft().getType().equals(Environment.Type.DECIMAL)) {
                if (ast.getRight().getType().equals(Environment.Type.DECIMAL)) {
                    ast.setType(Environment.Type.DECIMAL);
                } else {
                    throw new RuntimeException("RHS of binary expression is not a Decimal.");
                }
            } else {
                throw new RuntimeException("Invalid binary expression.");
            }
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            ast.setVariable(ast.getReceiver().get().getType().getField(ast.getName()));
        } else {
            ast.setVariable(scope.lookupVariable(ast.getName()));
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            ast.setFunction(ast.getReceiver().get().getType().getMethod(ast.getName(), ast.getArguments().size()));
            for (int i = 0; i < ast.getArguments().size(); i++) {
                visit(ast.getArguments().get(i));
                requireAssignable(ast.getFunction().getParameterTypes().get(i + 1), ast.getArguments().get(i).getType());
            }
        } else {
            ast.setFunction(scope.lookupFunction(ast.getName(), ast.getArguments().size()));
            for (int i = 0; i < ast.getArguments().size(); i++) {
                visit(ast.getArguments().get(i));
                requireAssignable(ast.getFunction().getParameterTypes().get(i), ast.getArguments().get(i).getType());
            }
        }

        return null;
    } // TODO test

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (!target.equals(Environment.Type.ANY)) {
            if (target.equals(Environment.Type.COMPARABLE)) {
                if (!(type.equals(Environment.Type.INTEGER) || type.equals(Environment.Type.DECIMAL) || type.equals(Environment.Type.CHARACTER) || type.equals(Environment.Type.STRING))) {
                    throw new RuntimeException("Comparable target type does not match the given type.");
                }
            } else if (!target.equals(type)) {
                throw new RuntimeException("Target type and given type do not match.");
            }
        }
    }

}
