/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2007 Sun Microsystems, Inc.
 */
package org.netbeans.modules.jackpot30.backend.ui.highlighting;

import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.jackpot30.resolve.api.CompilationInfo;

/**
 *
 * @author Jan Lahoda
 */
public class TokenList {

    private CompilationInfo info;
    private SourcePositions sourcePositions;
    private AtomicBoolean cancel;

    private TokenSequence ts;
        
    public TokenList(CompilationInfo info, TokenSequence<?> topLevel, AtomicBoolean cancel) {
        this.info = info;
        this.cancel = cancel;
        
        this.sourcePositions = info.getTrees().getSourcePositions();
        
                if (TokenList.this.cancel.get())
                    return ;
                
                assert topLevel.language() == JavaTokenId.language();
                
                    ts = topLevel;
                    ts.moveStart();
                    ts.moveNext(); //XXX: what about empty document
    }
    
    public void moveToOffset(long inputOffset) {
        final int offset = (int) inputOffset;

        if (offset < 0)
            return ;
                if (cancel.get())
                    return ;
                
                if (ts != null && !ts.isValid()) {
                    cancel.set(true);
                    return ;
                }

                    while (ts.offset() < offset) {
                        if (!ts.moveNext())
                            return ;
                    }
    }

    public void moveToEnd(Tree t) {
        if (t == null)
            return ;

        long end = sourcePositions.getEndPosition(info.getCompilationUnit(), t);

        if (end == (-1))
            return ;

        if (t.getKind() == Kind.ARRAY_TYPE) {
            moveToEnd(((ArrayTypeTree) t).getType());
            return ;
        }
        moveToOffset(end);
    }

    public void moveToEnd(Collection<? extends Tree> trees) {
        if (trees == null)
            return ;

        for (Tree t : trees) {
            moveToEnd(t);
        }
    }

    public void firstIdentifier(final TreePath tp, final String name, final Map<Tree, Token> tree2Token) {
                if (cancel.get())
                    return ;
                
                if (ts != null && !ts.isValid()) {
                    cancel.set(true);
                    return ;
                }
                
                if (ts == null)
                    return ;
                
                boolean next = true;

                while (ts.token().id() != JavaTokenId.IDENTIFIER && (next = ts.moveNext()))
                    ;

                if (next) {
                    if (name.equals(ts.token().text().toString())) {
                        tree2Token.put(tp.getLeaf(), ts.token());
                    } else {
//                            System.err.println("looking for: " + name + ", not found");
                    }
                }
    }

    public void identifierHere(final IdentifierTree tree, final Map<Tree, Token> tree2Token) {
                if (cancel.get())
                    return ;
                
                if (ts != null && !ts.isValid()) {
                    cancel.set(true);
                    return ;
                }
                
                if (ts == null)
                    return ;
                
                Token t = ts.token();

                if (t.id() == JavaTokenId.IDENTIFIER && tree.getName().toString().equals(t.text().toString())) {
    //                System.err.println("visit ident 1");
                    tree2Token.put(tree, ts.token());
                } else {
    //                System.err.println("visit ident 2");
                }
    }
    
    public void moveBefore(final List<? extends Tree> tArgs) {
                if (cancel.get())
                    return ;
                
                if (ts != null && !ts.isValid()) {
                    cancel.set(true);
                    return ;
                }
                
                if (ts == null)
                    return ;
                
                if (!tArgs.isEmpty()) {
                    int offset = (int) info.getTrees().getSourcePositions().getStartPosition(info.getCompilationUnit(), tArgs.get(0));
                    
                    if (offset < 0)
                        return ;
                    
                    while (ts.offset() >= offset) {
                        if (!ts.movePrevious()) {
                            return;
                        }
                    }
                }
    }

    public void moveNext() {
                if (cancel.get())
                    return ;
                
                if (ts != null && !ts.isValid()) {
                    cancel.set(true);
                    return ;
                }
                
                if (ts == null)
                    return ;
                
                ts.moveNext();
    }
    
}
