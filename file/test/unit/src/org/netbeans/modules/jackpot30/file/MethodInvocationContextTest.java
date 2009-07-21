package org.netbeans.modules.jackpot30.file;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;
import org.netbeans.modules.jackpot30.file.Condition.MethodInvocation.ParameterKind;

/**
 *
 * @author lahvac
 */
public class MethodInvocationContextTest {

    @Test
    public void testSetCode() {
        MethodInvocationContext mic = new MethodInvocationContext();

        mic.setCode("", Arrays.asList("public boolean test() {return false;}"));
        assertFalse(mic.invokeMethod(null, "test", Collections.<String, ParameterKind>emptyMap()));
    }

    @Test
    public void testPerformance() {
        MethodInvocationContext mic = new MethodInvocationContext();

        mic.setCode("", Collections.<String>emptyList());
        assertEquals(1, mic.ruleUtilities.size());
    }

}