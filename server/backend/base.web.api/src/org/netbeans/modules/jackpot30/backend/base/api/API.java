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

package org.netbeans.modules.jackpot30.backend.base.api;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.netbeans.modules.jackpot30.backend.base.CategoryStorage;
import org.netbeans.modules.java.source.usages.ClassIndexImpl;
import org.netbeans.modules.java.source.usages.ClassIndexManager;

/**
 *
 * @author lahvac
 */
@Path("/index")
public class API {

    @GET
    @Path("/list")
    @Produces("text/plain")
    public String list() throws IOException {
        StringBuilder sb = new StringBuilder();

        for (CategoryStorage c : CategoryStorage.listCategories()) {
            sb.append(c.getId());
            sb.append(":");
            sb.append(c.getDisplayName());
            sb.append("\n");
        }

        return sb.toString();
    }

    @GET
    @Path("/internal/indexUpdated")
    @Produces("test/plain")
    public String indexUpdated() throws IOException {
        //XXX: should allow individual providers to do their own cleanup:
        //XXX: synchronize with the queries!
        //XXX: well, still does not work!

        try {
            for (String name : Arrays.asList("instances", "transientInstances")) {
                Field instances = ClassIndexManager.class.getDeclaredField(name);

                instances.setAccessible(true);

                Map<?, ClassIndexImpl> toClear = (Map<?, ClassIndexImpl>) instances.get(ClassIndexManager.getDefault());

                for (ClassIndexImpl impl : toClear.values()) {
                    Method close = ClassIndexImpl.class.getDeclaredMethod("close");

                    close.setAccessible(true);
                    close.invoke(impl);
                }

                toClear.clear();
            }
        } catch (InvocationTargetException ex) {
            Logger.getLogger(API.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(API.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(API.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(API.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(API.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(API.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return "Done";
    }

}
