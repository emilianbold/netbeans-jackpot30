/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009-2011 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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
 * Portions Copyrighted 2009-2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.jackpot30.cmdline;

import java.util.regex.Pattern;
import org.netbeans.modules.jackpot30.cmdline.lib.CreateStandaloneJar;
import org.netbeans.modules.jackpot30.cmdline.lib.CreateStandaloneJar.Info;
import org.netbeans.modules.java.hints.declarative.PatternConvertorImpl;
import org.netbeans.modules.java.hints.declarative.test.api.DeclarativeHintsTestBase;
import org.netbeans.modules.java.hints.jackpot.spi.PatternConvertor;
import org.netbeans.modules.java.j2seproject.J2SEProject;
import org.netbeans.modules.java.platform.DefaultJavaPlatformProvider;
import org.netbeans.modules.project.ui.OpenProjectsTrampolineImpl;

/**
 *
 * @author lahvac
 */
public class CreateTool extends CreateStandaloneJar {

    public CreateTool(String name) {
        super(name, "jackpot");
    }

    @Override
    protected Info computeInfo() {
        return new Info().addAdditionalRoots(Main.class.getName(), DeclarativeHintsTestBase.class.getName(), OpenProjectsTrampolineImpl.class.getName(), J2SEProject.class.getName(), DefaultJavaPlatformProvider.class.getName(), PatternConvertorImpl.class.getName())
                         .addAdditionalResources("org/netbeans/modules/java/hints/resources/Bundle.properties", "org/netbeans/modules/java/hints/declarative/resources/Bundle.properties")
                         .addAdditionalLayers("org/netbeans/modules/java/hints/resources/layer.xml", "org/netbeans/modules/java/hints/declarative/resources/layer.xml")
                         .addMetaInfRegistrations(new MetaInfRegistration(org.netbeans.modules.project.uiapi.OpenProjectsTrampoline.class, OpenProjectsTrampolineImpl.class))
                         .addMetaInfRegistrationToCopy(PatternConvertor.class.getName())
                         .addExcludePattern(Pattern.compile("junit\\.framework\\..*"))
                         .setEscapeJavaxLang();
    }

}
