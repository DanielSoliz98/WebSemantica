/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package buscadorsemanticopeliculas;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;

/**
 *
 *
 */
public class DBPedia {

    public DBPedia() {
    }

    public String testDBpediaConnection() {
        String result = "";
        String service = "http://dbpedia.org/sparql";
        Query query = QueryFactory.create("ASK {}");
        QueryExecution qe = QueryExecutionFactory.sparqlService(service, query);
        try {
            if (qe.execAsk()) {
                result = "Conexion establecida";
            }
        } catch (QueryExceptionHTTP e) {
            result = "Fallo en la conexión";
        } finally {
            qe.close();
        }
        return result;
    }
}
