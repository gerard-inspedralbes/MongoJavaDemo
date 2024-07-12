package org.example;

import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Arrays;
import java.util.List;

public class ProyectoGestionAlumnos {
    private final MongoClient client;
    private final MongoDatabase db;
    private final MongoCollection<Document> alumnosCollection;

    public ProyectoGestionAlumnos(MongoClient client) {
        this.client = client;
        this.db = client.getDatabase("Proyecto_Gestion_Alumnos");
        this.alumnosCollection = db.getCollection("Alumnos2");
    }

    public int numAsig(String dni, String semestre) {
        List<Bson> pipeline = Arrays.asList(
                Aggregates.match(Filters.eq("dni", dni)),
                Aggregates.unwind("$matriculas"),
                Aggregates.unwind("$matriculas.semestres"),
                Aggregates.match(Filters.eq("matriculas.semestres.semestre_id", semestre)),
                Aggregates.unwind("$matriculas.semestres.asignaturas"),
                Aggregates.project(Projections.fields(
                        Projections.include("nombre", "apellidos", "dni"),
                        Projections.computed("asignatura_id", "$matriculas.semestres.asignaturas.asignatura_id"),
                        Projections.computed("estado", "$matriculas.semestres.asignaturas.estado")
                )),
                Aggregates.count("num_asig")
        );

        AggregateIterable<Document> results = alumnosCollection.aggregate(pipeline);

        for (Document item : results) {
            return item.getInteger("num_asig");
        }
        return 0;
    }

    public int numAsigAprob(String dni, String semestre) {
        List<Bson> pipeline = Arrays.asList(
                Aggregates.match(Filters.eq("dni", dni)),
                Aggregates.unwind("$matriculas"),
                Aggregates.unwind("$matriculas.semestres"),
                Aggregates.match(Filters.eq("matriculas.semestres.semestre_id", semestre)),
                Aggregates.unwind("$matriculas.semestres.asignaturas"),
                Aggregates.project(Projections.fields(
                        Projections.include("nombre", "apellidos", "dni"),
                        Projections.computed("asignatura_id", "$matriculas.semestres.asignaturas.asignatura_id"),
                        Projections.computed("estado", "$matriculas.semestres.asignaturas.estado")
                ))
        );

        AggregateIterable<Document> results = alumnosCollection.aggregate(pipeline);

        int count = 0;
        for (Document item : results) {
            if ("aprobada".equals(item.getString("estado"))) {
                count++;
            }
        }
        return count;
    }

    public boolean semestreSuperado(String dni, String semestre) {
        int asigTotal = numAsig(dni, semestre);
        int asigAprob = numAsigAprob(dni, semestre);
        return asigAprob / (double) asigTotal >= 0.5;
    }

    public String matricularNouSemestre(String dni, String semestreAct, String semestreAnt) {
        if (!semestreSuperado(dni, semestreAnt)) {
            return "El alumno no ha superado el semestre " + semestreAnt;
        }

        if (estaMatriculado(dni, semestreAct)) {
            return "El alumno ya está matriculado";
        }

        Document newSemestre = new Document("semestre_id", semestreAct)
                .append("asignaturas", Arrays.asList());

        alumnosCollection.updateOne(
                Filters.eq("dni", dni),
                Updates.push("matriculas.0.semestres", newSemestre)
        );

        return "Semestre matriculado";
    }

    public String matricularNovaAsig(String dni, String semestre, String asignaturaId) {
        if (numAsig(dni, semestre) > 6) {
            return "El alumno ya tiene 6 asignaturas";
        }

        Document newAsignatura = new Document("asignatura_id", asignaturaId)
                .append("estado", "pendiente");

        alumnosCollection.updateOne(
                Filters.eq("dni", dni),
                Updates.push("matriculas.0.semestres.$[elem].asignaturas", newAsignatura),
                new UpdateOptions().arrayFilters(Arrays.asList(Filters.eq("elem.semestre_id", semestre)))
        );

        return "Asignatura matriculada";
    }

    public boolean estaMatriculado(String dni, String semestre) {
        List<Bson> pipeline = Arrays.asList(
                Aggregates.match(Filters.eq("dni", dni)),
                Aggregates.unwind("$matriculas"),
                Aggregates.unwind("$matriculas.semestres"),
                Aggregates.match(Filters.eq("matriculas.semestres.semestre_id", semestre)),
                Aggregates.count("num_sem")
        );

        AggregateIterable<Document> result = alumnosCollection.aggregate(pipeline);

        for (Document item : result) {
            return true;
        }
        return false;
    }
}
