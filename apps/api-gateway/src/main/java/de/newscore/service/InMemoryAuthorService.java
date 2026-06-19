package de.newscore.service;

import de.newscore.domain.Author;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * In-memory {@link AuthorService} backed by a fixed set of mock authors.
 */
@Service
public class InMemoryAuthorService implements AuthorService {

    private final Map<String, Author> authorsById;

    public InMemoryAuthorService() {
        this.authorsById = seed().stream()
                .collect(Collectors.toMap(Author::id, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private static List<Author> seed() {
        return List.of(
                new Author("a1", "Lena Hoffmann", "lena.hoffmann@newscore.de",
                        "Politikredakteurin mit Schwerpunkt Europapolitik."),
                new Author("a2", "Marco Bauer", "marco.bauer@newscore.de",
                        "Wirtschaftsjournalist, schreibt über Märkte und Konjunktur."),
                new Author("a3", "Sophie Klein", "sophie.klein@newscore.de",
                        "Kulturkorrespondentin und Filmkritikerin."),
                new Author("a4", "Jonas Weber", "jonas.weber@newscore.de",
                        "Tech-Reporter mit Fokus auf KI und Cloud."));
    }

    @Override
    public Optional<Author> findById(String id) {
        return Optional.ofNullable(authorsById.get(id));
    }

    @Override
    public Map<String, Author> findByIds(Collection<String> ids) {
        return ids.stream()
                .distinct()
                .filter(authorsById::containsKey)
                .collect(Collectors.toMap(Function.identity(), authorsById::get));
    }
}
