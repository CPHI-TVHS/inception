/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tudarmstadt.ukp.inception.workload.dynamic.support;

import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;


public class ModalPanel extends Panel
{

    private static final long serialVersionUID = 2797336810690526392L;

    public ModalPanel(String aID, SourceDocument aDocument,
                      List<String> finshedUsers, List<String> inProgressUser)
    {
        super(aID);

        if (finshedUsers.size() == 0)
        {
            finshedUsers.add("-");
        }

        if (inProgressUser.size() == 0)
        {
            inProgressUser.add("-");
        }


        //TODO more content
        Label documentName = new Label("documentName", "Document name: "
            + aDocument.getName());
        Label userInProgress = new Label("userInProgress", "Users working on the Document: "
            + inProgressUser);
        Label userFinished = new Label("userFinished", "Users finished the document: "
            + finshedUsers);

        add(documentName);
        add(userInProgress);
        add(userFinished);

    }
}
