package org.netbeans.modules.jackpot30.file.conditionapi;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;

/**
 *
 * @author lahvac
 */
public final class DefaultRuleUtilities {

    private final Context context;
    private final Matcher matcher;

    DefaultRuleUtilities(Context context, Matcher matcher) {
        this.context = context;
        this.matcher = matcher;
    }
    
    public boolean referencedIn(Variable variable, Variable in) {
        return matcher.referencedIn(variable, in);
    }

    public boolean sourceVersionGE(SourceVersion source) {
        return context.sourceVersion().compareTo(source) >= 0;
    }

    public boolean hasModifier(Variable variable, Modifier modifier) {
        return context.modifiers(variable).contains(modifier);
    }

    public boolean parentMatches(String pattern) {
        Variable parent = context.parent(context.variableForName("$_"));
        
        if (parent == null) {
            return false;
        }
        
        return matcher.matches(parent, pattern); //XXX: $_ currently not part of variables map, so this won't work!!!
    }
}
