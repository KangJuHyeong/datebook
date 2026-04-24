package app.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "couples")
public class Couple extends BaseEntity {

    public Couple() {
    }
}
