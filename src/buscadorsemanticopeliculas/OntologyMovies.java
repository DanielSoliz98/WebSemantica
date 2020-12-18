/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package buscadorsemanticopeliculas;

import java.util.HashMap;
import java.util.Map;
import org.apache.jena.ontology.Individual;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.util.FileManager;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ModelFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author
 */
public class OntologyMovies {

    private Model model;
    private OntModel ontologyModel;
    private boolean loadedOntologyStructure;
    private Map<String, Map<String, Individual>> data;
    private String path;
    private JSONObject dataOntology;
    private String[] classes;
    private String service;
    private String[] relations;

    public OntologyMovies() throws IOException {
        this.service = "http://dbpedia.org/sparql";
        this.classes = new String[]{"persons", "cities", "movies", "distributors", "studios", "personFunctions"};
        this.relations = new String[]{"movies", "persons"};
        this.path = new File(".").getCanonicalPath();
        this.initData();
        this.loadOntologyStructure();
    }

    private void initData() {
        this.data = new HashMap<>();
        this.data.put("persons", new HashMap<>());
        this.data.put("cities", new HashMap<>());
        this.data.put("movies", new HashMap<>());
        this.data.put("distributors", new HashMap<>());
        this.data.put("studios", new HashMap<>());
        this.data.put("personFunctions", new HashMap<>());
    }

    private void loadOntologyStructure() {
        try {
            FileManager.get().addLocatorClassLoader(BuscadorSemanticoPeliculas.class.getClassLoader());
            model = FileManager.get().loadModel(path + "/src/movies.rdf");
            this.loadedOntologyStructure = true;
        } catch (Exception e) {
            this.loadedOntologyStructure = false;
        }
    }

    public boolean loadData() throws IOException, FileNotFoundException, ParseException {
        boolean result = false;
        ontologyModel = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM, model);
        try {
            dataOntology = this.readJsonData();
            if (loadedOntologyStructure) {
                for (String classType : classes) {
                    this.loadInstances(classType);
                }
                this.loadRelations();
                result = true;
            }
            return result;
        } catch (IOException | ParseException e) {
            System.out.println(e);
            return result;
        }

    }

    public JSONObject readJsonData() throws FileNotFoundException, IOException, ParseException {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(path + "/src/dataOntology.json"));
        return (JSONObject) obj;
    }

    private void loadInstances(String classType) throws ParseException {
        Query query;
        QueryExecution qe;
        ResultSet resultSet;
        QuerySolution qs;
        JSONObject dataClass = (JSONObject) dataOntology.get(classType);
        String resourceClass = (String) dataClass.get("resource");
        String nameClass = (String) dataClass.get("nameClass");
        JSONArray resources = (JSONArray) dataClass.get("resources");
        JSONArray properties = (JSONArray) dataClass.get("properties");

        for (Object resource : resources) {
            JSONParser parser = new JSONParser();
            JSONObject resourceObject = (JSONObject) parser.parse(resource.toString());
            Map<String, String> propertiesData = new HashMap<>();

            for (Object property : properties) {
                JSONObject propertyObject = (JSONObject) parser.parse(property.toString());

                query = this.getQueryProperty(
                        (String) resourceObject.get("resource"), propertyObject
                );
                qe = QueryExecutionFactory.sparqlService(service, query);
                resultSet = qe.execSelect();
                qs = resultSet.nextSolution();
                String nameProperty = (String) propertyObject.get("nameProperty");

                propertiesData.put(nameProperty, qs.getLiteral((String) propertyObject.get("name")).getString());

            }
            Individual instance = ontologyModel.createIndividual(
                    resourceClass + resourceObject.get("resourceName"),
                    model.getResource(resourceClass + nameClass)
            );
            System.out.println(instance);
            propertiesData.forEach((type, value)
                    -> instance.addProperty(model.getProperty(type), value));
            data.get(classType).put((String) resourceObject.get("resourceName"), instance);
        }

    }

    private Query getQueryProperty(String resourceDBPedia, JSONObject propertyObject) {
        String nameProperty = (String) propertyObject.get("name");
        String typeProperty = (String) propertyObject.get("type");
        boolean inside = (boolean) propertyObject.get("inside");
        boolean language = (boolean) propertyObject.get("language");
        Query query = null;
        if (language) {
            if (inside) {
                String nameInside = (String) propertyObject.get("nameInside");
                String typeInside = (String) propertyObject.get("typeIndise");
                query = QueryFactory.create("SELECT ?" + nameProperty + "\n"
                        + "WHERE {" + resourceDBPedia + " " + typeProperty + " " + "?" + nameInside + " " + ".\n"
                        + "?" + nameInside + " " + typeInside + " " + "?" + nameProperty + " " + ".\n"
                        + "FILTER langMatches( lang(?" + nameProperty + "), 'en')\n"
                        + "}");
            } else {
                query = QueryFactory.create("SELECT ?" + nameProperty + "\n"
                        + "WHERE {" + resourceDBPedia + " " + typeProperty + " ?" + nameProperty + " .\n"
                        + "FILTER langMatches( lang(?" + nameProperty + "), 'en')}");
            }
        } else {
            if (inside) {
                String nameInside = (String) propertyObject.get("nameInside");
                String typeInside = (String) propertyObject.get("typeIndise");
                query = QueryFactory.create("SELECT ?" + nameProperty + "\n"
                        + "WHERE {" + resourceDBPedia + " " + typeProperty + " " + "?" + nameInside + " " + ".\n"
                        + "?" + nameInside + " " + typeInside + " " + "?" + nameProperty + " " + ".\n"
                        + "}");

            } else {
                query = QueryFactory.create("SELECT ?" + nameProperty + "\n"
                        + "WHERE {" + resourceDBPedia + " " + typeProperty + " " + "?" + nameProperty + " " + ".\n"
                        + "}");
            }
        }
        return query;

    }

    private void loadRelations() throws ParseException {
        JSONObject dataClass = (JSONObject) dataOntology.get("relations");
        String key;
        String value;
        String valueClass;
        String relationName;
        String reverseRelationName;
        for (String relation : relations) {
            JSONArray relationObjects = (JSONArray) dataClass.get(relation);
            for (Object relationClass : relationObjects) {
                JSONParser parser = new JSONParser();
                JSONObject relationObject = (JSONObject) parser.parse(relationClass.toString());
                key = (String) relationObject.get("key");
                value = (String) relationObject.get("value");
                valueClass = (String) relationObject.get("valueClass");
                relationName = (String) relationObject.get("relation");
                reverseRelationName = (String) relationObject.get("reverseRelation");
                Individual keyInstance = (Individual) data.get(relation).get(key);
                Individual valueInstance = data.get(valueClass).get(value);
                keyInstance.addProperty(model.getProperty(relationName), valueInstance);
                valueInstance.addProperty(model.getProperty(reverseRelationName), keyInstance);
                System.out.println(keyInstance);
                System.out.println(valueInstance);
            }
        }
        model.add(ontologyModel);
        System.out.println("Ontologia Poblada.");
    }
}
