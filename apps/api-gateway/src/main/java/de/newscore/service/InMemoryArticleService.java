package de.newscore.service;

import de.newscore.domain.Article;
import de.newscore.domain.ArticleConnection;
import de.newscore.domain.Category;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * In-memory {@link ArticleService} backed by a fixed set of mock articles, sorted newest-first.
 * Active by default; the Sophora CMS implementation takes over under the {@code sophora} profile
 * (see ADR-010).
 *
 * <p>Filtering by category resolves the requested slug via {@link CategoryService}, keeping category
 * ownership in a single place (DRY). Hot read paths are cached with Caffeine (see ADR-003).</p>
 */
@Service
@Profile("!sophora")
public class InMemoryArticleService implements ArticleService {

    /** Upper bound for a single page to protect the backend from abusive limits. */
    private static final int MAX_LIMIT = 100;

    private final CategoryService categoryService;
    private final List<Article> articles;

    public InMemoryArticleService(CategoryService categoryService) {
        this.categoryService = categoryService;
        this.articles = seed().stream()
                .sorted(Comparator.comparing(Article::publishedAt).reversed())
                .toList();
    }

    @Override
    @Cacheable("article")
    public Optional<Article> findById(String id) {
        return articles.stream().filter(a -> a.id().equals(id)).findFirst();
    }

    @Override
    @Cacheable("articles")
    public ArticleConnection findArticles(String categorySlug, int limit, int offset) {
        List<Article> filtered = filterByCategory(categorySlug);
        int totalCount = filtered.size();

        int effectiveLimit = Math.max(0, Math.min(limit, MAX_LIMIT));
        int from = Math.min(Math.max(offset, 0), totalCount);
        int to = Math.min(from + effectiveLimit, totalCount);

        List<Article> page = filtered.subList(from, to);
        boolean hasNextPage = to < totalCount;
        return new ArticleConnection(List.copyOf(page), totalCount, hasNextPage);
    }

    @Override
    public List<Article> findAll() {
        return articles;
    }

    /**
     * Filters the corpus by category slug. A blank slug returns all articles; an unknown slug
     * returns an empty list.
     */
    private List<Article> filterByCategory(String categorySlug) {
        if (!StringUtils.hasText(categorySlug)) {
            return articles;
        }
        Optional<String> categoryId = categoryService.findByIds(distinctCategoryIds()).values().stream()
                .filter(c -> c.slug().equalsIgnoreCase(categorySlug))
                .map(Category::id)
                .findFirst();
        return categoryId
                .map(id -> articles.stream().filter(a -> a.categoryId().equals(id)).toList())
                .orElseGet(List::of);
    }

    private List<String> distinctCategoryIds() {
        return articles.stream().map(Article::categoryId).distinct().toList();
    }

    private static List<Article> seed() {
        return List.of(
                new Article("1",
                        "EU einigt sich auf neues Klimapaket",
                        "Die Mitgliedstaaten haben sich nach langen Verhandlungen auf verbindliche Ziele geeinigt.",
                        "Brüssel — In einer nächtlichen Sitzung haben sich die EU-Staaten auf ein "
                                + "umfassendes Klimapaket verständigt. Es sieht verbindliche Reduktionsziele "
                                + "bis 2035 vor und stärkt den Emissionshandel.",
                        "a1", "c1",
                        OffsetDateTime.parse("2026-06-18T07:15:00Z"),
                        List.of("klima", "eu", "politik"),
                        "/images/covers/cover-1.svg"),
                new Article("2",
                        "Inflation sinkt den dritten Monat in Folge",
                        "Verbraucherpreise legen nur noch moderat zu — Entlastung für Haushalte.",
                        "Die Inflationsrate ist im Mai erneut gesunken. Ökonomen sehen darin ein Signal "
                                + "für eine baldige Zinswende der Zentralbank.",
                        "a2", "c2",
                        OffsetDateTime.parse("2026-06-17T11:40:00Z"),
                        List.of("inflation", "wirtschaft", "ezb"),
                        "/images/covers/cover-2.svg"),
                new Article("3",
                        "Filmfestival eröffnet mit gefeiertem Drama",
                        "Der Eröffnungsfilm erhält stehende Ovationen vom Publikum.",
                        "Mit einem viel beachteten Drama ist das diesjährige Filmfestival eröffnet worden. "
                                + "Kritiker loben Regie und Hauptdarstellerin gleichermaßen.",
                        "a3", "c3",
                        OffsetDateTime.parse("2026-06-17T18:05:00Z"),
                        List.of("film", "festival", "kultur"),
                        "/images/covers/cover-3.svg"),
                new Article("4",
                        "Neues KI-Modell setzt Maßstäbe bei Effizienz",
                        "Das Modell erreicht Spitzenwerte bei deutlich geringerem Energiebedarf.",
                        "Ein neues KI-Modell verspricht vergleichbare Leistung bei einem Bruchteil des "
                                + "Energieverbrauchs. Die Branche reagiert mit großem Interesse.",
                        "a4", "c4",
                        OffsetDateTime.parse("2026-06-16T09:00:00Z"),
                        List.of("ki", "technik", "effizienz"),
                        "/images/covers/cover-4.svg"),
                new Article("5",
                        "Koalition streitet über Haushaltsentwurf",
                        "Die Verhandlungen über den Etat geraten ins Stocken.",
                        "Im Streit über den Haushaltsentwurf zeichnet sich keine schnelle Einigung ab. "
                                + "Mehrere Ressorts pochen auf zusätzliche Mittel.",
                        "a1", "c1",
                        OffsetDateTime.parse("2026-06-15T14:20:00Z"),
                        List.of("haushalt", "koalition", "politik"),
                        "/images/covers/cover-5.svg"),
                new Article("6",
                        "Startup-Finanzierungen ziehen wieder an",
                        "Wagniskapital fließt verstärkt in Klima- und KI-Startups.",
                        "Nach einem schwachen Vorjahr nehmen die Finanzierungsrunden wieder zu. "
                                + "Besonders gefragt sind nachhaltige Geschäftsmodelle.",
                        "a2", "c2",
                        OffsetDateTime.parse("2026-06-14T08:30:00Z"),
                        List.of("startup", "venture", "wirtschaft"),
                        "/images/covers/cover-6.svg"),
                new Article("7",
                        "Museum zeigt wiederentdeckte Werke",
                        "Eine Sonderausstellung präsentiert lange verschollene Gemälde.",
                        "Das Museum widmet den wiederentdeckten Werken eine umfangreiche Sonderausstellung. "
                                + "Besucher können die Gemälde erstmals seit Jahrzehnten sehen.",
                        "a3", "c3",
                        OffsetDateTime.parse("2026-06-13T10:10:00Z"),
                        List.of("kunst", "ausstellung", "kultur"),
                        "/images/covers/cover-7.svg"),
                new Article("8",
                        "Cloud-Anbieter senken Preise für Speicher",
                        "Der Wettbewerb im Cloud-Markt verschärft sich weiter.",
                        "Mehrere große Cloud-Anbieter haben Preissenkungen für Objektspeicher angekündigt. "
                                + "Profitieren dürften vor allem datenintensive Anwendungen.",
                        "a4", "c4",
                        OffsetDateTime.parse("2026-06-12T16:45:00Z"),
                        List.of("cloud", "technik", "preise"),
                        "/images/covers/cover-8.svg"));
    }
}
