package de.newscore.search;

import de.newscore.domain.Article;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * ElasticSearch representation of an {@link Article}. Text fields are analyzed for full-text search;
 * ids and the timestamp are stored as keywords. Mapping to/from the domain record lives here so the
 * conversion has a single home (DRY).
 *
 * <p>Implemented as a class (not a record) because Spring Data's {@code @Field} cannot target record
 * components.</p>
 */
@Document(indexName = "articles")
public class ArticleDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String teaser;

    @Field(type = FieldType.Text)
    private String body;

    @Field(type = FieldType.Keyword)
    private String authorId;

    @Field(type = FieldType.Keyword)
    private String categoryId;

    @Field(type = FieldType.Keyword)
    private String publishedAt;

    @Field(type = FieldType.Text)
    private List<String> tags;

    @Field(type = FieldType.Keyword)
    private String imageUrl;

    public ArticleDocument() {
    }

    /** Builds a document from a domain article. */
    public static ArticleDocument from(Article article) {
        ArticleDocument document = new ArticleDocument();
        document.id = article.id();
        document.title = article.title();
        document.teaser = article.teaser();
        document.body = article.body();
        document.authorId = article.authorId();
        document.categoryId = article.categoryId();
        document.publishedAt = article.publishedAt().toString();
        document.tags = article.tags();
        document.imageUrl = article.imageUrl();
        return document;
    }

    /** Converts this document back into a domain article. */
    public Article toArticle() {
        return new Article(id, title, teaser, body, authorId, categoryId,
                OffsetDateTime.parse(publishedAt), tags, imageUrl);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTeaser() {
        return teaser;
    }

    public void setTeaser(String teaser) {
        this.teaser = teaser;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
