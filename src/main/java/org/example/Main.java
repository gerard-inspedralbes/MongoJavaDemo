package org.example;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoIterable;

public class Main {
    public static void main(String[] args) {
        // URL de conexión a MongoDB
        String connectionString = "mongodb+srv://gtorren8:Industex00@cluster0.wm47zyi.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";

        // Crear un cliente de MongoDB
        try (MongoClient mongoClient = MongoClients.create(connectionString)) {
            // Obtener y listar nombres de las bases de datos
            MongoIterable<String> databaseNames = mongoClient.listDatabaseNames();
            for (String dbName : databaseNames) {
                System.out.println(dbName);
            }

            ProyectoGestionAlumnos p1 = new ProyectoGestionAlumnos(mongoClient);

            int assig = p1.numAsig("12345678A","2324_S1");
            System.out.println(assig);
        }
    }
}