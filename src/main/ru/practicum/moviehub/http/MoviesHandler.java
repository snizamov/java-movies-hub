package ru.practicum.moviehub.http;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public class MoviesHandler extends BaseHttpHandler {
    MoviesStore moviesStore;
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        switch (method) {
            case "GET":
                getMovieHandle(exchange);
                break;

            case "POST":
                postMovieHandle(exchange);
                break;

            case "DELETE":
                deleteMovieHandle(exchange);
                break;

            default:
                ErrorResponse errorResponse = new ErrorResponse();
                errorResponse.setError("Method Not Allowed");
                sendJson(exchange, 405, gson.toJson(errorResponse));
        }
    }

    private void getMovieHandle(HttpExchange exchange) throws IOException {
        String[] splitPath = exchange.getRequestURI().getPath().split("/");
        String query = exchange.getRequestURI().getQuery();

        if (query != null && query.startsWith("year=")) {
            try {
                int year = Integer.parseInt(query.split("=")[1]);
                sendJson(exchange, 200, gson.toJson(moviesStore.getMoviesByYear(year)));
                return;
            } catch (NumberFormatException e) {
                ErrorResponse errorResponse = new ErrorResponse();
                errorResponse.setError("Некорректный параметр запроса — year");
                sendJson(exchange, 400, gson.toJson(errorResponse));
            }
        }

        if (splitPath.length == 2) {
            String json = gson.toJson(moviesStore.getMovies());
            sendJson(exchange, 200, json);

        } else if (splitPath.length == 3) {
            try {
                int id = Integer.parseInt(splitPath[2]);
                Movie movie = moviesStore.getMovieById(id);

                if (movie == null) {
                    ErrorResponse errorResponse = new ErrorResponse();
                    errorResponse.setError("Фильм с ID-номером: " + id + " не найден. " +
                            "Проверьте номер и повторите попытку");
                    sendJson(exchange, 404, gson.toJson(errorResponse));
                    return;
                }
                sendJson(exchange, 200, gson.toJson(movie));
            } catch (NumberFormatException e) {
                ErrorResponse errorResponse = new ErrorResponse();
                errorResponse.setError("Некорректный ID");
                sendJson(exchange, 400, gson.toJson(errorResponse));
            }
        }
    }

    private void postMovieHandle(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");

        if (contentType == null || !contentType.equals(CT_JSON)) {
            ErrorResponse error = new ErrorResponse();
            error.setError("Unsupported Media Type");
            sendJson(exchange, 415, gson.toJson(error));
            return;
        }

        String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonElement jsonElement;
        try {
            jsonElement = JsonParser.parseString(json);
        } catch (JsonSyntaxException e) {
            ErrorResponse error = new ErrorResponse();
            error.setError("Некорректный JSON");
            sendJson(exchange, 400, gson.toJson(error));
            return;
        }

        Movie movie = gson.fromJson(jsonElement, Movie.class);
        ErrorResponse errorResponse = new ErrorResponse();

        if (hasValidationError(movie, errorResponse)) {
            errorResponse.setError("Ошибка валидации");
            sendJson(exchange, 422, gson.toJson(errorResponse));
            return;
        }
        moviesStore.addMovie(movie);
        sendJson(exchange, 201, gson.toJson(movie));
    }

    private void deleteMovieHandle(HttpExchange exchange) throws IOException {
        String[] splitPath = exchange.getRequestURI().getPath().split("/");

        try {
            int id = Integer.parseInt(splitPath[2]);
            Movie movie = moviesStore.getMovieById(id);
            if (movie == null) {
                ErrorResponse errorResponse = new ErrorResponse();
                errorResponse.setError("Фильм не найден");
                sendJson(exchange, 404, gson.toJson(errorResponse));
                return;
            }
            moviesStore.deleteMovieById(id);
            sendNoContent(exchange);
        } catch (NumberFormatException e) {
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setError("Некорректный ID");
            sendJson(exchange, 400, gson.toJson(errorResponse));
        }
    }

    private boolean hasValidationError(Movie movie, ErrorResponse errorResponse) {
        boolean hasError = false;
        if (movie.getTitle() == null || movie.getTitle().isBlank()) {
            errorResponse.getDetails().add("название не должно быть пустым");
            hasError = true;
        }
        if (movie.getTitle().length() > 100) {
            errorResponse.getDetails().add("количество символов в названии не должно превышать 100");
            hasError = true;
        }
        if (movie.getYear() < 1888 || movie.getYear() > LocalDate.now().getYear() + 1) {
            errorResponse.getDetails().add("год должен быть между 1888 и 2026");
            hasError = true;
        }
        return hasError;
    }
}

