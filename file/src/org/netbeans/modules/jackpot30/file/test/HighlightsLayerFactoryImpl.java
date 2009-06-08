package org.netbeans.modules.jackpot30.file.test;

import org.netbeans.spi.editor.highlighting.HighlightsLayer;
import org.netbeans.spi.editor.highlighting.HighlightsLayerFactory;
import org.netbeans.spi.editor.highlighting.ZOrder;

/**
 *
 * @author lahvac
 */
public class HighlightsLayerFactoryImpl implements HighlightsLayerFactory {

    public HighlightsLayer[] createLayers(Context context) {
        return new HighlightsLayer[] {
            HighlightsLayer.create(HighlightsLayerFactoryImpl.class.getName(), ZOrder.DEFAULT_RACK, true, EditorTestPerformer.getBag(context.getDocument())),
        };
    }

}
