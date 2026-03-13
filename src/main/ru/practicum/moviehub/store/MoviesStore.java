package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoviesStore {
    private int counter = 0;
    private Map<Integer, Movie> movieStore;

    public MoviesStore() {
        movieStore = new HashMap<>();
    }

    public void addMovie(Movie movie) {
        movie.setId(counter);
        counter++;
        movieStore.put(movie.getId(), movie);
    }

    public List<Movie> getMovies() {
        return new ArrayList<>(movieStore.values());
    }

    public void clear() {
        movieStore.clear();
        counter = 0;
    }

    public Movie getMovieById(int id) {
        return movieStore.get(id);
    }

    public void deleteMovieById(int id) {
        movieStore.remove(id);
    }

    public List<Movie> getMoviesByYear(int year) {
        return movieStore.values().stream()
                .filter(m -> m.getYear() == year)
                .toList();
    }
}