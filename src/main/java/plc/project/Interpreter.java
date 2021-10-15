package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        for (Ast.Field field : ast.getFields()) {
            visit(field);
        }
        for (Ast.Method method : ast.getMethods()) {
            visit(method);
        }

        return scope.lookupFunction("main", 0).invoke(null);
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), visit(ast.getValue().get()));
        } else {
            scope.defineVariable(ast.getName(), Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            scope = new Scope(scope);
            int argIndex = 0;
            for (String param : ast.getParameters()) {
                scope.defineVariable(param, args.get(argIndex));
                argIndex++;
            }
            for (Ast.Stmt stmt : ast.getStatements()) {
                try {
                    visit(stmt);
                } catch (Return r) {
                    scope = scope.getParent();
                    return r.value;
                }
            }
            scope = scope.getParent();
            return Environment.NIL;
        });
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Declaration ast) {
        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), visit(ast.getValue().get()));
        } else {
            scope.defineVariable(ast.getName(), Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Assignment ast) {
        if (ast.getReceiver() instanceof Ast.Expr.Access) {
            Ast.Expr.Access access = (Ast.Expr.Access) ast.getReceiver();
            if (access.getReceiver().isPresent()) {
                visit(access.getReceiver().get()).setField(access.getName(), visit(ast.getValue()));
            } else {
                scope.lookupVariable(access.getName()).setValue(visit(ast.getValue()));
            }
        } else {
            throw new RuntimeException("Expected a receiver of type Ast.Expr.Access.");
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.If ast) {
        if (requireType(Boolean.class, visit(ast.getCondition())).equals(true)) {
            try {
                scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getThenStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        } else if (requireType(Boolean.class, visit(ast.getCondition())).equals(false)) {
            try {
                scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getElseStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.For ast) {
        if (requireType(Iterable.class, visit(ast.getValue())) instanceof Iterable) {
            Iterable<Environment.PlcObject> iterable = requireType(Iterable.class, visit(ast.getValue()));
            iterable.forEach( (obj) -> {
                try {
                    scope = new Scope(scope);
                    scope.defineVariable(ast.getName(), obj);
                    for (Ast.Stmt stmt : ast.getStatements()) {
                        visit(stmt);
                    }
                } finally {
                    scope = scope.getParent();
                }
            });
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.While ast) {
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                // fancy java ==> ast.getStatements().forEach(this::visit);
                for (Ast.Stmt stmt : ast.getStatements()) {
                    visit(stmt);
                }
            } finally {
                // return to the original scope
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Return ast) {       //TODO
        throw new Return(visit(ast.getValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Literal ast) {
        if (ast.getLiteral() == null) {
            return Environment.NIL;
        }
        return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Binary ast) {
        String op = ast.getOperator();
        if (op.equals("OR")) {
            if (requireType(Boolean.class, visit(ast.getLeft()))) {
                return Environment.create(true);
            } else if (requireType(Boolean.class, visit(ast.getRight()))) {
                return Environment.create(true);
            } else {
                return Environment.create(false);
            }
        } else if (op.equals("AND")) {
            if (requireType(Boolean.class, visit(ast.getLeft()))) {
                if (requireType(Boolean.class, visit(ast.getRight()))) {
                    return Environment.create(true);
                } else {
                    return Environment.create(false);
                }
            } else {
                return Environment.create(false);
            }
        } else if (op.equals("+")) {
            if ((visit(ast.getLeft()).getValue() instanceof String) || (visit(ast.getRight()).getValue() instanceof String)) {
                return Environment.create(requireType(String.class, visit(ast.getLeft())) + requireType(String.class, visit(ast.getRight())));
            } else if (visit(ast.getLeft()).getValue() instanceof BigInteger) {
                if (visit(ast.getRight()).getValue() instanceof BigInteger) {
                    return Environment.create(requireType(BigInteger.class, visit(ast.getLeft())).add(requireType(BigInteger.class, visit(ast.getRight()))));
                } else {
                    throw new RuntimeException("Expected right operand to be a BigInteger" + ", received " + visit(ast.getRight()).getValue().getClass().getName() + ".");
                }
            } else if (visit(ast.getLeft()).getValue() instanceof BigDecimal) {
                if (visit(ast.getRight()).getValue() instanceof BigDecimal) {
                    return Environment.create(requireType(BigDecimal.class, visit(ast.getLeft())).add(requireType(BigDecimal.class, visit(ast.getRight()))));
                } else {
                    throw new RuntimeException("Expected right operand to be a BigDecimal" + ", received " + visit(ast.getRight()).getValue().getClass().getName() + ".");
                }
            } else {
                throw new RuntimeException("Error with types being added.");
            }
        } else if (op.equals("-")) {
            if (visit(ast.getLeft()).getValue() instanceof BigInteger) {
                if (visit(ast.getRight()).getValue() instanceof BigInteger) {
                    return Environment.create(requireType(BigInteger.class, visit(ast.getLeft())).subtract(requireType(BigInteger.class, visit(ast.getRight()))));
                } else {
                    throw new RuntimeException("Expected right operand to be a BigInteger" + ", received " + visit(ast.getRight()).getValue().getClass().getName() + ".");
                }
            } else if (visit(ast.getLeft()).getValue() instanceof BigDecimal) {
                if (visit(ast.getRight()).getValue() instanceof BigDecimal) {
                    return Environment.create(requireType(BigDecimal.class, visit(ast.getLeft())).subtract(requireType(BigDecimal.class, visit(ast.getRight()))));
                } else {
                    throw new RuntimeException("Expected right operand to be a BigDecimal" + ", received " + visit(ast.getRight()).getValue().getClass().getName() + ".");
                }
            } else {
                throw new RuntimeException("Error with types being subtracted.");
            }
        } else if (op.equals("*")) {
            if (visit(ast.getLeft()).getValue() instanceof BigInteger) {
                if (visit(ast.getRight()).getValue() instanceof BigInteger) {
                    return Environment.create(requireType(BigInteger.class, visit(ast.getLeft())).multiply(requireType(BigInteger.class, visit(ast.getRight()))));
                } else {
                    throw new RuntimeException("Expected right operand to be a BigInteger" + ", received " + visit(ast.getRight()).getValue().getClass().getName() + ".");
                }
            } else if (visit(ast.getLeft()).getValue() instanceof BigDecimal) {
                if (visit(ast.getRight()).getValue() instanceof BigDecimal) {
                    return Environment.create(requireType(BigDecimal.class, visit(ast.getLeft())).multiply(requireType(BigDecimal.class, visit(ast.getRight()))));
                } else {
                    throw new RuntimeException("Expected right operand to be a BigDecimal" + ", received " + visit(ast.getRight()).getValue().getClass().getName() + ".");
                }
            } else {
                throw new RuntimeException("Error with types being multiplied.");
            }
        } else if (op.equals("/")) {
            if (visit(ast.getLeft()).getValue() instanceof BigInteger) {
                if (visit(ast.getRight()).getValue() instanceof BigInteger) {
                    if (visit(ast.getRight()).getValue().equals(0)) {
                        throw new RuntimeException("Cannot divide by 0.");
                    } else {
                        return Environment.create(requireType(BigInteger.class, visit(ast.getLeft())).divide(requireType(BigInteger.class, visit(ast.getRight()))));
                    }
                } else {
                    throw new RuntimeException("Expected right operand to be a BigInteger" + ", received " + visit(ast.getRight()).getValue().getClass().getName() + ".");
                }
            } else if (visit(ast.getLeft()).getValue() instanceof BigDecimal) {
                if (visit(ast.getRight()).getValue() instanceof BigDecimal) {
                    if (visit(ast.getRight()).getValue().equals(0.0)) {
                        throw new RuntimeException("Cannot divide by 0.0.");
                    } else {
                        return Environment.create(requireType(BigDecimal.class, visit(ast.getLeft())).divide(requireType(BigDecimal.class, visit(ast.getRight())), RoundingMode.HALF_EVEN));
                    }
                } else {
                    throw new RuntimeException("Expected right operand to be a BigDecimal" + ", received " + visit(ast.getRight()).getValue().getClass().getName() + ".");
                }
            } else {
                throw new RuntimeException("Error with types being divided.");
            }
        } else if (op.equals("==")) {
            if (visit(ast.getLeft()).getValue().equals(visit(ast.getRight()).getValue())) {
                return Environment.create(true);
            } else {
                return Environment.create(false);
            }
        } else if (op.equals("!=")) {
            if (visit(ast.getLeft()).getValue().equals(visit(ast.getRight()).getValue())) {
                return Environment.create(false);
            } else {
                return Environment.create(true);
            }
        } else if (op.equals("<")) {
            if (visit(ast.getLeft()).getValue() instanceof Comparable) {
                if (visit(ast.getRight()).getValue() instanceof Comparable) {
                    if (((Comparable<Object>) visit(ast.getLeft()).getValue()).compareTo(visit(ast.getRight()).getValue()) < 0) {
                        return Environment.create(true);
                    } else {
                        return Environment.create(false);
                    }
                } else {
                    throw new RuntimeException("right hand side is not comparable.");
                }
            } else {
                throw new RuntimeException("left hand side is not comparable.");
            }
        } else if (op.equals(">")) {
            if (visit(ast.getLeft()).getValue() instanceof Comparable) {
                if (visit(ast.getRight()).getValue() instanceof Comparable) {
                    if (((Comparable<Object>) visit(ast.getLeft()).getValue()).compareTo(visit(ast.getRight()).getValue()) > 0) {
                        return Environment.create(true);
                    } else {
                        return Environment.create(false);
                    }
                } else {
                    throw new RuntimeException("right hand side is not comparable.");
                }
            } else {
                throw new RuntimeException("left hand side is not comparable.");
            }
        } else if (op.equals("<=")) {
            if (visit(ast.getLeft()).getValue() instanceof Comparable) {
                if (visit(ast.getRight()).getValue() instanceof Comparable) {
                    if (((Comparable<Object>) visit(ast.getLeft()).getValue()).compareTo(visit(ast.getRight()).getValue()) <= 0) {
                        return Environment.create(true);
                    } else {
                        return Environment.create(false);
                    }
                } else {
                    throw new RuntimeException("right hand side is not comparable.");
                }
            } else {
                throw new RuntimeException("left hand side is not comparable.");
            }
        } else if (op.equals(">=")) {
            if (visit(ast.getLeft()).getValue() instanceof Comparable) {
                if (visit(ast.getRight()).getValue() instanceof Comparable) {
                    if (((Comparable<Object>) visit(ast.getLeft()).getValue()).compareTo(visit(ast.getRight()).getValue()) >= 0) {
                        return Environment.create(true);
                    } else {
                        return Environment.create(false);
                    }
                } else {
                    throw new RuntimeException("right hand side is not comparable.");
                }
            } else {
                throw new RuntimeException("left hand side is not comparable.");
            }
        } else {
            throw new RuntimeException("Error, invalid binary expression.");
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Access ast) {
        if (ast.getReceiver().isPresent()) {
            return visit(ast.getReceiver().get()).getField(ast.getName()).getValue();
        } else {
            return scope.lookupVariable(ast.getName()).getValue();
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Function ast) {
        List<Environment.PlcObject> args = new ArrayList<>();
        for (Ast.Expr expr : ast.getArguments()) {
            args.add(visit(expr));
        }

        if (ast.getReceiver().isPresent()) {
            return visit(ast.getReceiver().get()).callMethod(ast.getName(), args);
        } else {
            return scope.lookupFunction(ast.getName(), ast.getArguments().size()).invoke(args);
        }
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
