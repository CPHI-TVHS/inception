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

package de.tudarmstadt.ukp.inception.app.ui.monitoring.support;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.IFilterStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public class DataProvider extends SortableDataProvider
    <SourceDocument, String> implements IFilterStateLocator<Filter>, Serializable
{


    private static final long serialVersionUID = 4125678936105494485L;
    private final List<String> headers;
    private final List<SourceDocument> data;
    private final IModel<List<SourceDocument>> model;
    private Filter filter;

    private final DocumentSupport documentSupport;
    private String input;
    private String type;
    private Project project;

    private UserDao userRepository;
    private DocumentService documentService;


    public DataProvider(
        List<SourceDocument> aContents,
        List<String> headers, Project project)
    {
        this.project = project;
        this.data = aContents;
        this.headers = headers;
        this.documentSupport = new DocumentSupport(this.project,data);

        //Initialise beans with helper methods and class
        userRepository = documentSupport.getUserRepository();
        documentService = documentSupport.getDocumentService();

        //Init filter
        filter = new Filter();

        //Initial Sorting
        setSort(headers.get(0), SortOrder.ASCENDING);

        //Required
        model = new LoadableDetachableModel<List<SourceDocument>>() {
            private static final long serialVersionUID = -3938543310389673460L;

            @Override
            protected List<SourceDocument> load() {
                return aContents;
            }
        };


    }
    @Override
    public Iterator<SourceDocument> iterator(long first, long count)
    {
        List<SourceDocument> newList = data;

        input = filter.getInput();
        type = filter.getType();

        //Apply Filter
        //Filter only if initialised
        if (input != null && type != null)
        {
            newList = filterTable(data);
        }


        //Apply sorting
        newList.sort((o1, o2) ->
        {
            int dir = getSort().isAscending() ? 1 : -1;

            if (getSort().getProperty().equals(headers.get(0)))
            {
                return dir * (o1.getName().compareTo(o2.getName()));

            } else if (getSort().getProperty().equals(headers.get(1)))
            {
                return dir * Integer.compare(documentSupport.
                    getFinishedAmountForDocument(o1), documentSupport.
                    getFinishedAmountForDocument(o2));

            } else if (getSort().getProperty().equals(headers.get(2)))
            {
                return dir * Integer.compare(documentSupport.
                    getInProgressAmountForDocument(o1), documentSupport.
                    getInProgressAmountForDocument(o2));


            } else {
                return 0;
            }
        });

        return newList.subList(0, newList.size()).iterator();
    }

    @Override
    public long size()
    {
        return data.size();
    }

    @Override
    public IModel<SourceDocument> model(SourceDocument sourceDocument)
    {
        return Model.of(sourceDocument);
    }

    @Override
    public void detach()
    {
        super.detach();
        model.detach();

    }

    @Override
    public Filter getFilterState() {
        return filter;
    }

    @Override
    public void setFilterState(Filter filter) {
        this.filter = filter;
    }

    public List<SourceDocument> filterTable(List<SourceDocument> data)
    {
        List<SourceDocument> resultList = new ArrayList<>();

        //No filter selected
        if (this.type.equals("None"))
        {
            return data;
        }


        for (SourceDocument doc: data)
        {
            //Creation time filter
            if (this.type.equals("Document creation time:"))
            {
                try {
                    //TODO Rework of try catch and add Error (or regex for field input)
                    Date date = new SimpleDateFormat("dd/MM/yyyy").parse(input);
                    if (doc.getCreated().compareTo(date) >= 0)
                    {
                        resultList.add(doc);
                        continue;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }


            //User filter
            } else if (this.type.equals("User:"))
            {
                //Get the current user
                User user = userRepository.get(this.input);

                //The entered username is not available, simply return the full list
                if (user == null)
                {
                    resultList = data;
                    break;

                } else {
                    //If the user is working / has finished that document, add it to the list
                    if (documentService.existsAnnotationDocument(doc, user))
                    {
                        resultList.add(doc);
                    }
                }

            //Unused Document filter used
            } else if (this.type.equals("Unused documents:"))
            {
                System.out.println("Checking");
                if ((documentSupport.getInProgressAmountForDocument(doc) == 0)
                    && (documentSupport.getFinishedAmountForDocument(doc) == 0))
                {
                    resultList.add(doc);
                }
            } else {
                //Default case to avoid unexpected errors, simply return the list
                resultList = data;
                break;
            }
        }
        return resultList;
    }
}
