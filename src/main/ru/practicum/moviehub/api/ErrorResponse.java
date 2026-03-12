package ru.practicum.moviehub.api;

import java.util.ArrayList;
import java.util.List;

public class ErrorResponse {
    private String error;
    private List<String> details = new ArrayList<>();

    public void setError(String error) {
        this.error = error;
    }

    public List<String> getDetails() {
        return details;
    }
}