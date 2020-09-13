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
package de.tudarmstadt.ukp.inception.workload.model;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.workload.config.WorkloadManagerAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.registry.WorkloadRegistry;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link WorkloadManagerAutoConfiguration#workloadManagementService}
 * </p>
 */
public class WorkloadManagementServiceImplBase implements WorkloadManagementService
{
    private final EntityManager entityManager;
    private final WorkloadRegistry workloadRegistry;

    @Autowired
    public WorkloadManagementServiceImplBase(
        EntityManager aEntityManager, WorkloadRegistry aWorkloadRegistry)
    {
        entityManager = aEntityManager;
        workloadRegistry = aWorkloadRegistry;
    }

    @Override
    @Transactional
    public WorkloadManager getOrCreateWorkloadManagerConfiguration(Project aProject) {
        try {
            WorkloadManager result = entityManager.createQuery(
                "SELECT wm " +
                    "FROM WorkloadManager wm " +
                    "WHERE wm.project = :projectID", WorkloadManager.class)
                .setParameter("projectID", aProject).getSingleResult();
            return result;
        } catch (NoResultException e) {
            return createDefaultEntry(aProject);
        }
    }

    @Override
    @Transactional
    public void setWorkloadManagerConfiguration(String aExtensionPointID, Project aProject) {
        entityManager.createQuery(
            "UPDATE WorkloadManager " +
                "SET workloadType = :extensionPointID " +
                "WHERE project = :projectID")
            .setParameter("extensionPointID", aExtensionPointID)
            .setParameter("projectID", aProject).executeUpdate();
    }

    @Override
    @Transactional
    public void setTraits(String aTraits, Project aProject) {
        entityManager.createQuery(
            "UPDATE WorkloadManager " +
                "SET traits = :traits " +
                "WHERE project = :projectID")
            .setParameter("traits", aTraits)
            .setParameter("projectID", aProject).executeUpdate();
    }

    private WorkloadManager createDefaultEntry(Project aProject)
    {
        entityManager.persist(new WorkloadManager(
            aProject,"Static Workload",null));
        WorkloadManager result = entityManager.createQuery(
            "SELECT wm " +
                "FROM WorkloadManager wm " +
                "WHERE wm.project = :projectID", WorkloadManager.class)
            .setParameter("projectID", aProject).getSingleResult();
        return result;
    }
}
