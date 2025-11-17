package main.service;

import main.model.Category;
import main.model.Event;
import main.model.Role;
import main.model.Subscription;
import main.model.Ticket;
import main.model.User;
import main.repository.CategoryRepository;
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
import org.springframework.validation.BindingResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final SubscriptionService subscriptionService;
    private final TicketService ticketService;

    public EventService(EventRepository eventRepository,
                        CategoryRepository categoryRepository,
                        SubscriptionService subscriptionService,
                        TicketService ticketService) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.subscriptionService = subscriptionService;
        this.ticketService = ticketService;
    }

    @Cacheable(value = "stats", key = "'eventCount'")
    public Long getCount(){
        return eventRepository.count();
    }

    public List<Category> getAvailableCategories() {
        return categoryRepository.findByIsActiveTrue(Sort.by(Sort.Direction.ASC, "name"));
    }

    public Sort resolveSort(String sortParam) {
        if ("startDesc".equalsIgnoreCase(sortParam)) {
            return Sort.by(Sort.Direction.DESC, "startTime");
        }
        if ("categoryAsc".equalsIgnoreCase(sortParam)) {
            return Sort.by(Sort.Direction.ASC, "category.name").and(Sort.by(Sort.Direction.ASC, "startTime"));
        }
        if ("categoryDesc".equalsIgnoreCase(sortParam)) {
            return Sort.by(Sort.Direction.DESC, "category.name").and(Sort.by(Sort.Direction.ASC, "startTime"));
        }
        return Sort.by(Sort.Direction.ASC, "startTime");
    }

    public List<EventView> getAllEventsForAdmin() {
        return eventRepository.findAll(Sort.by(Sort.Direction.ASC, "startTime")).stream()
                .map(event -> toView(event, false, null))
                .toList();
    }

    public long getDistinctCategoryCount() {
        return eventRepository.countDistinctCategories();
    }

    public Event getById(UUID eventId) {
        Objects.requireNonNull(eventId, "Идентификаторът на събитие е задължителен");
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Събитието не е намерено"));
    }

    public void validateSchedule(EventCreateRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return;
        }
        if (request.getStartTime() != null && request.getEndTime() != null &&
                !request.getEndTime().isAfter(request.getStartTime())) {
            bindingResult.rejectValue("endTime", "event.endTime.invalid", "Краят трябва да е след началото");
        }
    }

    public EventCreateRequest buildEditRequest(UUID eventId, User user) {
        Event event = getById(eventId);
        if (event.getCreator() == null || !event.getCreator().getId().equals(user.getId())) {
            throw new IllegalStateException("Можеш да редактираш само събития, които си създал");
        }

        EventCreateRequest eventCreateRequest = new EventCreateRequest();
        eventCreateRequest.setName(event.getName());
        eventCreateRequest.setDescription(event.getDescription());
        eventCreateRequest.setLocation(event.getLocation());
        eventCreateRequest.setStartTime(event.getStartTime());
        eventCreateRequest.setEndTime(event.getEndTime());
        eventCreateRequest.setCapacity(event.getCapacity());
        eventCreateRequest.setCategoryId(event.getCategory() != null ? event.getCategory().getId() : null);
        return eventCreateRequest;
    }

    @Transactional
    @CacheEvict(value = {"events", "stats"}, allEntries = true)
    public Event create(EventCreateRequest request, User creator) {
        if (creator == null) {
            throw new IllegalArgumentException("Организаторът е задължителен");
        }

        UUID categoryId = request.getCategoryId();
        if (categoryId == null) {
            throw new IllegalArgumentException("Категорията е задължителна");
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Категорията не е намерена"));

        if (category.getIsActive() == null || !category.getIsActive()) {
            throw new IllegalArgumentException("Не можеш да използваш неактивна категория");
        }

        Event event = Event.builder()
                .name(request.getName())
                .description(request.getDescription())
                .location(request.getLocation())
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
        LocalDateTime now = LocalDateTime.now();
        List<Event> events = eventRepository.findUpcomingEvents(now);
        
        events = filterByCategory(events, categoryFilter);
        events = sortEvents(events, sort, true);
        
        Set<UUID> subscribedEventIds = subscriptionService.getSubscribedEventIds(userId);
        return convertToEventViews(events, subscribedEventIds);
    }

    public List<EventView> getPastEventsForListing(UUID userId, Sort sort, UUID categoryFilter) {
        LocalDateTime now = LocalDateTime.now();
        List<Event> events = eventRepository.findPastEvents(now);
        
        events = filterByCategory(events, categoryFilter);
        events = sortEvents(events, sort, false);
        
        Set<UUID> subscribedEventIds = subscriptionService.getSubscribedEventIds(userId);
        return convertToEventViews(events, subscribedEventIds);
    }
    
    private List<Event> filterByCategory(List<Event> events, UUID categoryFilter) {
        if (categoryFilter == null) {
            return events;
        }
        
        List<Event> filtered = new java.util.ArrayList<>();
        for (Event event : events) {
            if (event.getCategory() != null && categoryFilter.equals(event.getCategory().getId())) {
                filtered.add(event);
            }
        }
        return filtered;
    }
    
    private List<Event> sortEvents(List<Event> events, Sort sort, boolean defaultAscending) {
        final boolean ascending;
        if (sort != null && sort.isSorted()) {
            Sort.Order order = sort.iterator().next();
            if (order.getProperty().equals("startTime")) {
                ascending = order.isAscending();
            } else {
                ascending = defaultAscending;
            }
        } else {
            ascending = defaultAscending;
        }
        
        List<Event> sorted = new java.util.ArrayList<>(events);
        sorted.sort((e1, e2) -> {
            LocalDateTime time1 = e1.getStartTime();
            LocalDateTime time2 = e2.getStartTime();
            
            if (time1 == null && time2 == null) return 0;
            if (time1 == null) return 1;
            if (time2 == null) return -1;
            
            int comparison = time1.compareTo(time2);
            return ascending ? comparison : -comparison;
        });
        
        return sorted;
    }
    
    private List<EventView> convertToEventViews(List<Event> events, Set<UUID> subscribedEventIds) {
        List<EventView> views = new java.util.ArrayList<>();
        for (Event event : events) {
            boolean isSubscribed = subscribedEventIds.contains(event.getId());
            EventView view = toView(event, isSubscribed, null);
            views.add(view);
        }
        return views;
    }

    public List<EventView> getCreatedEvents(UUID userId) {
        List<Event> allEvents = eventRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        
        List<Event> upcomingEvents = new java.util.ArrayList<>();
        for (Event event : allEvents) {
            boolean isCreatedByUser = event.getCreator() != null && event.getCreator().getId().equals(userId);
            boolean isNotExpired = event.getEndTime() != null && event.getEndTime().isAfter(now);
            
            if (isCreatedByUser && isNotExpired) {
                upcomingEvents.add(event);
            }
        }
        
        upcomingEvents.sort((e1, e2) -> {
            LocalDateTime time1 = e1.getStartTime();
            LocalDateTime time2 = e2.getStartTime();
            if (time1 == null && time2 == null) return 0;
            if (time1 == null) return 1;
            if (time2 == null) return -1;
            return time1.compareTo(time2);
        });
        
        List<EventView> views = new java.util.ArrayList<>();
        for (Event event : upcomingEvents) {
            views.add(toView(event, false, null));
        }
        return views;
    }

    public List<EventView> getPastCreatedEvents(UUID userId) {
        List<Event> allEvents = eventRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        
        List<Event> pastEvents = new java.util.ArrayList<>();
        for (Event event : allEvents) {
            boolean isCreatedByUser = event.getCreator() != null && event.getCreator().getId().equals(userId);
            boolean isExpired = event.getEndTime() != null && event.getEndTime().isBefore(now);
            
            if (isCreatedByUser && isExpired) {
                pastEvents.add(event);
            }
        }
        
        pastEvents.sort((e1, e2) -> {
            LocalDateTime time1 = e1.getStartTime();
            LocalDateTime time2 = e2.getStartTime();
            if (time1 == null && time2 == null) return 0;
            if (time1 == null) return 1;
            if (time2 == null) return -1;
            return time2.compareTo(time1);
        });
        
        List<EventView> views = new java.util.ArrayList<>();
        for (Event event : pastEvents) {
            views.add(toView(event, false, null));
        }
        return views;
    }

    public List<EventView> getSubscribedEvents(UUID userId) {
        List<Subscription> subscriptions = subscriptionService.findByUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        
        java.util.Map<UUID, Ticket> ticketsByEventId = ticketService.getTicketsForUser(userId);
        
        List<Event> upcomingEvents = new java.util.ArrayList<>();
        for (Subscription subscription : subscriptions) {
            Event event = subscription.getEvent();
            if (event != null) {
                boolean isNotExpired = event.getEndTime() != null && event.getEndTime().isAfter(now);
                if (isNotExpired) {
                    upcomingEvents.add(event);
                }
            }
        }
        
        upcomingEvents.sort((e1, e2) -> {
            LocalDateTime time1 = e1.getStartTime();
            LocalDateTime time2 = e2.getStartTime();
            if (time1 == null && time2 == null) return 0;
            if (time1 == null) return 1;
            if (time2 == null) return -1;
            return time1.compareTo(time2);
        });
        
        List<EventView> views = new java.util.ArrayList<>();
        for (Event event : upcomingEvents) {
            Ticket ticket = ticketsByEventId.get(event.getId());
            String ticketCode = ticket != null ? ticket.getCode() : null;
            views.add(toView(event, true, ticketCode));
        }
        return views;
    }

    public List<EventView> getPastSubscribedEvents(UUID userId) {
        List<Subscription> subscriptions = subscriptionService.findByUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        
        java.util.Map<UUID, Ticket> ticketsByEventId = ticketService.getTicketsForUser(userId);
        
        List<Event> pastEvents = new java.util.ArrayList<>();
        for (Subscription subscription : subscriptions) {
            Event event = subscription.getEvent();
            if (event != null) {
                boolean isExpired = event.getEndTime() != null && event.getEndTime().isBefore(now);
                if (isExpired) {
                    pastEvents.add(event);
                }
            }
        }
        
        pastEvents.sort((e1, e2) -> {
            LocalDateTime time1 = e1.getStartTime();
            LocalDateTime time2 = e2.getStartTime();
            if (time1 == null && time2 == null) return 0;
            if (time1 == null) return 1;
            if (time2 == null) return -1;
            return time2.compareTo(time1);
        });
        
        List<EventView> views = new java.util.ArrayList<>();
        for (Event event : pastEvents) {
            Ticket ticket = ticketsByEventId.get(event.getId());
            String ticketCode = ticket != null ? ticket.getCode() : null;
            views.add(toView(event, true, ticketCode));
        }
        return views;
    }

    @Transactional
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
        Objects.requireNonNull(eventId, "Идентификаторът на събитие е задължителен");
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

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Категорията не е намерена"));

        if (category.getIsActive() == null || !category.getIsActive()) {
            throw new IllegalArgumentException("Не можеш да използваш неактивна категория");
        }

        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setLocation(request.getLocation());
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
        Objects.requireNonNull(eventId, "Идентификаторът на събитие е задължителен");
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
    @CacheEvict(value = {"events", "stats"}, allEntries = true)
    public void deleteAllByCreatorId(UUID creatorId) {
        Objects.requireNonNull(creatorId, "Идентификаторът на организатора е задължителен");
        List<Event> events = eventRepository.findByCreatorId(creatorId);
        for (Event event : events) {
            subscriptionService.deleteAllByEventId(event.getId());
            eventRepository.delete(event);
            logger.info("Event deleted as part of user cleanup: {}", event.getName());
        }
    }

    @Transactional
    @CacheEvict(value = {"events", "stats"}, allEntries = true)
    public void deleteAllByCategoryId(UUID categoryId) {
        Objects.requireNonNull(categoryId, "Идентификаторът на категорията е задължителен");
        List<Event> events = eventRepository.findByCategoryId(categoryId);
        for (Event event : events) {
            subscriptionService.deleteAllByEventId(event.getId());
            eventRepository.delete(event);
            logger.info("Event deleted as part of category deletion: {} (category: {})", event.getName(), categoryId);
        }
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

    private EventView toView(Event event, boolean subscribed, String ticketCode) {
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
                event.getLocation(),
                event.getStartTime(),
                event.getEndTime(),
                event.getCapacity(),
                remaining,
                registeredCount,
                full,
                categoryName,
                creatorId,
                creatorName,
                subscribed,
                ticketCode
        );
    }

}
