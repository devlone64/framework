package io.gitlab.lone64.framework.bukkit.api.database.mysql;

import io.gitlab.lone64.framework.bukkit.api.database.implement.AbstractConnection;
import io.gitlab.lone64.framework.bukkit.api.database.implement.AbstractDatabase;
import io.gitlab.lone64.framework.bukkit.api.database.handler.TypeHandler;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.plugin.Plugin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class MySQLDatabase implements AbstractDatabase {

    private final MySQLConnection connection;

    public MySQLDatabase(Plugin plugin, String host, String port, String name, String username, String password) {
        this.connection = new MySQLConnection(plugin, host, port, name, username, password);
    }

    @Override
    @SneakyThrows
    public void createTable(String table, String columns) {
        var id = "id INT AUTO_INCREMENT PRIMARY KEY";
        var query = "CREATE TABLE IF NOT EXISTS %s(%s, %s)".formatted(table, id, columns);
        var statement = prepareStatement(getConnection(), query);

        statement.executeUpdate();
        statement.close();
    }

    @Override
    @SneakyThrows
    public void deleteTable(String table) {
        var query = "DROP TABLE IF EXISTS %s".formatted(table);
        var statement = prepareStatement(getConnection(), query);

        statement.executeUpdate();
        statement.close();
    }

    @Override
    @SneakyThrows
    public void set(String table, String selected, Object[] values, String logic, String column, String data) {
        if (is(table, column, data)) {
            var query = "UPDATE %s SET %s WHERE %s %s ?".formatted(table, "`" + String.join("` = ?, `", selected.split(", ")) + "` = ?", column, logic);
            var statement = prepareStatement(getConnection(), query, values, data);

            statement.executeUpdate();
            statement.close();
        } else {
            var query = "INSERT INTO %s(%s) VALUES(%s)".formatted(table, selected, String.join(", ", Collections.nCopies(values.length, "?")));
            var statement = prepareStatement(getConnection(), query, values);

            statement.executeUpdate();
            statement.close();
        }
    }

    @Override
    @SneakyThrows
    public void setIfAbuse(String table, String selected, Object[] values, String logic, String column, String data) {
        if (is(table, column, data)) return;

        var query = "INSERT INTO %s(%s, %s) VALUES(?, %s)".formatted(table, column, selected, String.join(", ", Collections.nCopies(values.length, "?")));
        var statement = prepareStatement(getConnection(), query, values);

        statement.executeUpdate();
        statement.close();
    }

    @Override
    @SneakyThrows
    public void remove(String table, String logic, String column, String data) {
        if (!is(table, column, data)) return;

        var query = "DELETE FROM %s WHERE %s %s ?".formatted(table, column, logic);
        var statement = prepareStatement(getConnection(), query, data);

        statement.executeUpdate();
        statement.close();
    }

    @Override
    @SneakyThrows
    public Object get(String table, String selected, String logic, String column, String data) {
        var query = "SELECT %s FROM %s WHERE %s %s ?".formatted(selected, table, column, logic);
        var statement = prepareStatement(getConnection(), query, data);
        var resultSet = statement.executeQuery();
        if (!resultSet.next()) return null;

        var result = resultSet.getObject(selected);

        statement.close();
        resultSet.close();

        return result;
    }

    @Override
    @SneakyThrows
    public String getString(String table, String selected, String logic, String column, String data) {
        var query = "SELECT %s FROM %s WHERE %s %s ?".formatted(selected, table, column, logic);
        var statement = prepareStatement(getConnection(), query, data);
        var resultSet = statement.executeQuery();
        if (!resultSet.next()) return null;

        var result = resultSet.getString(selected);

        statement.close();
        resultSet.close();

        return result;
    }

    @Override
    @SneakyThrows
    public <T> T get(String table, String selected, String logic, String column, String data, TypeHandler<T, ResultSet> handler) {
        boolean equals = column == null || column.isEmpty() || logic == null || logic.isEmpty();
        var query = equals ? "SELECT %s FROM %s WHERE 1".formatted(selected, table) : "SELECT %s FROM %s WHERE %s %s ?".formatted(selected, table, column, logic);
        var statement = prepareStatement(getConnection(), query, equals ? new Object[0] : new Object[]{ data });
        var resultSet = statement.executeQuery();
        if (!resultSet.next()) return null;

        var result = handler.accept(resultSet);

        statement.close();
        resultSet.close();

        return result;
    }

    @Override
    @SneakyThrows
    public List<Object> getList(String table, String selected, String logic, String column, String data) {
        var query = "SELECT %s FROM %s WHERE %s %s ?".formatted(selected, table, column, logic);
        var statement = prepareStatement(getConnection(), query, data);
        try (ResultSet resultSet = statement.executeQuery()) {
            var count = 0;
            List<Object> result = new ArrayList<>();
            while (resultSet.next()) {
                var strings = selected.split(",");
                if (count >= strings.length) break;
                result.add(resultSet.getObject(strings[count].replace(" ", "")));
                count++;
            }

            statement.close();
            resultSet.close();

            return result;
        }
    }

    @Override
    @SneakyThrows
    public List<String> getStringList(String table, String selected, String logic, String column, String data) {
        var query = "SELECT %s FROM %s WHERE %s %s ?".formatted(selected, table, column, logic);
        var statement = prepareStatement(getConnection(), query, data);
        try (ResultSet resultSet = statement.executeQuery()) {
            var count = 0;
            List<String> result = new ArrayList<>();
            while (resultSet.next()) {
                var strings = selected.split(",");
                if (count >= strings.length) break;
                result.add(resultSet.getString(strings[count].replace(" ", "")));
                count++;
            }

            statement.close();
            resultSet.close();

            return result;
        }
    }

    @Override
    @SneakyThrows
    public <T> List<T> getList(String table, String selected, String logic, String column, String data, TypeHandler<T, ResultSet> handler) {
        boolean equals = column == null || column.isEmpty() || logic == null || logic.isEmpty();
        var query = equals ? "SELECT %s FROM %s WHERE 1".formatted(selected, table) : "SELECT %s FROM %s WHERE %s %s ?".formatted(selected, table, column, logic);
        var statement = prepareStatement(getConnection(), query, equals ? new Object[0] : new Object[]{ data });
        try (ResultSet resultSet = statement.executeQuery()) {
            List<T> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(handler.accept(resultSet));
            }

            statement.close();
            resultSet.close();

            return result;
        }
    }

    @Override
    @SneakyThrows
    public List<String> getKeys(String table, String selected) {
        var query = "SELECT %s FROM %s".formatted(selected, table);
        var statement = prepareStatement(getConnection(), query);
        try (ResultSet resultSet = statement.executeQuery()) {
            var count = 0;
            List<String> result = new ArrayList<>();
            while (resultSet.next()) {
                var strings = selected.split(",");
                if (count >= strings.length) break;
                result.add(resultSet.getString(strings[count].replace(" ", "")));
                count++;
            }

            statement.close();
            resultSet.close();

            return result;
        }
    }

    @Override
    @SneakyThrows
    public boolean is(String table, String column, String data) {
        var query = "SELECT * FROM %s WHERE %s = ?".formatted(table, column);
        var statement = prepareStatement(getConnection(), query, data);
        var resultSet = statement.executeQuery();
        var exists = resultSet.next();

        statement.close();
        resultSet.close();

        return exists;
    }

    @SneakyThrows
    private PreparedStatement prepareStatement(AbstractConnection info, String sql, Object... objectsToSet) {
        var statement = info.getConnection().prepareStatement(sql);
        for (int i = 0; i < objectsToSet.length; i++) statement.setObject(i + 1, objectsToSet[i]);
        return statement;
    }

}