package scan;

import annotation.*;
import jakarta.persistence.*;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class Generation<T> {
    private Class<T> entityClass;
    private DbUtil dbUtil;

    public Generation(Class<T> entityClass, DbUtil dbUtil) {
        this.entityClass = entityClass;
        this.dbUtil = dbUtil;
    }

    public void save(T entity) {
        try (Connection connection = dbUtil.getConnection()) {
            Table table = entityClass.getAnnotation(Table.class);
            String tableName = table.name();

            StringBuilder columns = new StringBuilder();
            StringBuilder values = new StringBuilder();
            List<Object> params = new ArrayList<>();

            for (Field field : entityClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Column.class)) {
                    field.setAccessible(true);
                    Column column = field.getAnnotation(Column.class);
                    columns.append(column.name()).append(", ");
                    values.append("?, ");
                    params.add(field.get(entity));
                }
            }

            columns.setLength(columns.length() - 2);
            values.setLength(values.length() - 2);

            String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ")";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                for (int i = 0; i < params.size(); i++) {
                    preparedStatement.setObject(i + 1, params.get(i));
                }
                preparedStatement.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la sauvegarde de l'entité");
        }
    }

    public List<T> findAll() {
        List<T> resultList = new ArrayList<>();
        try (Connection connection = dbUtil.getConnection()) {
            Table table = entityClass.getAnnotation(Table.class);
            String sql = "SELECT * FROM " + table.name();
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
                 ResultSet resultSet = preparedStatement.executeQuery()) {

                while (resultSet.next()) {
                    T entity = entityClass.getDeclaredConstructor().newInstance();
                    for (Field field : entityClass.getDeclaredFields()) {
                        if (field.isAnnotationPresent(Column.class)) {
                            field.setAccessible(true);
                            Column column = field.getAnnotation(Column.class);
                            field.set(entity, resultSet.getObject(column.name()));
                        }
                    }
                    resultList.add(entity);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération des données");
        }
        return resultList;
    }

    public T findById(String columnName, Object value) {
        try (Connection connection = dbUtil.getConnection()) {
            Table table = entityClass.getAnnotation(Table.class);
            String sql = "SELECT * FROM " + table.name() + " WHERE " + columnName + " = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setObject(1, value);
                ResultSet resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    T entity = entityClass.getDeclaredConstructor().newInstance();
                    for (Field field : entityClass.getDeclaredFields()) {
                        if (field.isAnnotationPresent(Column.class)) {
                            field.setAccessible(true);
                            Column column = field.getAnnotation(Column.class);
                            field.set(entity, resultSet.getObject(column.name()));
                        }
                    }
                    return entity;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la recherche par ID");
        }
        return null;
    }

    public void update(T entity, String idColumn, Object idValue) {
        try (Connection connection = dbUtil.getConnection()) {
            Table table = entityClass.getAnnotation(Table.class);
            String tableName = table.name();

            StringBuilder setClause = new StringBuilder();
            List<Object> params = new ArrayList<>();

            for (Field field : entityClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Column.class)) {
                    field.setAccessible(true);
                    Column column = field.getAnnotation(Column.class);
                    setClause.append(column.name()).append(" = ?, ");
                    params.add(field.get(entity));
                }
            }

            setClause.setLength(setClause.length() - 2);
            params.add(idValue);

            String sql = "UPDATE " + tableName + " SET " + setClause + " WHERE " + idColumn + " = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                for (int i = 0; i < params.size(); i++) {
                    preparedStatement.setObject(i + 1, params.get(i));
                }
                preparedStatement.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la mise à jour de l'entité");
        }
    }

    public List<T> customQuery(String query) {
        List<T> resultList = new ArrayList<>();
        try (Connection connection = dbUtil.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                T entity = entityClass.getDeclaredConstructor().newInstance();
                for (Field field : entityClass.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Column.class)) {
                        field.setAccessible(true);
                        Column column = field.getAnnotation(Column.class);
                        field.set(entity, resultSet.getObject(column.name()));
                    }
                }
                resultList.add(entity);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'exécution de la requête personnalisée");
        }
        return resultList;
    }

    public void delete(String columnName, Object value) {
        try (Connection connection = dbUtil.getConnection()) {
            Table table = entityClass.getAnnotation(Table.class);
            String sql = "DELETE FROM " + table.name() + " WHERE " + columnName + " = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setObject(1, value);
                preparedStatement.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la suppression de l'entité");
        }
    }
} 
