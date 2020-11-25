package org.silkca;

//import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static java.util.Arrays.asList;

//import org.apache.uima.cas.CAS;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.plugin.api.ExportedComponent;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactoryImplBase;

@ExportedComponent
@Component
public class SilkRecFactory extends RecommendationEngineFactoryImplBase<Void>
{
 // This is a string literal so we can rename/refactor the class without it changing its ID
    // and without the database starting to refer to non-existing recommendation tools.
    public static final String ID =
        "de.tudarmstadt.ukp.inception.recommendation.imls.datamajority.de.tudarmstadt.ukp.inception.recommendation.imls.SilkRecommender";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public RecommendationEngine build(Recommender aRecommender)
    {
        return new SilkRecommender(aRecommender);
    }

    @Override
    public String getName()
    {
        return "Silk Recommender";
    }

    @Override
    public boolean accepts(AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        if (aLayer == null || aFeature == null) {
            return false;
        }

        return (asList(SINGLE_TOKEN, TOKENS).contains(aLayer.getAnchoringMode()));
                //&& SPAN_TYPE.equals(aLayer.getType())
                //&& CAS.TYPE_NAME_STRING.equals(aFeature.getType()) || aFeature.isVirtualFeature();
    }
}
