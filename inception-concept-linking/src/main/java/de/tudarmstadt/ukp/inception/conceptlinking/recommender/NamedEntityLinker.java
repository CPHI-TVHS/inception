/*
 * Copyright 2018
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

package de.tudarmstadt.ukp.inception.conceptlinking.recommender;

import static org.apache.uima.fit.util.CasUtil.getAnnotationType;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.indexCovered;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingService;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.type.PredictedSpan;
import de.tudarmstadt.ukp.inception.recommendation.api.v2.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.v2.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.v2.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.v2.RecommenderContext.Key;

public class NamedEntityLinker
    implements RecommendationEngine
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Recommender recommender;

    private KnowledgeBaseService kbService;
    private ConceptLinkingService clService;
    private AnnotationSchemaService annoService;
    private FeatureSupportRegistry fsRegistry;

    private int numPredictions = 3;

    private static final Key<Collection<ImmutablePair<String, Collection<AnnotationFS>>>> KEY_MODEL
        = new Key<>("model");

    public NamedEntityLinker(Recommender aRecommender, KnowledgeBaseService aKbService,
        ConceptLinkingService aClService, AnnotationSchemaService aAnnoService,
        FeatureSupportRegistry aFsRegistry)
    {
        recommender = aRecommender;
        kbService = aKbService;
        clService = aClService;
        annoService = aAnnoService;
        fsRegistry = aFsRegistry;
    }

    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasList)
    {
        Collection<ImmutablePair<String, Collection<AnnotationFS>>> nameSamples =
            extractNamedEntities(aCasList);
        aContext.put(KEY_MODEL, nameSamples);
    }

    private Collection<ImmutablePair<String, Collection<AnnotationFS>>> extractNamedEntities(
        List<CAS> aCasList)
    {
        Type tokenType = org.apache.uima.fit.util.CasUtil
            .getType(aCasList.get(0), recommender.getLayer().getName());
        Feature feature = tokenType.getFeatureByBaseName(recommender.getFeature());

        Collection<ImmutablePair<String, Collection<AnnotationFS>>> nameSamples = new HashSet<>();
        for (CAS cas : aCasList) {
            Collection<AnnotationFS> namesPerDocument = new ArrayList<>();
            Type sentenceType = getType(cas, Sentence.class);

            Map<AnnotationFS, Collection<AnnotationFS>> sentences = indexCovered(cas, sentenceType,
                tokenType);
            for (Map.Entry<AnnotationFS, Collection<AnnotationFS>> e : sentences.entrySet()) {
                Collection<AnnotationFS> tokens = e.getValue().stream()
                    .filter(a->a.getFeatureValue(feature) != null)  //TODO maybe it must be hardcoded to "value"
                    .collect(Collectors.toSet());
                namesPerDocument.addAll(tokens);
            }

            // TODO #176 use the document Id once it is available in the CAS
            nameSamples.add(
                new ImmutablePair<>(DocumentMetaData.get(cas).getDocumentUri(), namesPerDocument));
        }
        return nameSamples;
    }

    // TODO #176 use the document Id once it is available in the CAS
    private boolean isNamedEntity(RecommenderContext aContext, AnnotationFS token,
        String aDocumentUri)
    {
        return aContext.get(KEY_MODEL).stream().anyMatch(pair->pair.getLeft().equals(aDocumentUri)
        && pair.getRight().stream().anyMatch(t->t.getBegin() == token.getBegin()));
    }

    @Override
    public void predict(RecommenderContext aContext, CAS aCas)
    {
        try {
            JCas jCas = aCas.getJCas();
            Type sentenceType = getType(aCas, Sentence.class);
            Type tokenType = getType(aCas, Token.class);

            // Does it have to be ordered?
            select(aCas, sentenceType).parallelStream().forEach(sentence -> {
                List<AnnotationFS> tokenAnnotations = selectCovered(tokenType, sentence);
                predictSentence(aContext, tokenAnnotations, jCas);
            });
        }
        catch (CASException e) {
            log.error("An error when to trying to access the JCas from Cas.", e);
        }

    }

    private void predictSentence(RecommenderContext aContext, List<AnnotationFS> aTokenAnnotations,
        JCas aJcas)
    {
        int sentenceIndex = 0;
        while (sentenceIndex < aTokenAnnotations.size() - 1) {
            AnnotationFS token = aTokenAnnotations.get(sentenceIndex);

            if (isNamedEntity(aContext, token, DocumentMetaData.get(aJcas).getDocumentUri())) {
                StringBuilder coveredText = new StringBuilder(token.getCoveredText());
                int begin = token.getBegin();
                int end = token.getEnd();

                AnnotationFS nextTokenObject = aTokenAnnotations.get(sentenceIndex + 1);
                // Checking whether the next TokenObject is a NE
                // and whether the sentenceIndex for the next TokenObject is still
                // in the range of the sentence
                while (isNamedEntity(aContext, nextTokenObject,
                    DocumentMetaData.get(aJcas).getDocumentUri())
                    && sentenceIndex + 1 < aTokenAnnotations.size() - 1) {
                    coveredText.append(" ").append(nextTokenObject.getCoveredText());
                    end = nextTokenObject.getEnd();
                    sentenceIndex++;
                    nextTokenObject = aTokenAnnotations.get(sentenceIndex + 1);
                }
                predictToken(coveredText.toString(), begin, end, aJcas);

            }
            sentenceIndex++;
        }
    }

    private void predictToken(String aCoveredText, int aBegin, int aEnd, JCas aJcas)
    {
        List<KBHandle> handles = new ArrayList<>();

        AnnotationFeature feat = annoService
            .getFeature(recommender.getFeature(), recommender.getLayer());
        FeatureSupport<ConceptFeatureTraits> fs = fsRegistry.getFeatureSupport(feat);
        ConceptFeatureTraits traits = fs.readTraits(feat);

        if (traits.getRepositoryId() != null) {
            Optional<KnowledgeBase> kb = kbService
                .getKnowledgeBaseById(recommender.getProject(), traits.getRepositoryId());
            if (kb.isPresent() && kb.get().isSupportConceptLinking()) {
                handles.addAll(readCandidates(kb.get(), aCoveredText, aBegin, aJcas));
            }
        } //TODO if-else
        for (KnowledgeBase kb : kbService.getEnabledKnowledgeBases(recommender.getProject())) {
            if (kb.isSupportConceptLinking()) {
                handles.addAll(readCandidates(kb, aCoveredText, aBegin, aJcas));
            }
        }

        Type predictionType = getAnnotationType(aJcas.getCas(), PredictedSpan.class);

        Feature labelFeature = predictionType.getFeatureByBaseName("label");

        for (KBHandle prediction : handles.stream().limit(numPredictions)
            .collect(Collectors.toList())) {
            AnnotationFS annotation = aJcas.getCas().createAnnotation(predictionType, aBegin, aEnd);
            annotation.setStringValue(labelFeature, prediction.getIdentifier());
            aJcas.getCas().addFsToIndexes(annotation);
        }

    }

    private List<KBHandle> readCandidates(KnowledgeBase kb, String aCoveredText, int aBegin,
        JCas aJcas)
    {
        return kbService
            .read(kb, (conn) -> clService.disambiguate(kb, null, aCoveredText, aBegin, aJcas));
    }

    @Override
    public double evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
    {
        return -1;
    }

    @Override
    public boolean isEvaluable()
    {
        return false;
    }
}
