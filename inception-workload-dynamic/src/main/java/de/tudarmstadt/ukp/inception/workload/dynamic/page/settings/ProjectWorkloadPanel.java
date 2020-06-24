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

package de.tudarmstadt.ukp.inception.workload.dynamic.page.settings;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.workload.dynamic.manager.WorkflowProperties;
import de.tudarmstadt.ukp.inception.workload.dynamic.manager.WorkloadProperties;

//Overall new Panel
public class ProjectWorkloadPanel extends Panel
{
    private @Autowired WorkloadProperties workloadProperties;
    private @Autowired WorkflowProperties workflowProperties;

    public ProjectWorkloadPanel(String aID, IModel<Project> aProject)
    {
        super(aID, aProject);

        //Add custom panel window
        add(new WorkflowAndMonitoringPanel("workflow",aProject));
    }
}
