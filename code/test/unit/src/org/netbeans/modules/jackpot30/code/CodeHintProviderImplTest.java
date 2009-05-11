package org.netbeans.modules.jackpot30.code;

import com.sun.source.tree.Tree.Kind;
import java.util.Collection;
import java.util.Iterator;
import org.junit.Test;
import org.netbeans.modules.jackpot30.code.CodeHintProviderImpl.WorkerImpl;
import org.netbeans.modules.jackpot30.code.spi.Constraint;
import org.netbeans.modules.jackpot30.code.spi.Hint;
import org.netbeans.modules.jackpot30.code.spi.TriggerPattern;
import org.netbeans.modules.jackpot30.code.spi.TriggerTreeKind;
import org.netbeans.modules.jackpot30.spi.HintContext;
import static org.junit.Assert.*;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.PatternDescription;
import org.netbeans.spi.editor.hints.ErrorDescription;

/**
 *
 * @author lahvac
 */
public class CodeHintProviderImplTest {

    public CodeHintProviderImplTest() {
    }

    @Test
    public void testComputeHints() {
        Collection<? extends HintDescription> hints = new CodeHintProviderImpl().computeHints();

        assertEquals(2, hints.size());

        Iterator<? extends HintDescription> it = hints.iterator();

        assertEquals("null:$1.toURL():public static org.netbeans.spi.editor.hints.ErrorDescription org.netbeans.modules.jackpot30.code.CodeHintProviderImplTest.hintPattern1(org.netbeans.modules.jackpot30.spi.HintContext)", toString(it.next()));
        assertEquals("METHOD_INVOCATION:null:public static org.netbeans.spi.editor.hints.ErrorDescription org.netbeans.modules.jackpot30.code.CodeHintProviderImplTest.hintPattern2(org.netbeans.modules.jackpot30.spi.HintContext)", toString(it.next()));
    }

    private static String toString(HintDescription hd) {
        StringBuilder sb = new StringBuilder();

        sb.append(hd.getTriggerKind());
        sb.append(":");
        
        PatternDescription p = hd.getTriggerPattern();

        sb.append(p != null ? p.getPattern() : "null");
        //TODO: constraints
        sb.append(":");
        sb.append(((WorkerImpl) hd.getWorker()).getMethod().toGenericString());

        return sb.toString();
    }

    @Hint("hintPattern1")
    @TriggerPattern(value="$1.toURL()", constraints=@Constraint(variable="$1", type="java.io.File"))
    public static ErrorDescription hintPattern1(HintContext ctx) {
        return null;
    }

    @Hint("hintPattern2")
    @TriggerTreeKind(Kind.METHOD_INVOCATION)
    public static ErrorDescription hintPattern2(HintContext ctx) {
        return null;
    }

}