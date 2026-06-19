package de.newscore.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data repository for {@link ArticleDocument}. Repositories are disabled by default and only
 * activated under the {@code elasticsearch} profile (see {@code application-elasticsearch.yml}).
 */
public interface ArticleSearchRepository extends ElasticsearchRepository<ArticleDocument, String> {
}
