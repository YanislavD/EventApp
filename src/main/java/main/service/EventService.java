package main.service;

import main.model.Category;
import main.model.Event;
import main.model.Role;
import main.model.User;
import main.repository.EventRepository;
import main.web.dto.EventCreateRequest;
import main.web.view.EventView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final CategoryService categoryService;
    private final SubscriptionService subscriptionService;

    public EventService(EventRepository eventRepository,
                        CategoryService categoryService,
                        SubscriptionService subscriptionService) {
        this.eventRepository = eventRepository;
        this.categoryService = categoryService;
        this.subscriptionService = subscriptionService;
    }

    @Cacheable(value = "stats", key = "'eventCount'")
    public Long getCount(){
        return eventRepository.count();
    }

    public List<Category> getAvailableCategories() {
        return categoryService.getAll();
    }

    public List<EventView> getAllEventsForAdmin() {
        return eventRepository.findAll(Sort.by(Sort.Direction.ASC, "startTime")).stream()
                .map(event -> toView(event, false))
                .toList();
    }

    public long getDistinctCategoryCount() {
        return eventRepository.countDistinctCategories();
    }

    public Event getById(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Събитието не е намерено"));
    }

    @Transactional
    @CacheEvict(value = {"events", "stats"}, allEntries = true)
    @SuppressWarnings("null")
    public Event create(EventCreateRequest request, User creator) {
        if (creator == null) {
            throw new IllegalArgumentException("Организаторът е задължителен");
        }

        UUID categoryId = request.getCategoryId();
        if (categoryId == null) {
            throw new IllegalArgumentException("Категорията е задължителна");
        }

        Category category = categoryService.getById(categoryId);

        Event event = Event.builder()
                .name(request.getName())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .capacity(request.getCapacity())
                .category(category)
                .creator(creator)
                .build();

        Event saved = eventRepository.save(event);
        logger.info("Event created successfully: {} by user {}", saved.getName(), creator.getEmail());
        return saved;
    }

    public List<EventView> getEventsForListing(UUID userId, Sort sort, UUID categoryFilter) {
        Set<UUID> subscribedEventIds = subscriptionService.getSubscribedEventIds(userId);
        Sort effectiveSort = (sort != null) ? sort : Sort.by(Sort.Direction.ASC, "startTime");

        return eventRepository.findAll(effectiveSort).stream()
                .filter(event -> categoryFilter == null || (event.getCategory() != null && categoryFilter.equals(event.getCategory().getId())))
                .map(event -> toView(event, subscribedEventIds.contains(event.getId())))
                .toList();
    }

    public List<EventView> getCreatedEvents(UUID userId) {
        return eventRepository.findAll(Sort.by(Sort.Direction.ASC, "startTime")).stream()
                .filter(event -> event.getCreator() != null && event.getCreator().getId().equals(userId))
                .map(event -> toView(event, false))
                .toList();
    }

    public List<EventView> getSubscribedEvents(UUID userId) {
        return subscriptionService.getSubscribedEvents(userId).stream()
                .sorted(Comparator.comparing(Event::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(event -> toView(event, true))
                .toList();
    }

    @Transactional
    @SuppressWarnings("null")
    public boolean subscribeUserToEvent(UUID eventId, User user) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Събитието не е намерено"));

        if (event.getCreator() != null && event.getCreator().getId().equals(user.getId())) {
            throw new IllegalStateException("OWN_EVENT");
        }

        if (subscriptionService.existsByUserAndEvent(user.getId(), eventId)) {
            return false;
        }

        if (event.getCapacity() != null) {
            long currentSubscriptions = subscriptionService.countByEvent(eventId);
            if (currentSubscriptions >= event.getCapacity()) {
                throw new IllegalStateException("FULL");
            }
        }

        subscriptionService.create(user, event);
        logger.info("User {} subscribed to event {}", user.getEmail(), event.getName());
        return true;
    }

    @Transactional
    public boolean unsubscribeUserFromEvent(UUID eventId, User user) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Събитието не е намерено"));

        if (!subscriptionService.existsByUserAndEvent(user.getId(), eventId)) {
            return false;
        }

        subscriptionService.deleteByUserAndEvent(user.getId(), eventId);
        logger.info("User {} unsubscribed from event {}", user.getEmail(), event.getName());
        return true;
    }

    @Transactional
    @CacheEvict(value = {"events", "stats"}, allEntries = true)
    @SuppressWarnings("null")
    public Event update(UUID eventId, EventCreateRequest request, User user) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Събитието не е намерено"));

        if (event.getCreator() == null || !event.getCreator().getId().equals(user.getId())) {
            throw new IllegalStateException("Можеш да редактираш само събития, които си създал");
        }

        UUID categoryId = request.getCategoryId();
        if (categoryId == null) {
            throw new IllegalArgumentException("Категорията е задължителна");
        }

        Category category = categoryService.getById(categoryId);

        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setCapacity(request.getCapacity());
        event.setCategory(category);

        Event updated = eventRepository.save(event);
        logger.info("Event updated successfully: {} by user {}", updated.getName(), user.getEmail());
        return updated;
    }

    @Transactional
    @CacheEvict(value = {"events", "stats"}, allEntries = true)
    public void delete(UUID eventId, User user) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Събитието не е намерено"));

        boolean isCreator = event.getCreator() != null && event.getCreator().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isCreator && !isAdmin) {
            throw new IllegalStateException("Можеш да изтриваш само събития, които си създал, или трябва да си администратор");
        }

        subscriptionService.deleteAllByEventId(eventId);
        eventRepository.delete(event);
        logger.info("Event deleted successfully: {} by user {}", event.getName(), user.getEmail());
    }

    @Transactional
    public int deleteEventsOlderThanDays(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        List<Event> oldEvents = eventRepository.findEventsOlderThan(cutoffDate);
        
        int deletedCount = 0;
        for (Event event : oldEvents) {
            subscriptionService.deleteAllByEventId(event.getId());
            eventRepository.delete(event);
            deletedCount++;
        }
        
        return deletedCount;
    }

    private EventView toView(Event event, boolean subscribed) {
        String categoryName = event.getCategory() != null ? event.getCategory().getName() : "";
        User creator = event.getCreator();
        UUID creatorId = creator != null ? creator.getId() : null;
        String creatorName = creator != null ? creator.getUsername() : "";
        long registeredCount = subscriptionService.countByEvent(event.getId());
        long remaining = event.getCapacity() != null ? Math.max(0, event.getCapacity() - registeredCount) : Long.MAX_VALUE;
        boolean full = event.getCapacity() != null && remaining <= 0;

        return new EventView(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getStartTime(),
                event.getEndTime(),
                event.getCapacity(),
                remaining,
                registeredCount,
                full,
                categoryName,
                creatorId,
                creatorName,
                subscribed
        );
    }
}
