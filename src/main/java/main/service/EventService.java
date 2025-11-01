package main.service;

import main.model.Category;
import main.model.Event;
import main.model.User;
import main.repository.EventRepository;
import main.web.dto.EventCreateRequest;
import main.web.view.EventView;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class EventService {

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

    public Long getCount(){
        return eventRepository.count();
    }

    public List<Category> getAvailableCategories() {
        return categoryService.getAll();
    }

    public long getDistinctCategoryCount() {
        return eventRepository.countDistinctCategories();
    }

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

        return eventRepository.save(event);
    }

    public List<EventView> getEventsForListing(UUID userId, Sort sort, UUID categoryFilter) {
        Set<UUID> subscribedEventIds = subscriptionService.getSubscribedEventIds(userId);
        Sort effectiveSort = (sort != null) ? sort : Sort.by(Sort.Direction.ASC, "startTime");

        return eventRepository.findAll(effectiveSort).stream()
                .filter(event -> event.getCreator() == null || !event.getCreator().getId().equals(userId))
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
        return true;
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
