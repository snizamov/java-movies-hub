package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {

    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer();
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        server.getMoviesStore().clear();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode(),
                "GET /movies должен вернуть 200");

        String contentTypeHeaderValue = response.headers()
                .firstValue("Content-Type")
                .orElse("");
        assertEquals("application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = response.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");

        Gson gson = new Gson();
        List<Movie> movies = gson.fromJson(
                response.body(),
                new ListOfMoviesTypeToken().getType()
        );
        assertTrue(movies.isEmpty());
    }

    @Test
    void getMovies_whenNotEmpty_returnsMovieArray() throws IOException, InterruptedException {
        server.getMoviesStore().addMovie(new Movie("Harry Potter", 2002));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode(),
                "GET /movies должен вернуть 200");

        String contentTypeHeaderValue = response.headers()
                .firstValue("Content-Type")
                .orElse("");
        assertEquals("application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = response.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");

        Gson gson = new Gson();
        List<Movie> movies = gson.fromJson(
                response.body(),
                new ListOfMoviesTypeToken().getType()
        );
        assertEquals(1, movies.size());
        assertEquals("Harry Potter", movies.get(0).getTitle());
        assertEquals(2002, movies.get(0).getYear());
    }

    @Test
    void postMovie_whenInputCorrect_returnsAddedMovie() throws IOException, InterruptedException {

        String json = "{\"title\":\"Harry Potter\",\"year\":2002}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(201, response.statusCode(),
                "POST /movies должен вернуть 201");

        String contentTypeHeaderValue = response.headers()
                .firstValue("Content-Type")
                .orElse("");
        assertEquals("application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = response.body().trim();
        assertTrue(body.startsWith("{") && body.endsWith("}"),
                "Ожидается JSON");
        assertTrue(body.contains("Harry Potter"));
    }

    @Test
    void postMovie_whenTitleIsBlank_returnsError() throws IOException, InterruptedException {
        String json = "{\"title\":\"\",\"year\":2002}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, response.statusCode(),
                "POST /movies должен вернуть 422");
        String contentTypeHeaderValue = response.headers()
                .firstValue("Content-Type")
                .orElse("");
        assertEquals("application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = response.body().trim();
        assertTrue(body.startsWith("{") && body.endsWith("}"),
                "Ожидается JSON");
        assertTrue(body.contains("название не должно быть пустым"));
    }

    @Test
    void postMovie_whenTitleIsMore100Symbols_returnsError() throws IOException, InterruptedException {
        String json = "{\"title\":\"" +
                "1234567890" +
                "1234567890" +
                "1234567890" +
                "1234567890" +
                "1234567890" +
                "1234567890" +
                "1234567890" +
                "1234567890" +
                "1234567890" +
                "1234567890" +
                "1" +
                "\",\"year\":2002}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, response.statusCode(),
                "POST /movies должен вернуть 422");
        String contentTypeHeaderValue = response.headers()
                .firstValue("Content-Type")
                .orElse("");
        assertEquals("application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = response.body().trim();
        assertTrue(body.startsWith("{") && body.endsWith("}"),
                "Ожидается JSON");
        assertTrue(body.contains("количество символов в названии не должно превышать 100"));
    }

    @Test
    void postMovie_whenYearIsIncorrect_returnsError() throws IOException, InterruptedException {

        String json = "{\"title\":\"Harry Potter\",\"year\":1861}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, response.statusCode(),
                "POST /movies должен вернуть 422");

        String contentTypeHeaderValue = response.headers()
                .firstValue("Content-Type")
                .orElse("");
        assertEquals("application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = response.body().trim();
        assertTrue(body.startsWith("{") && body.endsWith("}"),
                "Ожидается JSON");
        assertTrue(body.contains("год должен быть между 1888 и 2027"));
    }

    @Test
    void postMovie_whenContentTypeIsIncorrect_returnsError() throws IOException, InterruptedException {

        String json = "{\"title\":\"Harry Potter\",\"year\":2002}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/plain; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(415, response.statusCode(),
                "POST /movies должен вернуть 415");

    }

    @Test
    void postMovie_whenJsonIsIncorrect_returnsError() throws IOException, InterruptedException {
        String json = "title:Harry Potter,year:2002";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, response.statusCode(),
                "POST /movies должен вернуть 400");
    }

    @Test
    void getMovieId_returnsMovie() throws IOException, InterruptedException {
        server.getMoviesStore().addMovie(new Movie("Harry Potter", 2002));
        server.getMoviesStore().addMovie(new Movie("Бриллиантовая рука", 1969));
        server.getMoviesStore().addMovie(new Movie("Криминальное чтиво", 1994));

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies/2"))
                .build();

        HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode(),
                "GET /movies/{id} должен вернуть 200");

        String contentTypeHeaderValue = response.headers()
                .firstValue("Content-Type")
                .orElse("");
        assertEquals("application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = response.body().trim();
        assertTrue(body.startsWith("{") && body.endsWith("}"),
                "Ожидается JSON-объект");
        assertTrue(body.contains("Бриллиантовая рука"));
    }

    @Test
    void getMovieId_whenMovieDoesNotFound_returnsError() throws IOException, InterruptedException {
        server.getMoviesStore().addMovie(new Movie("Harry Potter", 2002));
        server.getMoviesStore().addMovie(new Movie("Бриллиантовая рука", 1969));
        server.getMoviesStore().addMovie(new Movie("Криминальное чтиво", 1994));

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies/10"))
                .build();

        HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(404, response.statusCode(),
                "GET /movies/{id} должен вернуть 404");

        String contentTypeHeaderValue = response.headers()
                .firstValue("Content-Type")
                .orElse("");
        assertEquals("application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = response.body().trim();
        assertTrue(body.startsWith("{") && body.endsWith("}"),
                "Ожидается JSON-объект");
    }

    @Test
    void getMovieId_whenIdIsNotNumber_returnsError() throws IOException, InterruptedException {
        server.getMoviesStore().addMovie(new Movie("Harry Potter", 2002));
        server.getMoviesStore().addMovie(new Movie("Бриллиантовая рука", 1969));
        server.getMoviesStore().addMovie(new Movie("Криминальное чтиво", 1994));

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies/abc"))
                .build();

        HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, response.statusCode(),
                "GET /movies/{id} должен вернуть 400");

        String contentTypeHeaderValue = response.headers()
                .firstValue("Content-Type")
                .orElse("");
        assertEquals("application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = response.body().trim();
        assertTrue(body.startsWith("{") && body.endsWith("}"),
                "Ожидается JSON-объект");
    }

    @Test
    void deleteMovieId_returnNoContent() throws IOException, InterruptedException {
        server.getMoviesStore().addMovie(new Movie("Harry Potter", 2002));
        server.getMoviesStore().addMovie(new Movie("Бриллиантовая рука", 1969));
        server.getMoviesStore().addMovie(new Movie("Криминальное чтиво", 1994));

        HttpRequest request = HttpRequest.newBuilder()
                .DELETE()
                .uri(URI.create(BASE + "/movies/0"))
                .build();

        HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(204, response.statusCode(),
                "GET /movies/{id} должен вернуть 204");

        String contentTypeHeaderValue = response.headers()
                .firstValue("Content-Type")
                .orElse("");
        assertEquals("application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
    }

    @Test
    void deleteMovieId_whenMovieDoesNotFound_returnNoContent() throws IOException, InterruptedException {
        server.getMoviesStore().addMovie(new Movie("Harry Potter", 2002));
        server.getMoviesStore().addMovie(new Movie("Бриллиантовая рука", 1969));
        server.getMoviesStore().addMovie(new Movie("Криминальное чтиво", 1994));

        HttpRequest request = HttpRequest.newBuilder()
                .DELETE()
                .uri(URI.create(BASE + "/movies/10"))
                .build();

        HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(404, response.statusCode(),
                "GET /movies/{id} должен вернуть 404");

        String contentTypeHeaderValue = response.headers()
                .firstValue("Content-Type")
                .orElse("");
        assertEquals("application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = response.body().trim();
        assertTrue(body.startsWith("{") && body.endsWith("}"),
                "Ожидается JSON-объект");
    }

    @Test
    void deleteMovieId_whenIdIsNotNumber_returnError() throws IOException, InterruptedException {
        server.getMoviesStore().addMovie(new Movie("Harry Potter", 2002));
        server.getMoviesStore().addMovie(new Movie("Бриллиантовая рука", 1969));
        server.getMoviesStore().addMovie(new Movie("Криминальное чтиво", 1994));

        HttpRequest request = HttpRequest.newBuilder()
                .DELETE()
                .uri(URI.create(BASE + "/movies/abc"))
                .build();

        HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, response.statusCode(),
                "GET /movies/{id} должен вернуть 400");

        String contentTypeHeaderValue = response.headers()
                .firstValue("Content-Type")
                .orElse("");
        assertEquals("application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = response.body().trim();
        assertTrue(body.startsWith("{") && body.endsWith("}"),
                "Ожидается JSON-объект");
    }

    @Test
    void getMoviesByYear_returnListOfMovies() throws IOException, InterruptedException {
        server.getMoviesStore().addMovie(new Movie("Harry Potter", 2002));
        server.getMoviesStore().addMovie(new Movie("Бриллиантовая рука", 1969));
        server.getMoviesStore().addMovie(new Movie("Беспечный ездок", 1969));
        server.getMoviesStore().addMovie(new Movie("Дикая Банда", 1969));
        server.getMoviesStore().addMovie(new Movie("Супер крутой фильм 1969 года", 1969));
        server.getMoviesStore().addMovie(new Movie("Криминальное чтиво", 1994));

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies?year=1969"))
                .build();

        HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode(),
                "GET /movies?year=YYYY должен вернуть 200");

        String contentTypeHeaderValue = response.headers()
                .firstValue("Content-Type")
                .orElse("");
        assertEquals("application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = response.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");

        Gson gson = new Gson();
        List<Movie> movies = gson.fromJson(
                response.body(),
                new ListOfMoviesTypeToken().getType()
        );
        assertEquals(4, movies.size());
        for (Movie movie : movies) {
            assertEquals(1969, movie.getYear());
        }
    }

    @Test
    void getMoviesByYear_whenYearNotFound_returnsEmptyList() throws IOException, InterruptedException {
        server.getMoviesStore().addMovie(new Movie("Harry Potter", 2002));
        server.getMoviesStore().addMovie(new Movie("Бриллиантовая рука", 1969));
        server.getMoviesStore().addMovie(new Movie("Беспечный ездок", 1969));
        server.getMoviesStore().addMovie(new Movie("Дикая Банда", 1969));
        server.getMoviesStore().addMovie(new Movie("Супер крутой фильм 1969 года", 1969));
        server.getMoviesStore().addMovie(new Movie("Криминальное чтиво", 1994));

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies?year=2026"))
                .build();

        HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode(),
                "GET /movies?year=YYYY должен вернуть 200");

        String contentTypeHeaderValue = response.headers()
                .firstValue("Content-Type")
                .orElse("");
        assertEquals("application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = response.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMoviesByYear_whenYearIsNotNumber_returnsError() throws IOException, InterruptedException {
        server.getMoviesStore().addMovie(new Movie("Harry Potter", 2002));
        server.getMoviesStore().addMovie(new Movie("Бриллиантовая рука", 1969));
        server.getMoviesStore().addMovie(new Movie("Беспечный ездок", 1969));
        server.getMoviesStore().addMovie(new Movie("Дикая Банда", 1969));
        server.getMoviesStore().addMovie(new Movie("Супер крутой фильм 1969 года", 1969));
        server.getMoviesStore().addMovie(new Movie("Криминальное чтиво", 1994));

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies?year=abc"))
                .build();

        HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, response.statusCode(),
                "GET /movies?year=YYYY должен вернуть 400");

        String contentTypeHeaderValue = response.headers()
                .firstValue("Content-Type")
                .orElse("");
        assertEquals("application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = response.body().trim();
        assertTrue(body.startsWith("{") && body.endsWith("}"),
                "Ожидается JSON-объект");
        assertTrue(body.contains("Некорректный параметр запроса — year"));
    }

    @Test
    void sendNotAllowedMethod_returnsError() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .PUT(HttpRequest.BodyPublishers.ofString("test"))
                .uri(URI.create(BASE + "/movies"))
                .build();

        HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(405, response.statusCode(),
                "PUT должен вернуть 405");

        String contentTypeHeaderValue = response.headers()
                .firstValue("Content-Type")
                .orElse("");
        assertEquals("application/json; charset=UTF-8",
                contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = response.body().trim();
        assertTrue(body.startsWith("{") && body.endsWith("}"),
                "Ожидается JSON-объект");
    }
}