package org.oasis_open.wemi.context.server.impl.services;

import org.apache.cxf.helpers.IOUtils;
import org.oasis_open.wemi.context.server.api.conditions.Tag;
import org.oasis_open.wemi.context.server.api.conditions.ConditionType;
import org.oasis_open.wemi.context.server.api.consequences.ConsequenceType;
import org.oasis_open.wemi.context.server.api.services.DefinitionsService;
import org.oasis_open.wemi.context.server.persistence.spi.PersistenceService;
import org.ops4j.pax.cdi.api.OsgiService;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.json.*;
import java.net.URL;
import java.util.*;

@Singleton
@Default
@OsgiServiceProvider
public class DefinitionsServiceImpl implements DefinitionsService {

    private static final Logger logger = LoggerFactory.getLogger(DefinitionsServiceImpl.class.getName());

    Map<String, Tag> conditionTags = new HashMap<String, Tag>();
    Set<Tag> rootTags = new LinkedHashSet<Tag>();
    Map<String, ConditionType> conditionTypeByName = new HashMap<String, ConditionType>();
    Map<String, ConsequenceType> consequencesTypeByName = new HashMap<String, ConsequenceType>();
    Map<Tag, Set<ConditionType>> conditionTypeByTag = new HashMap<Tag, Set<ConditionType>>();

    public DefinitionsServiceImpl() {
        System.out.println("Instantiating definitions service...");
    }

    @Inject
    private BundleContext bundleContext;

    @Inject
    @OsgiService
    private PersistenceService persistenceService;

    @PostConstruct
    public void postConstruct() {
        logger.debug("postConstruct {" + bundleContext.getBundle() + "}");

        loadPredefinedMappings();

        loadPredefinedTags();

        loadPredefinedCondition();
        loadPredefinedConsequences();
    }


