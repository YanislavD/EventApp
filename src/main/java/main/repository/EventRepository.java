package main.repository;

import main.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    @Query("SELECT COUNT(DISTINCT e.category.id) FROM Event e WHERE e.category IS NOT NULL")
    long countDistinctCategories();

    @Query("SELECT e FROM Event e WHERE e.endTime < :cutoffDate")
    List<Event> findEventsOlderThan(LocalDateTime cutoffDate);
}
