package app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "questions")
public class Question extends CreatedAtEntity {

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Question() {
    }

    public Question(String content, boolean active, int sortOrder) {
        this.content = content;
        this.active = active;
        this.sortOrder = sortOrder;
    }

    public String getContent() {
        return content;
    }

    public boolean isActive() {
        return active;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
