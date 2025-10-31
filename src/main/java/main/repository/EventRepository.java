package main.repository;

import main.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    @Query("SELECT COUNT(DISTINCT e.category.id) FROM Event e WHERE e.category IS NOT NULL")
    long countDistinctCategories();
}
