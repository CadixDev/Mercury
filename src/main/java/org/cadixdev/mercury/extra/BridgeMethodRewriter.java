/*
 * Copyright (c) 2018 Minecrell (https://github.com/Minecrell)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.cadixdev.mercury.extra;

import org.cadixdev.mercury.RewriteContext;
import org.cadixdev.mercury.SourceRewriter;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import java.util.List;
import java.util.Optional;

/**
 * Removes redundant synthetic bridge methods that cause compile errors.
 */
public final class BridgeMethodRewriter implements SourceRewriter {

    private static final SourceRewriter INSTANCE = new BridgeMethodRewriter();

    public static SourceRewriter create() {
        return INSTANCE;
    }

    private BridgeMethodRewriter() {
    }

    @Override
    public int getFlags() {
        return FLAG_RESOLVE_BINDINGS;
    }

    @Override
    public void rewrite(RewriteContext context) {
        context.getCompilationUnit().accept(new Visitor(context));
    }

    private static class Visitor extends ASTVisitor {

        private final RewriteContext context;

        private Visitor(RewriteContext context) {
            this.context = context;
        }

        private static IMethodBinding findBridgedMethod(MethodDeclaration node) {
            Block body = node.getBody();
            if (body == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            List<Statement> statements = body.statements();
            if (statements.size() != 1) {
                return null;
            }

            Statement statement = statements.get(0);
            Expression expression;

            switch (statement.getNodeType()) {
                case ASTNode.EXPRESSION_STATEMENT:
                    expression = ((ExpressionStatement) statement).getExpression();
                    break;
                case ASTNode.RETURN_STATEMENT:
                    expression = ((ReturnStatement) statement).getExpression();
                    break;
                default:
                    return null;
            }

            if (expression == null || expression.getNodeType() != ASTNode.METHOD_INVOCATION) {
                return null;
            }

            MethodInvocation invocation = (MethodInvocation) expression;
            expression = invocation.getExpression();
            if (expression == null || expression.getNodeType() != ASTNode.THIS_EXPRESSION) {
                return null;
            }

            if (((ThisExpression) expression).getQualifier() != null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            List<Expression> arguments = invocation.arguments();
            if (arguments.size() != node.parameters().size()) {
                return null;
            }

            for (Expression arg : arguments) {
                switch (arg.getNodeType()) {
                    case ASTNode.SIMPLE_NAME:
                        continue;
                    case ASTNode.CAST_EXPRESSION:
                        if (((CastExpression) arg).getExpression().getNodeType() == ASTNode.SIMPLE_NAME) {
                            continue;
                        }
                    default:
                        return null;
                }
            }

            return invocation.resolveMethodBinding().getMethodDeclaration();
        }

        @Override
        public boolean visit(MethodDeclaration node) {
            IMethodBinding bridged = findBridgedMethod(node);
            if (bridged == null) {
                return true;
            }

            MethodDeclaration other = (MethodDeclaration) this.context.getCompilationUnit().findDeclaringNode(bridged);
            if (other == null) {
                return true;
            }

            Optional<ASTRewrite> rewrite = this.context.getASTRewrite();
            String name = getIdentifier(node.getName(), rewrite);
            String otherName = getIdentifier(other.getName(), rewrite);
            if (!name.equals(otherName)) {
                return true;
            }

            // Check if the two methods would clash (due to same parameter types)
            IMethodBinding binding = node.resolveBinding();
            ITypeBinding[] myTypes = binding.getParameterTypes();
            ITypeBinding[] otherTypes = bridged.getParameterTypes();
            if (myTypes.length != otherTypes.length) {
                return true;
            }

            for (int i = 0; i < myTypes.length; i++) {
                if (!myTypes[i].getErasure().isEqualTo(otherTypes[i].getErasure())) {
                    return true;
                }
            }

            // Remove the bridge method
            this.context.createASTRewrite().remove(node, null);

            return true;
        }

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private static String getIdentifier(SimpleName name, Optional<ASTRewrite> rewrite) {
            return rewrite.map(r -> (String) r.get(name, SimpleName.IDENTIFIER_PROPERTY)).orElseGet(name::getIdentifier);
        }

    }

}
