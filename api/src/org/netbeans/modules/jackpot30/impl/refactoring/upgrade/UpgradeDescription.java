/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.impl.refactoring.upgrade;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codeviation.pojson.Pojson;
import org.codeviation.pojson.Pojson.SuppressStoring;
import org.netbeans.modules.java.hints.jackpot.impl.RulesManager;
import org.netbeans.modules.java.hints.jackpot.spi.HintDescription;
import org.netbeans.modules.java.hints.jackpot.spi.HintMetadata;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;

/**
 *
 * @author lahvac
 */
public class UpgradeDescription {

    @SuppressStoring
    private final FileObject sourceFile;
    private final String upgradeName;
    private final String bundle;
    private final Collection<String> hints;
    private final Collection<String> disabledHints;

    private UpgradeDescription(FileObject sourceFile) {
        this.sourceFile = sourceFile;
        this.hints = new ArrayList<String>();
        this.disabledHints = new ArrayList<String>();
        this.upgradeName = null;
        this.bundle = null;
    }

    String getBundle() {
        return bundle;
    }

    String getUpgradeName() {
        return upgradeName;
    }

    Collection< String> getHintIds() {
        return hints;
    }

    public String getDisplayName() {
        ResourceBundle b = NbBundle.getBundle(bundle);

        return b.getString("DN_" + upgradeName);
    }

    public Iterable<? extends HintDescription> getEnabledHints() {
        List<HintDescription> result = new LinkedList<HintDescription>();
        Set<String> hints = new HashSet<String>(this.hints);

        for (Entry<HintMetadata, Collection<? extends HintDescription>> e : RulesManager.getInstance().allHints.entrySet()) {
            if (hints.contains(e.getKey().id)) {
                result.addAll(e.getValue());
            }
        }

        return result;
    }

    Map<HintMetadata, Boolean> getAllHints() {
        Map<HintMetadata, Boolean> result = new LinkedHashMap<HintMetadata, Boolean>();
        Set<String> hints = new HashSet<String>(this.hints);
        Set<String> disabledHints = new HashSet<String>(this.disabledHints);

        for (Entry<HintMetadata, Collection<? extends HintDescription>> e : RulesManager.getInstance().allHints.entrySet()) {
            if (hints.contains(e.getKey().id)) {
                result.put(e.getKey(), true);
            }
            if (disabledHints.contains(e.getKey().id)) {
                result.put(e.getKey(), false);
            }
        }

        return result;
    }

    void putHintStates(Map<HintMetadata, Boolean> enabled) {
        this.hints.clear();
        this.disabledHints.clear();

        for (Entry<HintMetadata, Boolean> e : enabled.entrySet()) {
            if (e.getValue()) {
                this.hints.add(e.getKey().id);
            } else {
                this.disabledHints.add(e.getKey().id);
            }
        }
    }

    void store() {
        OutputStream outs = null;

        try {
            outs = sourceFile.getOutputStream();
            Pojson.save(this, outs);
        } catch (IOException ex) {
            Logger.getLogger(UpgradeDescription.class.getName()).log(Level.FINE, null, ex);
        } finally {
            try {
                outs.close();
            } catch (IOException ex) {
                Logger.getLogger(UpgradeDescription.class.getName()).log(Level.FINE, null, ex);
            }
        }
    }

    public static UpgradeDescription create(FileObject file) {
        InputStream ins = null;

        try {
            ins = file.getInputStream();
            return Pojson.update(new UpgradeDescription(file), ins);
        } catch (IOException ex) {
            Logger.getLogger(UpgradeDescription.class.getName()).log(Level.FINE, null, ex);
            return null;
        } finally {
            try {
                ins.close();
            } catch (IOException ex) {
                Logger.getLogger(UpgradeDescription.class.getName()).log(Level.FINE, null, ex);
            }
        }
    }

    public static Iterable<? extends UpgradeDescription> create() {
        FileObject upgradeDir = FileUtil.getConfigFile("org-netbeans-modules-java-hints/upgrades");

        if (upgradeDir == null) return Collections.<UpgradeDescription>emptyList();

        List<UpgradeDescription> result = new LinkedList<UpgradeDescription>();

        for (FileObject file : upgradeDir.getChildren()) {
            if ("upgrade".equals(file.getExt())) {
                result.add(create(file));
            }
        }

        return result;
    }
}
