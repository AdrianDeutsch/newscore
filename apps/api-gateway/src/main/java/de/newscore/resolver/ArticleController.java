package de.newscore.resolver;

import de.newscore.domain.Article;
import de.newscore.domain.ArticleConnection;
import de.newscore.domain.Author;
import de.newscore.domain.Category;
import de.newscore.service.ArticleService;
import de.newscore.service.AuthorService;
import de.newscore.service.CategoryService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

/**
 * GraphQL resolvers for {@code Article} queries and its nested {@code author}/{@code category}
 * fields.
 *
 * <p>The nested fields are resolved with {@link BatchMapping} so that, when a list of articles is
 * returned, authors and categories are loaded in a single batched call each rather than once per
 * article — this is the DataLoader-based N+1 avoidance described in ADR-002.</p>
 */
@Controller
public class ArticleController {

    private final ArticleService articleService;
    private final AuthorService authorService;
    private final CategoryService categoryService;

    public ArticleController(ArticleService articleService,
                             AuthorService authorService,
                             CategoryService categoryService) {
        this.articleService = articleService;
        this.authorService = authorService;
        this.categoryService = categoryService;
    }

    /**
     * Resolves {@code Query.article(id)}.
     *
     * @param id the requested article id
     * @return the article, or {@code null} if none exists
     */
    @QueryMapping
    @Nullable
    public Article article(@Argument String id) {
        return articleService.findById(id).orElse(null);
    }

    /**
     * Resolves {@code Query.articles(category, limit, offset)}.
     *
     * @param category optional category slug filter
     * @param limit    page size (schema default applies when omitted)
     * @param offset   number of leading articles to skip
     * @return a connection with the requested page and pagination metadata
     */
    @QueryMapping
    public ArticleConnection articles(@Argument @Nullable String category,
                                      @Argument int limit,
                                      @Argument int offset) {
        return articleService.findArticles(category, limit, offset);
    }

    /**
     * Batch-resolves the {@code Article.author} field for a list of articles in a single call.
     *
     * @param articles the articles whose authors are requested
     * @return a map from each article to its author
     */
    @BatchMapping
    public Map<Article, Author> author(List<Article> articles) {
        Map<String, Author> authorsById =
                authorService.findByIds(articles.stream().map(Article::authorId).toList());
        return associate(articles, Article::authorId, authorsById);
    }

    /**
     * Batch-resolves the {@code Article.category} field for a list of articles in a single call.
     *
     * @param articles the articles whose categories are requested
     * @return a map from each article to its category
     */
    @BatchMapping
    public Map<Article, Category> category(List<Article> articles) {
        Map<String, Category> categoriesById =
                categoryService.findByIds(articles.stream().map(Article::categoryId).toList());
        return associate(articles, Article::categoryId, categoriesById);
    }

    /**
     * Associates each article with the looked-up value for its foreign key. Articles whose key is
     * absent from {@code byId} are skipped (the schema marks these fields non-null, so for
     * consistent data every article resolves).
     */
    private <V> Map<Article, V> associate(List<Article> articles,
                                          java.util.function.Function<Article, String> keyExtractor,
                                          Map<String, V> byId) {
        Map<Article, V> result = new LinkedHashMap<>();
        for (Article article : articles) {
            Optional.ofNullable(byId.get(keyExtractor.apply(article)))
                    .ifPresent(value -> result.put(article, value));
        }
        return result;
    }
}
