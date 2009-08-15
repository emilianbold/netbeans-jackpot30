package org.netbeans.modules.jackpot30.impl;

import org.netbeans.modules.jackpot30.spi.HintContext.MessageKind;

/**
 *
 * @author lahvacc
 */
public class MessageImpl {

    public final MessageKind kind;
    public final String text;

    public MessageImpl(MessageKind kind, String text) {
        this.kind = kind;
        this.text = text;
    }

}