    private void loadPredefinedMappings() {
        Enumeration<URL> predefinedMappings = bundleContext.getBundle().findEntries("META-INF/mappings", "*.json", true);
        while (predefinedMappings.hasMoreElements()) {
            URL predefinedMappingURL = predefinedMappings.nextElement();
            logger.debug("Found mapping at " + predefinedMappingURL + ", loading... ");
            try {
                final String path = predefinedMappingURL.getPath();
                String name = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));
                String content = IOUtils.readStringFromStream(predefinedMappingURL.openStream());
                persistenceService.createMapping(name, content);
            } catch (Exception e) {
                logger.error("Error while loading segment definition " + predefinedMappingURL, e);
            }
        }
    }

    private void loadPredefinedTags() {
        Enumeration<URL> predefinedSegmentEntries = bundleContext.getBundle().findEntries("META-INF/tags", "*.json", true);
        while (predefinedSegmentEntries.hasMoreElements()) {
            URL predefinedSegmentURL = predefinedSegmentEntries.nextElement();
            logger.debug("Found predefined tags at " + predefinedSegmentURL + ", loading... ");

            JsonReader reader = null;
            try {
                reader = Json.createReader(predefinedSegmentURL.openStream());
                JsonStructure jsonst = reader.read();

                // dumpJSON(jsonst, null, "");
                JsonObject tagObject = (JsonObject) jsonst;
                Tag tag = new Tag(tagObject.getString("id"),
                        tagObject.getString("name"),
                        tagObject.getString("description"),
                        tagObject.getString("parent"));

                conditionTags.put(tag.getId(), tag);
            } catch (Exception e) {
                logger.error("Error while loading tag definition " + predefinedSegmentURL, e);
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }

        }

        // now let's resolve all the children.
        for (Tag tag : conditionTags.values()) {
            if (tag.getParentId() != null && tag.getParentId().length() > 0) {
                Tag parentTag = conditionTags.get(tag.getParentId());
                if (parentTag != null) {
                    parentTag.getSubTags().add(tag);
                }
            } else {
                rootTags.add(tag);
            }
        }
    }

    private void loadPredefinedCondition() {
        Enumeration<URL> predefinedSegmentEntries = bundleContext.getBundle().findEntries("META-INF/conditions", "*.json", true);
        while (predefinedSegmentEntries.hasMoreElements()) {
            URL predefinedConditionURL = predefinedSegmentEntries.nextElement();
            logger.debug("Found predefined conditions at " + predefinedConditionURL + ", loading... ");

            JsonReader reader = null;
            try {
                reader = Json.createReader(predefinedConditionURL.openStream());
                JsonStructure jsonst = reader.read();

                // dumpJSON(jsonst, null, "");
                JsonObject conditionObject = (JsonObject) jsonst;

                String id = conditionObject.getString("id");
                String name = conditionObject.getString("name");
                String description = conditionObject.getString("description");
                JsonArray tagArray = conditionObject.getJsonArray("tags");
                Set<String> tagIds = new LinkedHashSet<String>();
                for (int i = 0; i < tagArray.size(); i++) {
                    tagIds.add(tagArray.getString(i));
                }

                ConditionType condition = new ConditionType(id, name);
                condition.setDescription(description);
                condition.setConditionParameters(ParserHelper.parseParameters(conditionObject));

                conditionTypeByName.put(condition.getId(), condition);
                for (String tagId : tagIds) {
                    Tag tag = conditionTags.get(tagId);
                    if (tag != null) {
                        Set<ConditionType> conditionNodes = conditionTypeByTag.get(tag);
                        if (conditionNodes == null) {
                            conditionNodes = new LinkedHashSet<ConditionType>();
                        }
                        conditionNodes.add(condition);
                        conditionTypeByTag.put(tag, conditionNodes);
                    } else {
                        // we found a tag that is not defined, we will define it automatically
                        logger.warn("Unknown tag " + tagId + " used in condition definition " + predefinedConditionURL);
                    }
                }
            } catch (Exception e) {
                logger.error("Error while loading condition definition " + predefinedConditionURL, e);
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }

        }

    }

    private void loadPredefinedConsequences() {
        Enumeration<URL> predefinedSegmentEntries = bundleContext.getBundle().findEntries("META-INF/consequences", "*.json", true);
        while (predefinedSegmentEntries.hasMoreElements()) {
            URL predefinedConditionURL = predefinedSegmentEntries.nextElement();
            logger.debug("Found predefined consequence at " + predefinedConditionURL + ", loading... ");

            JsonReader reader = null;
            try {
                reader = Json.createReader(predefinedConditionURL.openStream());
                JsonStructure jsonst = reader.read();

                // dumpJSON(jsonst, null, "");
                JsonObject conditionObject = (JsonObject) jsonst;

                String id = conditionObject.getString("id");
                String name = conditionObject.getString("name");
                String description = conditionObject.getString("description");
                String clazz = conditionObject.getString("class");
//                JsonArray tagArray = conditionObject.getJsonArray("tags");
//                Set<String> tagIds = new LinkedHashSet<String>();
//                for (int i = 0; i < tagArray.size(); i++) {
//                    tagIds.add(tagArray.getString(i));
//                }
//
                ConsequenceType consequence = new ConsequenceType(id, name);

                consequence.setDescription(description);
                consequence.setConsequenceParameters(ParserHelper.parseParameters(conditionObject));

                consequencesTypeByName.put(consequence.getId(), consequence);
            } catch (Exception e) {
                logger.error("Error while loading condition definition " + predefinedConditionURL, e);
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        }

    }


    public ConditionType getConditionType(String name) {
        return conditionTypeByName.get(name);
    }

    public ConsequenceType getConsequenceType(String name) {
        return consequencesTypeByName.get(name);
    }

    public Set<Tag> getConditionTags() {
        return new HashSet<Tag>(conditionTags.values());
    }

    public Set<ConditionType> getConditions(Tag tag) {
        return conditionTypeByTag.get(tag);
    }

}