package de.newscore.service;

import de.newscore.domain.Category;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * In-memory {@link CategoryService} backed by a fixed set of mock categories.
 */
@Service
public class InMemoryCategoryService implements CategoryService {

    private final Map<String, Category> categoriesById;

    public InMemoryCategoryService() {
        this.categoriesById = seed().stream()
                .collect(Collectors.toMap(Category::id, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private static List<Category> seed() {
        return List.of(
                new Category("c1", "Politik", "politik"),
                new Category("c2", "Wirtschaft", "wirtschaft"),
                new Category("c3", "Kultur", "kultur"),
                new Category("c4", "Technik", "technik"));
    }

    @Override
    public Optional<Category> findById(String id) {
        return Optional.ofNullable(categoriesById.get(id));
    }

    @Override
    public Map<String, Category> findByIds(Collection<String> ids) {
        return ids.stream()
                .distinct()
                .filter(categoriesById::containsKey)
                .collect(Collectors.toMap(Function.identity(), categoriesById::get));
    }
}
